package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.BookSummary;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈 화면용 도서 메타데이터 관리 서비스
 *
 * 전략:
 * 1. DB 우선 조회 (캐시된 메타데이터)
 * 2. DB에 없으면 비동기로 카카오 API 호출하여 보강
 * 3. 홈 화면 응답은 DB 데이터만으로 즉시 반환 (지연 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookMetadataService {

    private final BooksRepository booksRepository;
    private final KakaoBookClient kakaoBookClient;

    /**
     * 홈 화면용 도서 요약 정보 조회
     * Redis 캐시 적용 (6시간 TTL)
     *
     * @param bookId 도서 ID
     * @param title 도서명
     * @param ranking 순위 (nullable)
     * @return BookSummary
     */
    @Cacheable(value = "bookMetadata", key = "'book:' + #bookId", unless = "#result == null")
    @Transactional(readOnly = true)
    public BookSummary getBookSummary(Long bookId, String title, Integer ranking) {
        // 1. DB에서 조회
        Optional<Books> bookOpt = booksRepository.findByTitle(title);

        if (bookOpt.isPresent()) {
            Books book = bookOpt.get();

            // 2. 메타데이터 보강 필요 시 비동기로 처리
            if (needsMetadataEnrichment(book)) {
                enrichMetadataAsync(book.getId(), title);
            }

            // 3. 현재 DB 데이터로 응답 (즉시 반환)
            return createBookSummary(bookId, book, ranking);
        }

        // 4. DB에 없으면 빈 Books 생성 후 비동기 보강
        Books newBook = createEmptyBook(title);
        Books savedBook = booksRepository.save(newBook);
        enrichMetadataAsync(savedBook.getId(), title);

        // 5. 일단 title만으로 응답
        return new BookSummary(
                bookId,
                title,
                null,
                null,
                null,
                ranking
        );
    }

    /**
     * 여러 도서 일괄 조회 (홈 화면 최적화)
     *
     * @param bookInfoList (bookId, title, ranking) 리스트
     * @return BookSummary 리스트
     */
    @Transactional(readOnly = true)
    public List<BookSummary> getBookSummaries(List<BookInfo> bookInfoList) {
        // 1. title 목록 추출
        List<String> titles = bookInfoList.stream()
                .map(BookInfo::title)
                .distinct()
                .collect(Collectors.toList());

        // 2. 일괄 조회 (IN 쿼리 1번)
        List<Books> books = booksRepository.findAllByTitleIn(titles);
        Map<String, Books> bookMap = books.stream()
                .collect(Collectors.toMap(Books::getTitle, b -> b));

        // 3. BookSummary 변환
        List<BookSummary> results = new ArrayList<>();
        for (BookInfo info : bookInfoList) {
            Books book = bookMap.get(info.title);

            if (book != null) {
                // 메타데이터 보강 필요 시 비동기 처리
                if (needsMetadataEnrichment(book)) {
                    enrichMetadataAsync(book.getId(), info.title);
                }
                results.add(createBookSummary(info.bookId, book, info.ranking));
            } else {
                // DB에 없으면 빈 데이터 생성 후 비동기 보강
                Books newBook = createEmptyBook(info.title);
                Books saved = booksRepository.save(newBook);
                enrichMetadataAsync(saved.getId(), info.title);

                results.add(new BookSummary(
                        info.bookId,
                        info.title,
                        null,
                        null,
                        null,
                        info.ranking
                ));
            }
        }

        return results;
    }

    /**
     * 메타데이터 보강이 필요한지 판단
     */
    private boolean needsMetadataEnrichment(Books book) {
        // 썸네일이 없거나 출판사가 없으면 보강 필요
        return book.getThumbnailUrl() == null || book.getPublisherName() == null;
    }

    /**
     * 비동기로 카카오 API 호출하여 메타데이터 보강
     * 홈 화면 응답 지연을 방지하기 위해 별도 스레드에서 실행
     */
    @Async
    @Transactional
    public void enrichMetadataAsync(Long bookId, String title) {
        try {
            log.info("비동기 메타데이터 보강 시작: bookId={}, title={}", bookId, title);

            // 카카오 API 호출
            KakaoBookSearchResponse response = kakaoBookClient.search(title, 1, 1).block();

            if (response == null || response.getDocuments().isEmpty()) {
                log.warn("카카오 API 응답 없음: title={}", title);
                return;
            }

            // 첫 번째 결과로 업데이트
            KakaoBookSearchResponse.Document doc = response.getDocuments().get(0);

            Books book = booksRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));

            // ISBN 파싱
            String[] isbns = parseIsbn(doc.getIsbn());
            String isbn10 = isbns[0];
            String isbn13 = isbns[1];

            // Books 엔티티 업데이트
            book.updateBasicInfo(
                    doc.getTitle(),
                    doc.getContents(),
                    doc.getThumbnail(),
                    doc.getUrl(),
                    doc.getPublisher(),
                    null, // publishedDate는 별도 파싱 필요
                    doc.getIsbn(),
                    isbn10,
                    isbn13,
                    null // rawData는 필요시 추가
            );

            booksRepository.save(book);

            log.info("메타데이터 보강 완료: bookId={}, title={}", bookId, title);

        } catch (Exception e) {
            log.error("메타데이터 보강 실패: bookId={}, title={}, error={}", bookId, title, e.getMessage(), e);
        }
    }

    /**
     * BookSummary 생성
     */
    private BookSummary createBookSummary(Long bookId, Books book, Integer ranking) {
        // BookAuthors에서 저자명 추출
        String author = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getName())
                .collect(Collectors.joining(", "));

        return new BookSummary(
                bookId,
                book.getTitle(),
                author.isEmpty() ? null : author,
                book.getPublisherName(),
                book.getThumbnailUrl(),
                ranking
        );
    }

    /**
     * 빈 Books 엔티티 생성 (최초 저장용)
     */
    private Books createEmptyBook(String title) {
        return Books.builder()
                .title(title)
                .build();
    }

    /**
     * ISBN 문자열 파싱 (카카오 API 형식: "isbn10 isbn13")
     *
     * @param isbnRaw "9788936434267 8936434268" 형식
     * @return [isbn10, isbn13]
     */
    private String[] parseIsbn(String isbnRaw) {
        if (isbnRaw == null || isbnRaw.trim().isEmpty()) {
            return new String[]{null, null};
        }

        String[] parts = isbnRaw.trim().split("\\s+");
        if (parts.length == 2) {
            // 길이로 판단: 10자리 = ISBN-10, 13자리 = ISBN-13
            String first = parts[0];
            String second = parts[1];

            if (first.length() == 10 && second.length() == 13) {
                return new String[]{first, second};
            } else if (first.length() == 13 && second.length() == 10) {
                return new String[]{second, first};
            }
        }

        // 하나만 있는 경우
        if (parts.length == 1) {
            String isbn = parts[0];
            if (isbn.length() == 10) {
                return new String[]{isbn, null};
            } else if (isbn.length() == 13) {
                return new String[]{null, isbn};
            }
        }

        return new String[]{null, null};
    }

    /**
     * 도서 정보 전달용 레코드
     */
    public record BookInfo(
            Long bookId,
            String title,
            Integer ranking
    ) {}
}


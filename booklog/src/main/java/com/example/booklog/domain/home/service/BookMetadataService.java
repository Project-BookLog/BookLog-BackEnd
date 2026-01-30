package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.BookSummary;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.BookSource;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈 화면용 도서 메타데이터 관리 서비스
 *
 * [1차 구현 전략 - 동기 방식]
 * 1. DB 우선 조회
 * 2. DB에 없으면 카카오 API 동기 호출
 * 3. 카카오 API 응답을 DB에 저장
 * 4. 결과 반환
 *
 * [추후 개선 예정]
 * - Redis 캐싱
 * - 비동기 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookMetadataService {

    private final BooksRepository booksRepository;
    private final KakaoBookClient kakaoBookClient;

    /**
     * 여러 도서 일괄 조회 (홈 화면용)
     *
     * @param bookInfoList (bookId, title, ranking) 리스트
     * @return BookSummary 리스트
     */
    public List<BookSummary> getBookSummaries(List<BookInfo> bookInfoList) {
        log.info("도서 메타데이터 일괄 조회 시작: {} 건", bookInfoList.size());

        // 1. title 목록 추출
        List<String> titles = bookInfoList.stream()
                .map(BookInfo::title)
                .distinct()
                .collect(Collectors.toList());

        // 2. DB 일괄 조회
        List<Books> books = booksRepository.findAllByTitleIn(titles);
        Map<String, Books> bookMap = books.stream()
                .collect(Collectors.toMap(Books::getTitle, b -> b));

        // 3. BookSummary 변환
        List<BookSummary> results = new ArrayList<>();
        for (BookInfo info : bookInfoList) {
            Books book = bookMap.get(info.title);

            if (book != null) {
                // DB에 있음 → 바로 변환
                results.add(createBookSummary(info.bookId, book, info.ranking));
            } else {
                // DB에 없음 → 카카오 API 호출 후 저장
                Books newBook = fetchAndSaveFromKakao(info.title);
                if (newBook != null) {
                    results.add(createBookSummary(info.bookId, newBook, info.ranking));
                } else {
                    // 카카오 API에서도 못 찾음 → null 데이터 반환
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
        }

        log.info("도서 메타데이터 일괄 조회 완료: {} 건", results.size());
        return results;
    }

    /**
     * 카카오 API에서 도서 정보를 가져와서 DB에 저장
     * 별도 트랜잭션으로 실행 (읽기 전용 트랜잭션과 분리)
     *
     * @param title 도서명
     * @return 저장된 Books 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Books fetchAndSaveFromKakao(String title) {
        try {
            log.info("카카오 API 호출 시작: title={}", title);

            // 카카오 API 호출
            KakaoBookSearchResponse response = kakaoBookClient.search(title, 1, 1).block();

            if (response == null || response.getDocuments().isEmpty()) {
                log.warn("카카오 API 응답 없음: title={}", title);
                return null;
            }

            // 첫 번째 결과로 Books 엔티티 생성
            KakaoBookSearchResponse.Document doc = response.getDocuments().get(0);

            // ISBN 파싱
            String[] isbns = parseIsbn(doc.getIsbn());
            String isbn10 = isbns[0];
            String isbn13 = isbns[1];

            // Books 엔티티 생성
            Books book = Books.builder()
                    .title(doc.getTitle())
                    .description(doc.getContents())
                    .thumbnailUrl(doc.getThumbnail())
                    .detailUrl(doc.getUrl())
                    .publisherName(doc.getPublisher())
                    .isbn(doc.getIsbn())
                    .isbn10(isbn10)
                    .isbn13(isbn13)
                    .source(BookSource.KAKAO)
                    .build();

            // DB 저장
            Books saved = booksRepository.save(book);
            log.info("카카오 API 응답 DB 저장 완료: title={}, bookId={}", title, saved.getId());

            return saved;

        } catch (Exception e) {
            log.error("카카오 API 호출 또는 저장 실패: title={}, error={}", title, e.getMessage(), e);
            return null;
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


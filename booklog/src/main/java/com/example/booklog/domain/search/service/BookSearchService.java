package com.example.booklog.domain.search.service;

import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.BookImportService;
import com.example.booklog.domain.search.dto.BookSortType;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 도서 검색 서비스
 * - 카카오 API를 통한 초기 데이터 임포트
 * - DB 기반 검색 및 정렬 (최신순, 오래된순, 제목순, 저자순)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final BookImportService bookImportService;
    private final BooksRepository booksRepository;

    /**
     * 도서 검색 (정렬 기능 지원)
     *
     * [검색 전략]
     * 1. DB에서 검색 시도 (제목 LIKE 검색)
     * 2. 결과가 없으면 카카오 API로 임포트
     * 3. DB에서 재검색 (정렬 적용)
     *
     * [정렬 옵션]
     * - latest: 최신순 (출판일 내림차순)
     * - oldest: 오래된순 (출판일 오름차순)
     * - title: 제목순 (가나다순)
     * - author: 저자순 (첫 번째 저자 기준)
     *
     * @param query 검색어
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortType 정렬 기준 (기본값: latest)
     * @return 검색 결과
     */
    @Transactional
    public BookSearchResponse searchBooks(String query, int page, int size, BookSortType sortType) {
        log.info("도서 검색 요청 - query: {}, page: {}, size: {}, sort: {}",
                 query, page, size, sortType.getValue());

        // 검색어가 없으면 빈 배열 반환 (200 OK)
        if (query == null || query.trim().isEmpty()) {
            log.info("검색어 없음 - 빈 결과 반환");
            return new BookSearchResponse(page, size, true, 0, List.of());
        }

        // 입력 검증 (검색어 길이, 페이지 검증)
        validateSearchInput(query, page, size);

        // 정렬 기준 생성
        Sort sort = createSort(sortType);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // 1. DB에서 검색
        Page<Books> booksPage = booksRepository.searchByTitle(query.trim(), pageable);

        // 2. 결과가 없으면 카카오 API로 임포트
        if (booksPage.isEmpty()) {
            log.info("DB에 결과 없음. 카카오 API로 임포트 시작 - query: {}", query);
            bookImportService.searchAndUpsert(query, 1, 10); // 카카오 API는 최대 10개만 가져옴

            // 3. 임포트 후 재검색
            booksPage = booksRepository.searchByTitle(query.trim(), pageable);

            if (booksPage.isEmpty()) {
                log.info("임포트 후에도 검색 결과 없음 - query: {}", query);
                return new BookSearchResponse(page, size, true, 0, List.of());
            }
        }

        // 4. DTO 변환
        List<BookSearchItemResponse> items = booksPage.getContent().stream()
                .map(this::convertToSearchItem)
                .toList();

        boolean isEnd = booksPage.isLast();
        long totalCount = booksPage.getTotalElements();

        log.info("도서 검색 완료 - 총 {}권 중 {}권 조회", totalCount, items.size());

        return new BookSearchResponse(page, size, isEnd, (int) totalCount, items);
    }

    /**
     * 도서 검색 (정렬 기준 없이 호출 시 기본값 latest 적용)
     * 하위 호환성을 위한 오버로딩
     */
    public BookSearchResponse searchBooks(String query, int page, int size) {
        return searchBooks(query, page, size, BookSortType.LATEST);
    }

    /**
     * 정렬 기준에 따른 Sort 객체 생성
     *
     * @param sortType 정렬 기준
     * @return Sort 객체
     */
    private Sort createSort(BookSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(
                Sort.Order.desc("publishedDate").nullsLast(),
                Sort.Order.desc("id")
            );
            case OLDEST -> Sort.by(
                Sort.Order.asc("publishedDate").nullsLast(),
                Sort.Order.asc("id")
            );
            case TITLE -> Sort.by(
                Sort.Order.asc("title")
            );
            case AUTHOR -> Sort.by(
                Sort.Order.asc("bookAuthors.author.name"),
                Sort.Order.asc("id")
            );
        };
    }

    /**
     * Books 엔티티를 BookSearchItemResponse로 변환
     */
    private BookSearchItemResponse convertToSearchItem(Books book) {
        // 저자명 추출 (displayOrder 순서대로)
        List<String> authors = book.getBookAuthors().stream()
                .filter(ba -> ba.getRole() == com.example.booklog.domain.library.books.entity.AuthorRole.AUTHOR)
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(ba -> ba.getAuthor().getName())
                .toList();

        // 역자명 추출
        List<String> translators = book.getBookAuthors().stream()
                .filter(ba -> ba.getRole() == com.example.booklog.domain.library.books.entity.AuthorRole.TRANSLATOR)
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(ba -> ba.getAuthor().getName())
                .toList();

        return new BookSearchItemResponse(
                book.getId(),
                book.getTitle(),
                book.getThumbnailUrl(),
                book.getPublisherName(),
                book.getIsbn13(),
                authors,
                translators,
                book.getPublishedDate()
        );
    }

    /**
     * 검색 입력값 검증
     *
     * @throws GeneralException 검색어가 유효하지 않은 경우
     */
    private void validateSearchInput(String query, int page, int size) {
        // 검색어는 이미 상단에서 빈 값 체크 완료, 여기서는 길이만 검증
        if (query.trim().length() > 100) {
            throw new GeneralException(ErrorStatus.SEARCH_KEYWORD_TOO_LONG);
        }

        if (page < 1) {
            throw new GeneralException(ErrorStatus.PAGE_NUMBER_INVALID);
        }

        if (size < 1 || size > 100) {
            throw new GeneralException(ErrorStatus.PAGE_SIZE_INVALID);
        }
    }
}

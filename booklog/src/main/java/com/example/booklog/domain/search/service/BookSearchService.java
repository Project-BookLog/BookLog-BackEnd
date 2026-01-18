package com.example.booklog.domain.search.service;

import com.example.booklog.domain.library.books.service.BookImportService;
import com.example.booklog.domain.search.dto.BookSearchItemResponse;
import com.example.booklog.domain.search.dto.BookSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 도서 검색 서비스
 * - 카카오 API를 통한 도서 검색 및 DB 업서트
 */
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final BookImportService bookImportService;

    /**
     * 도서 검색 및 업서트
     * @param query 검색어
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색 결과
     */
    public BookSearchResponse searchBooks(String query, int page, int size) {
        // 기존 BookImportService의 검색 로직 활용
        com.example.booklog.domain.library.books.dto.BookSearchResponse legacyResponse =
                bookImportService.searchAndUpsert(query, page, size);

        // DTO 변환 (legacy -> search domain)
        return convertToSearchResponse(legacyResponse);
    }

    /**
     * legacy DTO를 search domain DTO로 변환
     */
    private BookSearchResponse convertToSearchResponse(
            com.example.booklog.domain.library.books.dto.BookSearchResponse legacyResponse) {

        var items = legacyResponse.items().stream()
                .map(this::convertToSearchItem)
                .toList();

        return new BookSearchResponse(
                legacyResponse.page(),
                legacyResponse.size(),
                legacyResponse.isEnd(),
                legacyResponse.totalCount(),
                items
        );
    }

    private BookSearchItemResponse convertToSearchItem(
            com.example.booklog.domain.library.books.dto.BookSearchItemResponse legacyItem) {

        return new BookSearchItemResponse(
                legacyItem.bookId(),
                legacyItem.title(),
                legacyItem.thumbnailUrl(),
                legacyItem.publisherName(),
                legacyItem.isbn13(),
                legacyItem.authors(),
                legacyItem.translators(),
                legacyItem.publishedAt()
        );
    }
}


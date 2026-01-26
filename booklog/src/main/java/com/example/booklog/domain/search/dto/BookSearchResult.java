package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.BookSearchResponse;

import java.util.List;

/**
 * 통합 검색 응답 내 도서 검색 결과
 *
 * [설계 근거]
 * - 전체 검색 탭에서는 도서의 기본 정보만 노출
 * - totalCount를 통해 "도서 결과 n권" 표시 및 "더보기" 판단
 * - 페이징 정보는 제외 (전체 탭에서는 제한된 개수만 노출)
 *
 * @param totalCount 총 검색된 도서 수
 * @param items 도서 정보 리스트 (최대 5개 등 제한)
 */
public record BookSearchResult(
        int totalCount,
        List<BookSearchItemResponse> items
) {
    /**
     * BookSearchResponse로부터 변환
     *
     * @param response 도서 검색 응답
     * @return 도서 검색 결과
     */
    public static BookSearchResult from(BookSearchResponse response) {
        return new BookSearchResult(
                response.totalCount(),
                response.items()
        );
    }
}


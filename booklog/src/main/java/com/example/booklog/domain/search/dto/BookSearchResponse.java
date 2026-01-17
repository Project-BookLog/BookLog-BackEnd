package com.example.booklog.domain.search.dto;

import java.util.List;

/**
 * 도서 검색 응답 DTO
 */
public record BookSearchResponse(
        int page,
        int size,
        boolean isEnd,
        int totalCount,
        List<BookSearchItemResponse> items
) {}


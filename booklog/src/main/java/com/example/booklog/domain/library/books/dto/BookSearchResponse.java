package com.example.booklog.domain.library.books.dto;

import java.util.List;

public record BookSearchResponse(
        int page,
        int size,
        boolean isEnd,
        int totalCount,
        List<BookSearchItemResponse> items
) {}

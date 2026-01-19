package com.example.booklog.domain.search.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 도서 검색 결과 항목 DTO
 */
public record BookSearchItemResponse(
        Long bookId,
        String title,
        String thumbnailUrl,
        String publisherName,
        String isbn13,
        List<String> authors,
        List<String> translators,
        LocalDateTime publishedAt
) {}


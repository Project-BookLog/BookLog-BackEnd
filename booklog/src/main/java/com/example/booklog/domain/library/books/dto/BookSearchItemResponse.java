package com.example.booklog.domain.library.books.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

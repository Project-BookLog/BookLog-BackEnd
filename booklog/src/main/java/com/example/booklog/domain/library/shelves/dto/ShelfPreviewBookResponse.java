package com.example.booklog.domain.library.shelves.dto;

public record ShelfPreviewBookResponse(
        Long bookId,
        String title,
        String thumbnailUrl,
        String authorName,
        String publisherName
) {}

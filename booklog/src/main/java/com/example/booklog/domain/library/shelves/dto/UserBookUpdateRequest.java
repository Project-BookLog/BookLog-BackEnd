package com.example.booklog.domain.library.shelves.dto;

public record UserBookUpdateRequest(
        Long shelfId,
        String status
) {}
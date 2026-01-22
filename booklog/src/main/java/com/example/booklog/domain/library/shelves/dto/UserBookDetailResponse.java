package com.example.booklog.domain.library.shelves.dto;

import java.time.LocalDate;

public record UserBookDetailResponse(
        Long userBookId,
        String status,
        int progressPercent,
        Integer currentPage,
        LocalDate startDate,
        LocalDate endDate,
        String format,
        Integer pageCountSnapshot,

        Long bookId,
        String title,
        String contents,
        String thumbnailUrl,
        String publisherName,
        LocalDate publishedAt,
        String kakaoUrl
) {}

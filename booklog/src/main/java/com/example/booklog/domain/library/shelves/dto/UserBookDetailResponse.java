package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.BookFormat;
import com.example.booklog.domain.library.shelves.entity.ReadingStatus;

import java.time.LocalDate;

public record UserBookDetailResponse(
        Long userBookId,
        ReadingStatus status,
        int progressPercent,
        Integer currentPage,
        LocalDate startDate,
        LocalDate endDate,
        BookFormat format,
        Integer pageCountSnapshot,

        Long bookId,
        String title,
        String contents,
        String thumbnailUrl,
        String publisherName,
        LocalDate publishedAt,
        String kakaoUrl
) {}

package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.shelves.entity.ReadingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record UserBookListItemResponse(
        Long userBookId,
        ReadingStatus status,
        int progressPercent,
        Integer currentPage,

        Long bookId,
        String title,
        String thumbnailUrl,
        String publisherName,
        String authorName
) {}
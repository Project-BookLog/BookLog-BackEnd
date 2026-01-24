package com.example.booklog.domain.library.shelves.dto;

import java.time.LocalDate;

public record ReadingLogResponse(
        Long logId,
        Long userBookId,
        LocalDate readDate,
        Integer pagesRead,
        Integer currentPage
) {}

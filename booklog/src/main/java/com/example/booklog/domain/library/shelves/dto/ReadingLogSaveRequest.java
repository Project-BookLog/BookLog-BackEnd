package com.example.booklog.domain.library.shelves.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReadingLogSaveRequest(
        @NotNull LocalDate readDate,
        @NotNull @Min(0) Integer pagesRead
) {}

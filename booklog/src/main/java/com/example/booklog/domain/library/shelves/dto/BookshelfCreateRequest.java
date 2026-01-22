package com.example.booklog.domain.library.shelves.dto;

import jakarta.validation.constraints.NotBlank;

public record BookshelfCreateRequest(
        @NotBlank String name,
        Boolean isPublic,
        String sortOrder // LATEST/OLDEST/TITLE/AUTHOR (서재 기본 정렬 프리셋)
) {}
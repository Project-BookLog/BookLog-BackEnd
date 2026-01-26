package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.UserBookSort;
import jakarta.validation.constraints.NotBlank;

public record BookshelfCreateRequest(
        @NotBlank String name,
        Boolean isPublic,
        UserBookSort sortOrder // LATEST/OLDEST/TITLE/AUTHOR (서재 기본 정렬 프리셋)
) {}
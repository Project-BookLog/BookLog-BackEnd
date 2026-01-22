package com.example.booklog.domain.library.shelves.dto;

import jakarta.validation.constraints.Size;

public record BookshelfUpdateRequest(
        @Size(max = 50)
        String name,

        Boolean isPublic,

        String sortOrder
) {}

package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.UserBookSort;
import jakarta.validation.constraints.Size;

public record BookshelfUpdateRequest(
        @Size(max = 50)
        String name,

        Boolean isPublic,

        UserBookSort sortOrder
) {}

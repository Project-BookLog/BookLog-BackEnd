package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.ReadingStatus;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record UserBookCreateRequest(
        @NotNull Long bookId,
        @Nullable Long shelfId,
        String status // TO_READ/READING/DONE/STOPPED
) {}

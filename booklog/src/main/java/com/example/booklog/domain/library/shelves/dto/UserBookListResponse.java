package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.dto.UserBookListItemResponse;

import java.util.List;

public record UserBookListResponse(
        long totalCount,
        List<UserBookListItemResponse> items
) {}

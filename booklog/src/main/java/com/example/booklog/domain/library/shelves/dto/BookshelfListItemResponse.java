package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.UserBookSort;

import java.util.List;

public record BookshelfListItemResponse(
        Long shelfId,
        String name,
        boolean isPublic,
        UserBookSort sortOrder,
        List<ShelfPreviewBookResponse> previewBooks // ✅ 서재 내 상위 3권 프리뷰
) {}

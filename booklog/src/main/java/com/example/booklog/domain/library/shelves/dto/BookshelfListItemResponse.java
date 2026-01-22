package com.example.booklog.domain.library.shelves.dto;

import java.util.List;

public record BookshelfListItemResponse(
        Long shelfId,
        String name,
        boolean isPublic,
        String sortOrder,
        List<ShelfPreviewBookResponse> previewBooks // ✅ 서재 내 상위 3권 프리뷰
) {}

package com.example.booklog.domain.users.dto;

import java.util.List;

/** 공개 서재 목록 응답(대표 3권 포함) */
public record UserPublicShelfListResponse(
        int totalCount,
        List<UserPublicShelfItem> items
) {
    /** 서재 카드 1개 */
    public record UserPublicShelfItem(
            Long shelfId,
            String name,
            int bookCount,
            List<ShelfBookPreview> topBooks
    ) {}

    /** 서재 카드에 보여줄 책 프리뷰 1개 */
    public record ShelfBookPreview(
            Long bookId,
            String thumbnailUrl,
            String publisherName,
            String authorName
    ) {}

    /** 특정 서재 도서 전체 목록 응답(페이징 없음) */
    public record UserPublicShelfBooksResponse(
            int totalCount,
            List<UserPublicShelfBookItem> items
    ) {}

    /** 서재 내 도서 1개(상태 없음) */
    public record UserPublicShelfBookItem(
            Long bookId,
            String thumbnailUrl,
            String publisherName,
            String authorName
    ) {}
}

package com.example.booklog.domain.home.dto;

/**
 * 홈 화면에서 사용되는 도서 요약 정보 DTO
 * 모든 섹션에서 재사용됨
 */
public record BookSummary(
        Long bookId,          // 서버 DB Book 테이블의 PK
        String title,         // 도서명
        String author,        // 저자 (nullable)
        String publisher,     // 출판사 (nullable)
        String coverImageUrl, // 표지 이미지 URL (nullable)
        Integer ranking       // 순위 정보 (실시간 랭킹에서만 사용, 나머지는 null)
) {
}


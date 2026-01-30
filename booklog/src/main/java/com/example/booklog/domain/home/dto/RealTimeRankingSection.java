package com.example.booklog.domain.home.dto;

import java.util.List;

/**
 * 실시간 랭킹 섹션
 * 2030 인기 도서 TOP 20
 */
public record RealTimeRankingSection(
        String sectionTitle,        // "2030 인기 도서 TOP 20"
        List<BookSummary> rankings  // TOP 20 도서 목록 (ranking 필드 포함)
) {
}


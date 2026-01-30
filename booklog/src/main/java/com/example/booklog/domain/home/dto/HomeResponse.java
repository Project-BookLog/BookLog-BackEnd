package com.example.booklog.domain.home.dto;

import java.util.List;

/**
 * 홈 화면 전체 응답 DTO
 * GET /api/v1/home의 단일 응답 객체
 */
public record HomeResponse(
        RealTimeRankingSection realTimeRanking,              // 실시간 랭킹 섹션
        List<TaggedBooksSection> moodBestsellers,            // 분위기별 베스트셀러
        List<TaggedBooksSection> writingStyleBestsellers,    // 문체별 베스트셀러
        List<TaggedBooksSection> immersionBestsellers        // 몰입도별 베스트셀러
) {
}


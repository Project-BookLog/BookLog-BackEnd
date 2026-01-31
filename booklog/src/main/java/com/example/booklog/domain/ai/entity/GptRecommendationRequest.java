package com.example.booklog.domain.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPT 추천 요청 DTO
 * - 온보딩 필드 + 최근 검색어를 기반으로 작가/장르 추론
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GptRecommendationRequest {

    /**
     * 추천 근거 필드명 (예: preferredMood1)
     */
    private String fieldName;

    /**
     * 추천 근거 값 (예: CALM)
     */
    private String fieldValue;

    /**
     * 필드 설명 (예: 잔잔한)
     */
    private String fieldDescription;

    /**
     * 사용자 최근 검색어 목록
     */
    private String recentSearchKeywords;
}


package com.example.booklog.domain.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GPT 추천 응답 DTO
 * - 검색 키워드 생성용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GptRecommendationResponse {

    /**
     * 추천 작가 타입 또는 작가명
     * 예: "박민규 같은 작가", "실험적인 작가"
     */
    private String authorType;

    /**
     * 추천 장르
     * 예: "현대문학", "SF"
     */
    private String genre;

    /**
     * GPT가 생성한 검색 키워드
     * 예: "실험적 현대문학"
     */
    private String searchKeyword;
}


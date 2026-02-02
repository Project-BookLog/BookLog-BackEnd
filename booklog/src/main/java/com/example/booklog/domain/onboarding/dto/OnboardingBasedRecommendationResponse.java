package com.example.booklog.domain.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 온보딩 기반 도서 추천 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingBasedRecommendationResponse {

    /**
     * 추천 카드 목록 (최대 6개)
     */
    private List<BookRecommendationCardResponse> recommendations;

    /**
     * 추천 생성에 사용된 온보딩 필드 개수
     */
    private int usedFieldCount;

    /**
     * 총 추천 카드 개수
     */
    private int totalRecommendations;
}


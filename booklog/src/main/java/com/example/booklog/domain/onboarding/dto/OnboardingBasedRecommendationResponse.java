package com.example.booklog.domain.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 온보딩 기반 도서 추천 응답 DTO
 * - 작가별, 장르별, 분위기별 3개 섹션으로 구성
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "온보딩 기반 도서 추천 응답")
public class OnboardingBasedRecommendationResponse {

    /**
     * 작가 기반 추천 섹션
     */
    @Schema(description = "작가 기반 추천 도서 목록")
    private RecommendationSection authorSection;

    /**
     * 장르 기반 추천 섹션
     */
    @Schema(description = "장르 기반 추천 도서 목록")
    private RecommendationSection genreSection;

    /**
     * 분위기 기반 추천 섹션
     */
    @Schema(description = "분위기 기반 추천 도서 목록")
    private RecommendationSection moodSection;

    /**
     * 추천 섹션 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 섹션")
    public static class RecommendationSection {

        /**
         * 섹션 제목
         */
        @Schema(description = "섹션 제목", example = "이런 작가는 어떠세요?")
        private String title;

        /**
         * 섹션 설명
         */
        @Schema(description = "섹션 설명", example = "선택하신 취향을 바탕으로 추천드립니다")
        private String description;

        /**
         * 추천 도서 목록
         */
        @Schema(description = "추천 도서 목록")
        private List<BookRecommendationCardResponse> books;
    }
}


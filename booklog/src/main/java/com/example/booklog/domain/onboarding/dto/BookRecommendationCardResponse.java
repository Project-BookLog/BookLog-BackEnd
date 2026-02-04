package com.example.booklog.domain.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도서 추천 카드 응답 DTO
 * - 온보딩 기반 개인화 추천 카드
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRecommendationCardResponse {

    /**
     * 도서 제목
     */
    private String bookTitle;

    /**
     * 저자
     */
    private String author;

    /**
     * 출판사
     */
    private String publisher;

    /**
     * 썸네일 URL
     */
    private String thumbnailUrl;

    /**
     * 추천 근거 필드명
     * 예: "preferredMood1", "sentenceBreath"
     */
    private String recommendationSourceField;

    /**
     * 추천 근거 값
     * 예: "CALM", "CONCISE"
     */
    private String recommendationSourceValue;

    /**
     * 분위기 키워드
     */
    private String moodKeyword;

    /**
     * 문체 키워드
     */
    private String styleKeyword;

    /**
     * 몰입도 키워드
     */
    private String immersionKeyword;
}


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
     * 도서 ID
     */
    private Long bookId;

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
     * 키워드 1 (온보딩 기반: 분위기, 서재 기반: 작가)
     */
    private String keyword1;

    /**
     * 키워드 2 (온보딩 기반: 문체, 서재 기반: 장르)
     */
    private String keyword2;

    /**
     * 키워드 3 (온보딩 기반: 몰입도, 서재 기반: 분위기)
     */
    private String keyword3;

    /**
     * 분위기 키워드 (deprecated - 하위 호환성을 위해 유지)
     */
    @Deprecated
    private String moodKeyword;

    /**
     * 문체 키워드 (deprecated - 하위 호환성을 위해 유지)
     */
    @Deprecated
    private String styleKeyword;

    /**
     * 몰입도 키워드 (deprecated - 하위 호환성을 위해 유지)
     */
    @Deprecated
    private String immersionKeyword;
}


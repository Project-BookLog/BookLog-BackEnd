package com.example.booklog.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 추천 검색어 목록 응답 DTO
 */
@Getter
@AllArgsConstructor
public class RecommendationSearchResponse {

    private List<RecommendedKeywordResponse> keywords;

    public static RecommendationSearchResponse of(List<RecommendedKeywordResponse> keywords) {
        return new RecommendationSearchResponse(keywords);
    }
}


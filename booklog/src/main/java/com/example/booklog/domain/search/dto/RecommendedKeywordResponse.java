package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.search.entity.RecommendedKeyword;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 추천 검색어 응답 DTO
 */
@Getter
@AllArgsConstructor
public class RecommendedKeywordResponse {

    private Long id;
    private String keyword;
    private String type;
    private String description;

    public static RecommendedKeywordResponse from(RecommendedKeyword recommendedKeyword) {
        return new RecommendedKeywordResponse(
                recommendedKeyword.getId(),
                recommendedKeyword.getKeyword(),
                recommendedKeyword.getType().name(),
                recommendedKeyword.getDescription()
        );
    }
}


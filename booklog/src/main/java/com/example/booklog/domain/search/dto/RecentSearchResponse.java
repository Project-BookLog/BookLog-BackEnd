package com.example.booklog.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 최근 검색어 목록 응답 DTO
 */
@Getter
@AllArgsConstructor
public class RecentSearchResponse {

    private List<SearchKeywordResponse> keywords;

    public static RecentSearchResponse of(List<SearchKeywordResponse> keywords) {
        return new RecentSearchResponse(keywords);
    }
}

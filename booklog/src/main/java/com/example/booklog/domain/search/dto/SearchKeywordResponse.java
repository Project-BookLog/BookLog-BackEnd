package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.search.entity.SearchKeyword;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 검색어 응답 DTO
 */
@Getter
@AllArgsConstructor
public class SearchKeywordResponse {

    private Long id;
    private String keyword;
    private LocalDateTime searchedAt;

    public static SearchKeywordResponse from(SearchKeyword searchKeyword) {
        return new SearchKeywordResponse(
                searchKeyword.getId(),
                searchKeyword.getKeyword(),
                searchKeyword.getCreatedAt()
        );
    }
}


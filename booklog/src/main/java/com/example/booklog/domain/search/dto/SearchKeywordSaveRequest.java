package com.example.booklog.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 검색어 저장 요청 DTO
 */
@Getter
@AllArgsConstructor
public class SearchKeywordSaveRequest {
    
    private String keyword;
    
    public String getKeyword() {
        return keyword != null ? keyword.trim() : null;
    }
}


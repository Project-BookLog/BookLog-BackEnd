package com.example.booklog.domain.search.dto;

import java.util.List;

/**
 * 작가 검색 응답 DTO
 *
 * @param page 현재 페이지 번호 (1부터 시작)
 * @param size 페이지 크기
 * @param isEnd 마지막 페이지 여부
 * @param totalCount 총 검색된 작가 수 (UI에서 "총 n명" 표시용)
 * @param items 작가 검색 결과 리스트
 */
public record AuthorSearchResponse(
        int page,
        int size,
        boolean isEnd,
        int totalCount,
        List<AuthorSearchItemResponse> items
) {
    /**
     * 페이지네이션 정보와 함께 응답 생성
     *
     * @param items 작가 검색 결과 리스트
     * @param page 현재 페이지
     * @param size 페이지 크기
     * @param totalCount 전체 작가 수
     * @return AuthorSearchResponse
     */
    public static AuthorSearchResponse of(
            List<AuthorSearchItemResponse> items,
            int page,
            int size,
            long totalCount
    ) {
        boolean isEnd = (long) page * size >= totalCount;
        return new AuthorSearchResponse(
                page,
                size,
                isEnd,
                (int) totalCount,
                items
        );
    }
}


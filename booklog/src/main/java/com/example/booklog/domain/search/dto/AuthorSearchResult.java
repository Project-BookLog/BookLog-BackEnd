package com.example.booklog.domain.search.dto;

import java.util.List;

/**
 * 통합 검색 응답 내 작가 검색 결과
 *
 * [설계 근거]
 * - 전체 검색 탭에서는 작가의 요약 정보만 필요 (책 목록 불포함)
 * - totalCount를 통해 "작가 결과 n명" 표시 및 "더보기" 판단
 * - 페이징 정보는 제외 (전체 탭에서는 제한된 개수만 노출)
 *
 * @param totalCount 총 검색된 작가 수
 * @param items 작가 요약 정보 리스트 (최대 5개 등 제한)
 */
public record AuthorSearchResult(
        int totalCount,
        List<AuthorSummary> items
) {
    /**
     * AuthorSearchResponse로부터 변환
     * 작가 검색 결과에서 책 목록을 제외한 요약 정보만 추출
     *
     * @param response 작가 검색 응답
     * @return 작가 검색 결과 (요약)
     */
    public static AuthorSearchResult from(AuthorSearchResponse response) {
        List<AuthorSummary> summaries = response.items().stream()
                .map(AuthorSummary::from)
                .toList();

        return new AuthorSearchResult(
                response.totalCount(),
                summaries
        );
    }
}


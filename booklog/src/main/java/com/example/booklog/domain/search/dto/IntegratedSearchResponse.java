package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;

/**
 * 통합 검색 응답 DTO
 * 작가 검색 결과와 도서 검색 결과를 통합하여 반환
 *
 * [설계 근거]
 * - 작가/도서 검색 결과를 각각 독립적인 구조로 분리하여 UI에서 영역별 렌더링 용이
 * - 각 결과의 totalCount를 제공하여 UI에서 "더보기" 표시 여부 판단 가능
 * - 검색어와 정렬 기준을 응답에 포함하여 클라이언트 측 상태 관리 간소화
 * - 향후 '작가' 탭, '도서' 탭 분리 시 각각의 API와 구조 재사용 가능
 *
 * @param query 검색어
 * @param sort 정렬 기준 (latest, popular 등)
 * @param authors 작가 검색 결과
 * @param books 도서 검색 결과
 */
public record IntegratedSearchResponse(
        String query,
        String sort,
        AuthorSearchResult authors,
        BookSearchResult books
) {
    /**
     * 작가/도서 검색 결과로부터 통합 응답 생성
     *
     * @param query 검색어
     * @param sort 정렬 기준
     * @param authorResponse 작가 검색 응답
     * @param bookResponse 도서 검색 응답
     * @return 통합 검색 응답
     */
    public static IntegratedSearchResponse of(
            String query,
            String sort,
            AuthorSearchResponse authorResponse,
            BookSearchResponse bookResponse
    ) {
        // 작가 검색 결과 변환 (책 목록 제외한 요약 정보만)
        AuthorSearchResult authorResult = AuthorSearchResult.from(authorResponse);

        // 도서 검색 결과 변환
        BookSearchResult bookResult = BookSearchResult.from(bookResponse);

        return new IntegratedSearchResponse(query, sort, authorResult, bookResult);
    }
}


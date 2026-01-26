package com.example.booklog.domain.search.dto;

/**
 * 통합 검색 응답에서 사용되는 작가 요약 정보
 *
 * [설계 근거]
 * - 전체 탭에서는 작가의 대표작 목록이 필요 없음
 * - UI 표시에 필요한 최소한의 정보만 포함 (이름, 프로필 이미지, 직업, 국적)
 * - 작가 상세 페이지로 이동하기 위한 authorId 포함
 *
 * @param authorId 작가 ID
 * @param name 작가명
 * @param profileImageUrl 프로필 이미지 URL
 * @param occupation 직업 (예: 소설가, 시인)
 * @param nationality 국적
 */
public record AuthorSummary(
        Long authorId,
        String name,
        String profileImageUrl,
        String occupation,
        String nationality
) {
    /**
     * AuthorSearchItemResponse로부터 변환
     * 책 목록을 제외한 작가 정보만 추출
     *
     * @param item 작가 검색 아이템
     * @return 작가 요약 정보
     */
    public static AuthorSummary from(AuthorSearchItemResponse item) {
        return new AuthorSummary(
                item.authorId(),
                item.name(),
                item.profileImageUrl(),
                item.occupation(),
                item.nationality()
        );
    }
}


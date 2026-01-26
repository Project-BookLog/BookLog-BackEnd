package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.library.books.entity.Authors;
import java.util.List;

/**
 * 작가 검색 결과 아이템 DTO
 * 작가 기본 정보 + 대표작 최대 2권
 *
 * @param authorId 작가 ID
 * @param name 작가명
 * @param profileImageUrl 프로필 이미지 URL
 * @param occupation 직업 (예: 소설가, 시인, 극작가)
 * @param nationality 국적
 * @param biography 작가 소개
 * @param books 해당 작가의 도서 목록 (최대 2권)
 */
public record AuthorSearchItemResponse(
        Long authorId,
        String name,
        String profileImageUrl,
        String occupation,
        String nationality,
        String biography,
        List<AuthorBookResponse> books
) {
    /**
     * Authors 엔티티로부터 DTO 생성
     * Wikidata JSON에서 occupation, nationality 추출
     *
     * @param author 작가 엔티티
     * @param books 작가의 도서 목록 (최대 2권)
     * @return AuthorSearchItemResponse
     */
    public static AuthorSearchItemResponse from(Authors author, List<AuthorBookResponse> books) {
        // TODO: wikidataRawJson에서 occupation, nationality 파싱 로직 추가
        // 현재는 기본값 처리
        String occupation = extractOccupation(author);
        String nationality = extractNationality(author);

        return new AuthorSearchItemResponse(
                author.getId(),
                author.getName(),
                author.getProfileImageUrl(),
                occupation,
                nationality,
                author.getBiography(),
                books
        );
    }

    /**
     * Wikidata JSON에서 occupation 추출
     * 파싱 실패 시 기본값 "작가" 반환
     */
    private static String extractOccupation(Authors author) {
        // TODO: JSON 파싱 로직 구현
        // wikidataRawJson에서 occupation 추출
        return "작가"; // 기본값
    }

    /**
     * Wikidata JSON에서 nationality 추출
     * 파싱 실패 시 null 반환
     */
    private static String extractNationality(Authors author) {
        // TODO: JSON 파싱 로직 구현
        // wikidataRawJson에서 nationality 추출
        return null;
    }
}


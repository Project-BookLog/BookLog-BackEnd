package com.example.booklog.domain.library.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 작가 상세정보 응답 DTO
 */
@Schema(description = "작가 상세정보 응답")
public record AuthorDetailResponse(
        @Schema(description = "작가 ID", example = "1")
        Long authorId,

        @Schema(description = "작가명", example = "이문열")
        String name,

        @Schema(description = "작가 프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "작가에 대한 한 줄 소개", example = "대한민국의 대표 소설가")
        String biography,

        @Schema(description = "해당 작가의 도서 요약 목록")
        List<AuthorBookSummary> books,

        @Schema(description = "작가 프로필 정보")
        AuthorProfile profile,

        @Schema(description = "수상경력 (연도별)")
        List<AuthorAward> awards
) {
    /**
     * 작가의 도서 요약 정보
     */
    @Schema(description = "작가 도서 요약")
    public record AuthorBookSummary(
            @Schema(description = "책 ID", example = "1")
            Long bookId,

            @Schema(description = "책 제목", example = "우리들의 일그러진 영웅")
            String title,

            @Schema(description = "저자명", example = "이문열")
            String authorName,

            @Schema(description = "출판사명", example = "민음사")
            String publisherName,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "책 취향 분석 (분위기, 문체, 몰입도)")
            BookTasteInfo tasteInfo
    ) {}

    /**
     * 책 취향 정보 (분위기, 문체, 몰입도)
     */
    @Schema(description = "책 취향 정보")
    public record BookTasteInfo(
            @Schema(description = "분위기", example = "잔잔한")
            String mood,

            @Schema(description = "문체", example = "간결한")
            String style,

            @Schema(description = "몰입도", example = "편안한")
            String immersion
    ) {}

    /**
     * 작가 프로필 정보
     */
    @Schema(description = "작가 프로필")
    public record AuthorProfile(
            @Schema(description = "학력")
            List<String> education,

            @Schema(description = "데뷔", example = "붉은 닭(1994)")
            String debut,

            @Schema(description = "출생", example = "1970. 11. 27")
            String birthDate,

            @Schema(description = "직업")
            List<String> occupations
    ) {}

    /**
     * 수상경력
     */
    @Schema(description = "수상경력")
    public record AuthorAward(
            @Schema(description = "연도", example = "2022")
            Integer year,

            @Schema(description = "상 이름", example = "제30회 대산문학상 소설부문(작별하지 않는다)")
            String awardName,

            @Schema(description = "수상작", example = "작별하지 않는다")
            String workTitle
    ) {}
}


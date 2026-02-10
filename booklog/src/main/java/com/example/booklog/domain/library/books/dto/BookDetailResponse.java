package com.example.booklog.domain.library.books.dto;

import com.example.booklog.domain.library.books.entity.AuthorRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 책 상세정보 응답 DTO
 */
@Schema(description = "책 상세정보 응답")
public record BookDetailResponse(
        @Schema(description = "책 ID", example = "1")
        Long bookId,

        @Schema(description = "책 제목", example = "클린 코드")
        String title,

        @Schema(description = "책 소개/설명")
        String description,

        @Schema(description = "책 간략 소개 (한 문장)", example = "상실, 사랑 그리고 숨어 있는 삶의 질서에 관한 이야기")
        String shortIntro,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailUrl,

        @Schema(description = "출판사명", example = "인사이트")
        String publisherName,

        @Schema(description = "출판일", example = "2013-12-24")
        LocalDate publishedDate,

        @Schema(description = "ISBN 원문")
        String isbn,

        @Schema(description = "ISBN10")
        String isbn10,

        @Schema(description = "ISBN13")
        String isbn13,

        @Schema(description = "카카오 상세 URL")
        String detailUrl,

        @Schema(description = "저자 목록")
        List<AuthorInfo> authors,

        @Schema(description = "AI 취향 코멘트")
        AiTasteComment aiTasteComment,

        @Schema(description = "상세 취향 분석")
        TasteAnalysis tasteAnalysis,

        @Schema(description = "목차 정보")
        List<String> tableOfContents
) {
    /**
     * 저자 정보 DTO
     */
    @Schema(description = "저자 정보")
    public record AuthorInfo(
            @Schema(description = "저자 ID", example = "1")
            Long authorId,

            @Schema(description = "저자명", example = "로버트 C. 마틴")
            String name,

            @Schema(description = "역할 (AUTHOR: 저자, TRANSLATOR: 역자)", example = "AUTHOR")
            AuthorRole role,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl
    ) {}

    /**
     * AI 취향 코멘트
     */
    @Schema(description = "AI 취향 코멘트")
    public record AiTasteComment(
            @Schema(description = "제목", example = "사유가 깊어지는 문장들")
            String title,

            @Schema(description = "설명", example = "다정한 대화체 속에 숨겨진 서늘한 반전...")
            String description
    ) {}

    /**
     * 상세 취향 분석
     */
    @Schema(description = "상세 취향 분석")
    public record TasteAnalysis(
            @Schema(description = "분위기 분석")
            TasteDetail mood,

            @Schema(description = "문체 분석")
            TasteDetail style,

            @Schema(description = "몰입도 분석")
            TasteDetail immersion
    ) {}

    /**
     * 취향 상세 정보
     */
    @Schema(description = "취향 상세 정보")
    public record TasteDetail(
            @Schema(description = "제목", example = "#몽환적인")
            String title,

            @Schema(description = "설명", example = "수채화처럼 번지는 깊은 여운...")
            String description
    ) {}
}

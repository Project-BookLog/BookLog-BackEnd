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
        List<AuthorInfo> authors
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
}

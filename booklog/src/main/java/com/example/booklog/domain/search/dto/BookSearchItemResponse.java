package com.example.booklog.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 도서 검색 결과 항목 DTO
 */
@Schema(description = "도서 검색 결과 항목")
public record BookSearchItemResponse(
        @Schema(description = "책 ID", example = "1")
        Long bookId,

        @Schema(description = "책 제목", example = "클린 코드")
        String title,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailUrl,

        @Schema(description = "출판사명", example = "인사이트")
        String publisherName,

        @Schema(description = "ISBN13")
        String isbn13,

        @Schema(description = "저자 목록", example = "[\"로버트 C. 마틴\"]")
        List<String> authors,

        @Schema(description = "역자 목록", example = "[\"박재호\", \"이해영\"]")
        List<String> translators,

        @Schema(description = "출판일")
        LocalDateTime publishedAt,

        @Schema(description = "태그 목록 (분위기/문체/몰입도)", example = "[\"#몽환적인\", \"#담백한\", \"#몰입감\"]")
        List<String> tags
) {}


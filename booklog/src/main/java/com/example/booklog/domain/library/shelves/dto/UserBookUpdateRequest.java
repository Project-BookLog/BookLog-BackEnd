package com.example.booklog.domain.library.shelves.dto;

import com.example.booklog.domain.library.shelves.entity.BookFormat;
import com.example.booklog.domain.library.shelves.entity.ReadingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserBookUpdateRequest(
        @Schema(description = "서재 ID(선택). A방식: 해당 서재에 '추가'합니다.", example = "10")
        Long shelfId,

        @Schema(description = "읽기 상태(선택)", example = "READING")
        ReadingStatus status,

        @Schema(description = "책 종류/매체(선택)", example = "EBOOK")
        BookFormat format
) {}

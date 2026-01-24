package com.example.booklog.domain.library.shelves.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TotalPageSaveRequest(
        @Schema(description = "사용자 입력 총 페이지 수(판본/앱 기준)", example = "312")
        @NotNull @Min(1) Integer pageCountSnapshot
) {}

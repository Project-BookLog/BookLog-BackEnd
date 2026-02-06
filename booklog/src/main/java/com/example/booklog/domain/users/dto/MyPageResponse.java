package com.example.booklog.domain.users.dto;

public record MyPageResponse(
        String month,          // "YYYY-MM"
        String tasteInsight    // ✅ 키스크린: 독서 취향 분석 문구(GPT)
) {}

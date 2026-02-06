// dto
package com.example.booklog.domain.users.dto;

public record CalendarReadingStatusResponse(
        String month,          // "YYYY-MM"
        String tasteInsight    // 캘린더 페이지: 월별 독서 취향/현황 AI 멘트(GPT)
) {}

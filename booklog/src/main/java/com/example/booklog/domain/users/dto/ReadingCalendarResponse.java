package com.example.booklog.domain.users.dto;

import java.time.LocalDate;
import java.util.List;

public record ReadingCalendarResponse(
        String month,           // "YYYY-MM"
        List<DayItem> days
) {
    public record DayItem(
            LocalDate date,
            String thumbnailUrl  // ✅ 날짜별 대표 썸네일 1개
    ) {}
}

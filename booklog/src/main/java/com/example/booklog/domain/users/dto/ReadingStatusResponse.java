package com.example.booklog.domain.users.dto;

import java.util.List;

public record ReadingStatusResponse(
        String month,                 // "2026-01"
        int progressPercent,          // 0~100
        DayProgress dayProgress,      // { currentDay, lastDay }
        List<String> topMoodTags,     // ["분위기", ...] 최대 3개
        String aiSummary              // 회색 글씨 (일단 null 가능)
) {
    public record DayProgress(int currentDay, int lastDay) {}
}

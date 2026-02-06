package com.example.booklog.domain.users.dto;

import java.util.List;

public record ReadingStatusResponse(
        String month,                 // "YYYY-MM"
        int progressPercent,          // 0~100
        DayProgress dayProgress,      // { currentDay, lastDay }
        List<String> topMoodTags,     // Top3
        String aiSummary              // GPT/기본문구
) {
    public record DayProgress(int currentDay, int lastDay) {}
}

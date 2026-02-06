package com.example.booklog.domain.users.dto;

import java.util.List;

public record ReadingStatusResponse(
        String month,                 // "YYYY-MM"
        int progressPercent,          // 0~100
        DayProgress dayProgress,      // { currentDay, lastDay }
        List<String> topMoodTags,     // 분위기 태그 Top3
        String aiSummary              // 캘린더용 회색 문구(UX 라이터 톤)
) {
    public record DayProgress(int currentDay, int lastDay) {}
}

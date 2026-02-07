package com.example.booklog.domain.users.dto;

import java.util.List;

public record KeyScreenInsightResponse(
        String month,             // "YYYY-MM"
        List<String> topMoodTags, // Top3
        String insight            // 문구
) {}

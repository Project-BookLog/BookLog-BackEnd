package com.example.booklog.domain.users.dto;

public record FriendReadingRankingItem(
        int rank,
        Long userId,
        String nickname,
        String profileImageUrl,
        long completedCount,
        long readingDays
) {}

package com.example.booklog.domain.users.dto;

public record MeProfileWithStatsResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        boolean isShelfPublic,
        boolean isBooklogPublic,

        String email,

        long followerCount,
        long followingCount,
        long completedBookCount,
        long myBooklogCount,
        long bookmarkCount
) {}

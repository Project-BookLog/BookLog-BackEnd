package com.example.booklog.domain.users.dto;

public record MeProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        Boolean isShelfPublic,
        Boolean isBooklogPublic
) {}
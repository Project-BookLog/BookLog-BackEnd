package com.example.booklog.domain.users.dto;

public record MeAvatarUpdateResponse(
        Long userId,
        String profileImageUrl
) {}
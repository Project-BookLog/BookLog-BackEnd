package com.example.booklog.domain.users.dto;

public record MutualFriendItem(
        //맞팔 관련 dto
        Long userId,
        String nickname,
        String profileImageUrl
) {}

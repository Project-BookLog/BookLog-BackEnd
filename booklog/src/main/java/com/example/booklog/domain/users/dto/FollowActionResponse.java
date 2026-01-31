package com.example.booklog.domain.users.dto;

import java.time.LocalDateTime;

public record FollowActionResponse(
        Long targetUserId,
        boolean following,      // follow=true / unfollow=false
        boolean isMutual,       // 맞팔 여부
        LocalDateTime followedAt // follow 시각(멱등 처리로 이미 팔로우면 기존 followedAt)
) {}

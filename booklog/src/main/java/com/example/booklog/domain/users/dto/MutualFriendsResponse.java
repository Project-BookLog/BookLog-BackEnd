package com.example.booklog.domain.users.dto;

import java.util.List;

public record MutualFriendsResponse(
        List<MutualFriendItem> items,
        Long nextCursor,     // 다음 요청에 넣을 cursor(마지막 userId)
        boolean hasNext
) {}

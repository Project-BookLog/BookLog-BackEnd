package com.example.booklog.domain.users.dto;

import java.util.List;

public record FriendReadingRankingResponse(
        String month,
        List<FriendReadingRankingItem> top3,
        List<FriendReadingRankingItem> others,
        PageInfo pageInfo
) {
    public record PageInfo(
            int page,
            int size,
            long totalElements)
    {}
}

package com.example.booklog.domain.users.dto;

import java.util.List;

public record FriendReadingRankingTop3Response(
        String month,
        List<FriendReadingRankingItem> top3
) {}

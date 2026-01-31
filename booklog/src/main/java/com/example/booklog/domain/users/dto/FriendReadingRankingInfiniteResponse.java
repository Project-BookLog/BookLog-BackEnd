package com.example.booklog.domain.users.dto;

import java.util.List;

public record FriendReadingRankingInfiniteResponse(
        String month,
        List<FriendReadingRankingItem> top3,   // ✅ 첫 호출에 포함 (전체보기 상단 고정)
        List<FriendReadingRankingItem> items,  // ✅ 4등부터 무한스크롤
        Integer nextCursor,                    // ✅ 다음 요청에 넣을 cursor(마지막 rank)
        boolean hasNext
) {}

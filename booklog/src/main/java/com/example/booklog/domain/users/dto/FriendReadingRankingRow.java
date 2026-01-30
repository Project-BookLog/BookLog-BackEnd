package com.example.booklog.domain.users.dto;

public interface FriendReadingRankingRow {
    Integer getRank();            // 1,2,3... (쿼리에서 계산 권장)
    Long getUserId();
    String getNickname();
    String getProfileImageUrl();
    Long getCompletedCount();     // "N권 읽음"
    Long getReadingDays();        // "N일 기록"
}

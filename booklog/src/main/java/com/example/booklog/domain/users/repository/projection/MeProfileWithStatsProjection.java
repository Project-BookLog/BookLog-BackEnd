package com.example.booklog.domain.users.repository.projection;

public interface MeProfileWithStatsProjection {
    Long getUserId();
    String getNickname();
    String getAvatarUrl();

    Integer getIsShelfPublic();   // 0/1
    Integer getIsBooklogPublic(); // 0/1

    Number getFollowerCount();
    Number getFollowingCount();
    Number getCompletedBookCount();
    Number getMyBooklogCount();
    Number getBookmarkCount();
}

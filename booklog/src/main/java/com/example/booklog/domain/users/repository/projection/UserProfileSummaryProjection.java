package com.example.booklog.domain.users.repository.projection;

public interface UserProfileSummaryProjection {
    Long getUserId();
    String getNickname();
    String getEmail();
    String getAvatarUrl();

    Long getFollowerCount();
    Long getFollowingCount();
    Long getSavedBookCount();
    Long getCompletedBookCount();
    Long getBooklogCount();
    Long getBookmarkCount();

    // ✅ 여기 3개를 Boolean -> Long
    Long getIsFollowing();
    Long getIsShelfPublic();
    Long getIsBooklogPublic();
}

package com.example.booklog.domain.users.dto;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String avatarUrl,

        long followerCount,
        long followingCount,

        long savedBookCount,      // ✅ user_books 전체 권수 (상태 무관)
        long completedBookCount,  // (UI에 있으면) 완독 수
        long booklogCount,        // ✅ 작성한 북로그 수 (posts)
        long bookmarkCount,       // ✅ 북마크 수 (post_bookmarks: user가 한 북마크)

        boolean isFollowing,      // me -> target 팔로잉 여부

        boolean isShelfPublic,    // 공개 토글(프로필에서 보여주면)
        boolean isBooklogPublic
) {}

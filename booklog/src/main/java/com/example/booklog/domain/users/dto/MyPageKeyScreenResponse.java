package com.example.booklog.domain.users.dto;

import java.time.YearMonth;

public record MyPageKeyScreenResponse(
        String month,
        ProfileSummary profile,
        KeyScreenInsightResponse readingStatus,
        FriendReadingRankingTop3Response friendRankingTop3,
        ReadingCalendarResponse readingCalendar
) {
    public static MyPageKeyScreenResponse of(
            YearMonth month,
            MeProfileWithStatsResponse profile,
            KeyScreenInsightResponse readingStatus,
            FriendReadingRankingTop3Response top3,
            ReadingCalendarResponse calendar
    ) {
        return new MyPageKeyScreenResponse(
                month.toString(),
                ProfileSummary.from(profile),
                readingStatus,
                top3,
                calendar
        );
    }

    public record ProfileSummary(
            String nickname,
            String avatarUrl,
            boolean isShelfPublic,
            boolean isBooklogPublic,

            String email,
            long followerCount,
            long followingCount,
            long completedBookCount,
            long myBooklogCount,
            long bookmarkCount
    ) {
        public static ProfileSummary from(MeProfileWithStatsResponse p) {
            return new ProfileSummary(
                    p.nickname(),
                    p.avatarUrl(),
                    p.isShelfPublic(),
                    p.isBooklogPublic(),

                    p.email(),
                    p.followerCount(),
                    p.followingCount(),
                    p.completedBookCount(),
                    p.myBooklogCount(),
                    p.bookmarkCount()
            );
        }
    }
}

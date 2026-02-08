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
            MeProfileResponse profile,
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
            boolean isBooklogPublic
            // + 만약 키스크린에서 counts 필요하면 여기 확장:
            // long followerCount, long followingCount, long completedCount, long booklogCount, long bookmarkCount
    ) {
        public static ProfileSummary from(MeProfileResponse p) {
            return new ProfileSummary(
                    p.nickname(),
                    p.profileImageUrl(),
                    p.isShelfPublic(),
                    p.isBooklogPublic()
            );
        }
    }
}

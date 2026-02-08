package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageKeyScreenFacadeService {

    private final MeProfileWithStatsService meProfileWithStatsService;
    private final KeyScreenInsightService keyScreenInsightService;
    private final FriendsReadingRankingService friendsReadingRankingService;
    private final ReadingCalendarService readingCalendarService;

    public MyPageKeyScreenResponse getMyPage(Long userId, String email, YearMonth month) {
        YearMonth target = (month != null) ? month : YearMonth.now();

        // 1) 유저 요약 + counts + email
        MeProfileWithStatsResponse profile =
                meProfileWithStatsService.getMyProfileWithStats(userId, email);

        // 2) 독서 현황 + AI 멘트
        KeyScreenInsightResponse insight =
                keyScreenInsightService.getKeyScreenInsight(userId, target);

        // 3) 친구 Top3
        FriendReadingRankingTop3Response top3 =
                friendsReadingRankingService.getTop3(userId, target.toString());

        // 4) 캘린더
        ReadingCalendarResponse calendar =
                readingCalendarService.getCalendar(userId, target.toString());

        return MyPageKeyScreenResponse.of(target, profile, insight, top3, calendar);
    }
}

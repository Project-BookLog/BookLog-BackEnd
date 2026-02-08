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

    private final MeProfileService meProfileService; // 유저 요약 쪽이 따로 있다면 그걸로
    private final KeyScreenInsightService keyScreenInsightService;
    private final FriendsReadingRankingService friendsReadingRankingService;
    private final ReadingCalendarService readingCalendarService;

    public MyPageKeyScreenResponse getMyPage(Long userId, YearMonth month) {
        YearMonth target = (month != null) ? month : YearMonth.now();

        // 1) 유저 요약
        // 현재 MeProfileService는 "프로필 편집용" 필드 중심이라면,
        // "키스크린 요약" 전용 쿼리/서비스로 분리하는 것도 추천 (nickname/avatar + counts)
        MeProfileResponse profile = meProfileService.getMyProfile(userId);

        // 2) 독서 현황 + AI 멘트
        KeyScreenInsightResponse insight = keyScreenInsightService.getKeyScreenInsight(userId, target);

        // 3) 친구 Top3 (너 코드는 month가 String이라서 맞춰줌)
        FriendReadingRankingTop3Response top3 =
                friendsReadingRankingService.getTop3(userId, target.toString());

        // 4) 캘린더
        ReadingCalendarResponse calendar =
                readingCalendarService.getCalendar(userId, target.toString());

        return MyPageKeyScreenResponse.of(target, profile, insight, top3, calendar);
    }
}

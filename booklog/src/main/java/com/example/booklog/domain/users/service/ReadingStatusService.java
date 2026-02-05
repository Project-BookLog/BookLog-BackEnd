package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.ReadingStatusResponse;
import com.example.booklog.domain.users.repository.ReadingStatusQueryRepository;
import com.example.booklog.domain.users.repository.projection.MonthlyStatusCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingStatusService {

    private final ReadingStatusQueryRepository readingStatusQueryRepository;

    /**
     * 마이페이지 - 독서 현황(월) 조회
     * - 퍼센트: completed / (completed + reading) * 100 (소수점 버림)
     * - 집계 대상: 해당 달에 reading_logs가 존재하는 내 user_books
     * - dayProgress.currentDay: 오늘 날짜(LocalDate.now())의 dayOfMonth
     * - topMoodTags: 해당 달 책들의 MOOD 태그 Top3
     */
    public ReadingStatusResponse getMonthlyReadingStatus(Long userId, YearMonth month) {

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.plusMonths(1).atDay(1);

        MonthlyStatusCountProjection p =
                readingStatusQueryRepository.findMonthlyStatusCounts(userId, monthStart, monthEnd);

        long completed = safeLong(p == null ? null : p.getCompletedCnt());
        long reading = safeLong(p == null ? null : p.getReadingCnt());

        int progressPercent = calcPercentFloor(completed, reading);

        List<String> topMoodTags =
                readingStatusQueryRepository.findTopMoodTags(userId, monthStart, monthEnd, 3);

        int currentDay = LocalDate.now().getDayOfMonth(); // ✅ 오늘 날짜 기준
        int lastDay = month.lengthOfMonth();

        return new ReadingStatusResponse(
                month.toString(), // "YYYY-MM"
                progressPercent,
                new ReadingStatusResponse.DayProgress(currentDay, lastDay),
                topMoodTags,
                null // aiSummary는 추후 GPT 결과로 채우기
        );
    }

    private int calcPercentFloor(long completed, long reading) {
        long denom = completed + reading;
        if (denom <= 0) return 0;
        return (int) ((completed * 100) / denom); // ✅ 정수 나눗셈 => 소수점 버림
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }
}

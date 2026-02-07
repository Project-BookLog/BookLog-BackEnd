package com.example.booklog.domain.users.service;

import com.example.booklog.domain.ai.service.GptUxCopyService;
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
    private final GptUxCopyService gptUxCopyService;

    public ReadingStatusResponse getMonthlyReadingStatus(Long userId, YearMonth month) {

        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);

        MonthlyStatusCountProjection p =
                readingStatusQueryRepository.findMonthlyStatusCounts(userId, start, end);

        long completed = safeLong(p == null ? null : p.getCompletedCnt());
        long reading = safeLong(p == null ? null : p.getReadingCnt());

        int progressPercent = calcPercentFloor(completed, reading);

        List<String> topMoodTags =
                readingStatusQueryRepository.findTopMoodTags(userId, start, end, 3);

        int lastDay = month.lengthOfMonth();
        int currentDay = resolveCurrentDay(month, lastDay);

        String aiSummary = gptUxCopyService.generateCalendarMonthlySummary(
                month, progressPercent, currentDay, lastDay, topMoodTags
        );

        return new ReadingStatusResponse(
                month.toString(),
                progressPercent,
                new ReadingStatusResponse.DayProgress(currentDay, lastDay),
                topMoodTags,
                aiSummary
        );
    }

    private int resolveCurrentDay(YearMonth month, int lastDay) {
        LocalDate today = LocalDate.now();
        return YearMonth.from(today).equals(month) ? today.getDayOfMonth() : lastDay;
    }

    private int calcPercentFloor(long completed, long reading) {
        long denom = completed + reading;
        if (denom <= 0) return 0;
        return (int) ((completed * 100) / denom);
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }
}

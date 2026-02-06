// service
package com.example.booklog.domain.users.service;

import com.example.booklog.domain.ai.service.GptKeyScreenInsightService;
import com.example.booklog.domain.users.dto.CalendarReadingStatusResponse;
import com.example.booklog.domain.users.repository.ReadingStatusQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarReadingStatusService {

    private final ReadingStatusQueryRepository readingStatusQueryRepository;
    private final GptKeyScreenInsightService gptKeyScreenInsightService;

    public CalendarReadingStatusResponse getCalendarReadingStatus(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);

        List<String> topMoodTags = readingStatusQueryRepository.findTopMoodTags(userId, start, end, 3);
        String tasteInsight = gptKeyScreenInsightService.generateTasteInsight(month, topMoodTags);

        return new CalendarReadingStatusResponse(month.toString(), tasteInsight);
    }
}

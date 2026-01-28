package com.example.booklog.domain.users.service;

import com.example.booklog.domain.library.shelves.repository.ReadingLogsRepository;
import com.example.booklog.domain.users.dto.ReadingCalendarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingCalendarService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReadingLogsRepository readingLogsRepository;

    public ReadingCalendarResponse getCalendar(Long userId, String month) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(KST)
                : parseYearMonth(month);

        LocalDate start = ym.atDay(1);
        LocalDate endExclusive = ym.plusMonths(1).atDay(1);

        List<ReadingLogsRepository.CalendarDayThumbnailRow> rows =
                readingLogsRepository.findCalendarDayThumbnails(userId, start, endExclusive);

        List<ReadingCalendarResponse.DayItem> days = rows.stream()
                .map(r -> new ReadingCalendarResponse.DayItem(r.getReadDate(), r.getThumbnailUrl()))
                .toList();

        return new ReadingCalendarResponse(ym.toString(), days);
    }

    private YearMonth parseYearMonth(String month) {
        try {
            return YearMonth.parse(month); // "YYYY-MM"
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month 형식이 올바르지 않습니다. 예) 2026-01");
        }
    }
}

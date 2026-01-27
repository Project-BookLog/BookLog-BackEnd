package com.example.booklog.domain.users.dto;

public record ReadingCalendarViewAllResponse(
        ReadingCalendarResponse calendar,
        Integer progressPercent
) {}
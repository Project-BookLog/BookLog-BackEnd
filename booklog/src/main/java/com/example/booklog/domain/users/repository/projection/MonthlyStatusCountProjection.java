package com.example.booklog.domain.users.repository.projection;

public interface MonthlyStatusCountProjection {
    Long getCompletedCnt();
    Long getReadingCnt();
}

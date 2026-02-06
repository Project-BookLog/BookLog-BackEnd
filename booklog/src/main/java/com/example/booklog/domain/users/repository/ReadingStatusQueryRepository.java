package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.repository.projection.MonthlyStatusCountProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReadingStatusQueryRepository extends Repository<Object, Long> {

    /**
     * 이번 달에 reading_logs(read_date)가 존재하는 user_books만 대상으로
     * COMPLETED / READING 카운트 집계
     *
     * progressPercent = completed / (completed + reading)
     */
    @Query(value = """
        SELECT
          SUM(CASE WHEN ub.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCnt,
          SUM(CASE WHEN ub.status = 'READING'   THEN 1 ELSE 0 END) AS readingCnt
        FROM user_books ub
        WHERE ub.user_id = :userId
          AND EXISTS (
              SELECT 1
              FROM reading_logs rl
              WHERE rl.user_book_id = ub.user_book_id
                AND rl.read_date >= :startDate
                AND rl.read_date <  :endDate
          )
        """, nativeQuery = true)
    MonthlyStatusCountProjection findMonthlyStatusCounts(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 이번 달에 "읽은 기록이 있는 책들"의 MOOD 태그 Top N
     * - status 상관없이 월간 기록(rl.read_date)으로 책을 선정
     * - 책 중복 제거: DISTINCT book_id
     */
    @Query(value = """
        WITH monthly_books AS (
            SELECT DISTINCT ub.book_id
            FROM user_books ub
            JOIN reading_logs rl
              ON rl.user_book_id = ub.user_book_id
            WHERE ub.user_id = :userId
              AND rl.read_date >= :startDate
              AND rl.read_date <  :endDate
        )
        SELECT t.name
        FROM monthly_books mb
        JOIN book_tags bt ON bt.book_id = mb.book_id
        JOIN tags t ON t.tag_id = bt.tag_id
        WHERE t.category = 'MOOD'
        GROUP BY t.name
        ORDER BY COUNT(*) DESC, t.name ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findTopMoodTags(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );
}

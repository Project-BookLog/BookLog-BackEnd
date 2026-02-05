package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.repository.projection.MonthlyStatusCountProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReadingStatusQueryRepository extends Repository<Object, Long> {

    @Query(value = """
            WITH month_books AS (
          SELECT DISTINCT ub.user_book_id
          FROM user_books ub
          JOIN reading_logs rl ON rl.user_book_id = ub.user_book_id
          WHERE ub.user_id = :userId
            AND rl.read_date >= :monthStart
            AND rl.read_date <  :monthEnd
        )
        SELECT
          SUM(CASE WHEN ub.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCnt,
          SUM(CASE WHEN ub.status = 'READING'   THEN 1 ELSE 0 END) AS readingCnt
        FROM user_books ub
        JOIN month_books mb ON mb.user_book_id = ub.user_book_id
        """, nativeQuery = true)
    MonthlyStatusCountProjection findMonthlyStatusCounts(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

    @Query(value = """
    WITH month_books AS (
      SELECT DISTINCT ub.user_book_id
      FROM user_books ub
      JOIN reading_logs rl ON rl.user_book_id = ub.user_book_id
      WHERE ub.user_id = :userId
        AND rl.read_date >= :monthStart
        AND rl.read_date <  :monthEnd
    )
    SELECT t.name
    FROM month_books mb
    JOIN user_books ub ON ub.user_book_id = mb.user_book_id
    JOIN book_tags bt ON bt.book_id = ub.book_id
    JOIN tags t ON t.tag_id = bt.tag_id
    WHERE t.category = 'MOOD'
    GROUP BY t.tag_id, t.name
    ORDER BY COUNT(*) DESC, t.name ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<String> findTopMoodTags(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("limit") int limit
    );

}

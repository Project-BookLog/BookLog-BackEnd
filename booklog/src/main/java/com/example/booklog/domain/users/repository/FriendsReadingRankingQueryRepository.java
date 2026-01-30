package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.dto.FriendReadingRankingRow;
import com.example.booklog.domain.users.entity.UserFollows;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FriendsReadingRankingQueryRepository
        extends JpaRepository<UserFollows, UserFollows.UserFollowId> {

    @Query(value = """
    WITH friends AS (
        SELECT uf1.followee_id AS friend_id
        FROM user_follows uf1
        JOIN user_follows uf2
          ON uf2.follower_id = uf1.followee_id
         AND uf2.followee_id = uf1.follower_id
        WHERE uf1.follower_id = :meId
    ),
    completed AS (
        SELECT ub.user_id, COUNT(*) AS completed_count
        FROM user_books ub
        WHERE ub.user_id IN (SELECT friend_id FROM friends)
          AND ub.status = 'COMPLETED'
          AND ub.end_date >= :startDate
          AND ub.end_date <  :endDate
        GROUP BY ub.user_id
    ),
    logs AS (
        SELECT ub.user_id,
               COUNT(DISTINCT rl.read_date) AS reading_days
        FROM reading_logs rl
        JOIN user_books ub ON ub.user_book_id = rl.user_book_id
        WHERE ub.user_id IN (SELECT friend_id FROM friends)
          AND rl.read_date >= :startDate
          AND rl.read_date <  :endDate
        GROUP BY ub.user_id
    ),
    ranked AS (
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY
                    COALESCE(c.completed_count, 0) DESC,
                    COALESCE(l.reading_days, 0) DESC,
                    u.nickname ASC,
                    u.user_id ASC
            ) AS `rank`,
            u.user_id           AS userId,
            u.nickname          AS nickname,
            u.profile_image_url AS profileImageUrl,
            COALESCE(c.completed_count, 0) AS completedCount,
            COALESCE(l.reading_days, 0)    AS readingDays
        FROM users u
        JOIN friends f ON f.friend_id = u.user_id
        LEFT JOIN completed c ON c.user_id = u.user_id
        LEFT JOIN logs l ON l.user_id = u.user_id
    )
    SELECT *
    FROM ranked
    WHERE `rank` > :afterRank
    ORDER BY `rank`
    LIMIT :limitPlusOne
    """, nativeQuery = true)
    List<FriendReadingRankingRow> findAfterRank(
            @Param("meId") Long meId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("afterRank") int afterRank,
            @Param("limitPlusOne") int limitPlusOne
    );

}

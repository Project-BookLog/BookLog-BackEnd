package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.projection.UserProfileSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileQueryRepository extends JpaRepository<Users, Long> {

    @Query(value = """
    SELECT
        u.user_id AS userId,
        u.nickname AS nickname,
        (
            SELECT aa.email
            FROM auth_accounts aa
            WHERE aa.user_id = u.user_id
            ORDER BY aa.id ASC
            LIMIT 1
        ) AS email,
        u.profile_image_url AS avatarUrl,

        (SELECT COUNT(*) FROM user_follows f WHERE f.followee_id = u.user_id) AS followerCount,
        (SELECT COUNT(*) FROM user_follows f WHERE f.follower_id = u.user_id) AS followingCount,

        (SELECT COUNT(*) FROM user_books ub WHERE ub.user_id = u.user_id) AS savedBookCount,
        (SELECT COUNT(*) FROM user_books ub WHERE ub.user_id = u.user_id AND ub.status = 'COMPLETED') AS completedBookCount,

        (SELECT COUNT(*) FROM booklog_posts bp WHERE bp.user_id = u.user_id) AS booklogCount,
        (SELECT COUNT(*) FROM booklog_bookmark bb WHERE bb.user_id = u.user_id) AS bookmarkCount,

        CASE
          WHEN EXISTS(
            SELECT 1
            FROM user_follows f2
            WHERE f2.follower_id = :meId
              AND f2.followee_id = u.user_id
          )
          THEN TRUE
          ELSE FALSE
        END AS isFollowing,

        CASE
          WHEN COALESCE(us.is_shelf_public, 0) = 1 THEN TRUE
          ELSE FALSE
        END AS isShelfPublic,

        CASE
          WHEN COALESCE(us.is_post_public, 0) = 1 THEN TRUE
          ELSE FALSE
        END AS isBooklogPublic

    FROM users u
    LEFT JOIN user_settings us ON us.user_id = u.user_id
    WHERE u.user_id = :targetUserId
    """, nativeQuery = true)
    Optional<UserProfileSummaryProjection> findUserProfileSummary(
            @Param("meId") Long meId,
            @Param("targetUserId") Long targetUserId
    );
}

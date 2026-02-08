package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.projection.MeProfileWithStatsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeProfileWithStatsQueryRepository extends Repository<Users, Long> {

    @Query(value = """
    SELECT
      u.user_id AS userId,
      u.nickname AS nickname,
      u.profile_image_url AS avatarUrl,

      /* settings 없으면 기본 true(=1) */
      IFNULL(us.is_shelf_public, 1) AS isShelfPublic,
      IFNULL(us.is_post_public,  1) AS isBooklogPublic,

      (SELECT COUNT(*)
         FROM user_follows f
        WHERE f.followee_id = u.user_id) AS followerCount,

      (SELECT COUNT(*)
         FROM user_follows f
        WHERE f.follower_id = u.user_id) AS followingCount,

      (SELECT COUNT(*)
         FROM user_books ub
        WHERE ub.user_id = u.user_id
          AND ub.status = :completedStatus) AS completedBookCount,

      (SELECT COUNT(*)
         FROM booklog_posts p
        WHERE p.user_id = u.user_id) AS myBooklogCount,

      (SELECT COUNT(*)
         FROM booklog_bookmark b
        WHERE b.user_id = u.user_id) AS bookmarkCount

    FROM users u
    LEFT JOIN user_settings us ON us.user_id = u.user_id
    WHERE u.user_id = :userId
    LIMIT 1
    """, nativeQuery = true)
    Optional<MeProfileWithStatsProjection> findMeProfileWithStats(
            @Param("userId") Long userId,
            @Param("completedStatus") String completedStatus
    );

}

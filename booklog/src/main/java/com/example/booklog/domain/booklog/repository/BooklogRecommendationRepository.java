package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.view.PopularPostItemView;
import com.example.booklog.domain.booklog.view.SimilarBookAggView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BooklogRecommendationRepository extends JpaRepository<BooklogPost, Long> {

    // 1) 태그 겹침 기반으로 "추천 책 후보(bookId)" 집계
    @Query(value = """
        SELECT
            p.book_id AS bookId,
            COUNT(DISTINCT pt.tag_id) AS overlapTags,
            SUM(p.view_count) AS totalViews,
            SUM(p.bookmark_count) AS totalBookmarks,
            MAX(p.created_at) AS latestPostAt
        FROM booklog_posts p
        JOIN booklog_post_tags pt ON pt.post_id = p.id
        WHERE p.status = 'PUBLISHED'
          AND p.book_id <> :excludeBookId
          AND pt.tag_id IN (:tagIds)
        GROUP BY p.book_id
        ORDER BY overlapTags DESC,
                 totalBookmarks DESC,
                 totalViews DESC,
                 latestPostAt DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<SimilarBookAggView> findSimilarBooksAgg(
            @Param("tagIds") List<Long> tagIds,
            @Param("excludeBookId") Long excludeBookId,
            @Param("limit") int limit
    );

    // 2) 태그 겹침 기반 "추천 인기글"
    @Query(value = """
        SELECT
            p.id AS postId,
            b.title AS bookTitle,
            SUBSTRING(p.content, 1, 80) AS excerpt,
            p.created_at AS createdAt,
            p.view_count AS viewCount,
            p.bookmark_count AS bookmarkCount,
            (
              SELECT i.image_url
              FROM booklog_post_images i
              WHERE i.post_id = p.id
              ORDER BY i.display_order ASC
              LIMIT 1
            ) AS thumbnailImageUrl
        FROM booklog_posts p
        JOIN booklog_post_tags pt ON pt.post_id = p.id
        JOIN books b ON b.book_id = p.book_id
        WHERE p.status = 'PUBLISHED'
          AND p.id <> :excludePostId
          AND pt.tag_id IN (:tagIds)
        GROUP BY p.id
        ORDER BY (p.bookmark_count * 3 + p.view_count) DESC,
                 p.created_at DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<PopularPostItemView> findPopularPostsByTags(
            @Param("tagIds") List<Long> tagIds,
            @Param("excludePostId") Long excludePostId,
            @Param("limit") int limit
    );
}
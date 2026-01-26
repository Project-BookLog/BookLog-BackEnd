package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BooklogPostRepository extends JpaRepository<BooklogPost, Long> {

    // 삭제된 글을 제외하고 단 건 조회
    Optional<BooklogPost> findByIdAndStatus(Long id, BooklogStatus status);


    // 피드(목록) : PUBLISHED 된 것만 최신순으로
    Slice<BooklogPost> findAllByStatusOrderByCreatedAtDesc(BooklogStatus status, Pageable pageable);


    // user 피드 목록
    Slice<BooklogPost> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long UserId, BooklogStatus status, Pageable pageable);

    // 특정 책에 대한 피드 목록
    Slice<BooklogPost> findAllByBookIdAndStatusOrderByCreatedAtDesc(Long bookId, BooklogStatus status, Pageable pageable);

    Slice<BooklogPost> findAllByIdInAndStatusOrderByCreatedAtDesc(Collection<Long> ids, BooklogStatus status, Pageable pageable);

    // 조회 수 +1 증가
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update BooklogPost p
           set p.viewCount = p.viewCount + 1
         where p.id = :postId
           and p.status = :published
    """)
    int increaseViewCount(@Param("postId") Long postId,
                          @Param("published") BooklogStatus published);


    // soft delete: status 변경 + updatedAt 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update BooklogPost p
           set p.status = :deleted,
               p.updatedAt = :now
         where p.id = :postId
           and p.status = :published
    """)
    int softDelete(@Param("postId") Long postId,
                   @Param("published") BooklogStatus published,
                   @Param("deleted") BooklogStatus deleted,
                   @Param("now") LocalDateTime now);


    @Query(
            value = """
    SELECT p.*
    FROM booklog_posts p
    WHERE p.status = 'PUBLISHED'
      AND (
        :moodEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'MOOD'
            AND pt.tag_id IN (:moodIds)
        )
      )
      AND (
        :styleEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'STYLE'
            AND pt.tag_id IN (:styleIds)
        )
      )
      AND (
        :immersionEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'IMMERSION'
            AND pt.tag_id IN (:immersionIds)
        )
      )
    ORDER BY p.created_at DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM booklog_posts p
    WHERE p.status = 'PUBLISHED'
      AND (
        :moodEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'MOOD'
            AND pt.tag_id IN (:moodIds)
        )
      )
      AND (
        :styleEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'STYLE'
            AND pt.tag_id IN (:styleIds)
        )
      )
      AND (
        :immersionEmpty = true OR EXISTS (
          SELECT 1
          FROM booklog_post_tags pt
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE pt.post_id = p.id
            AND t.category = 'IMMERSION'
            AND pt.tag_id IN (:immersionIds)
        )
      )
    """,
            nativeQuery = true
    )
    Page<BooklogPost> findPublishedFeedByTagFilters(
            @Param("moodEmpty") boolean moodEmpty,
            @Param("moodIds") List<Long> moodIds,
            @Param("styleEmpty") boolean styleEmpty,
            @Param("styleIds") List<Long> styleIds,
            @Param("immersionEmpty") boolean immersionEmpty,
            @Param("immersionIds") List<Long> immersionIds,
            Pageable pageable
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update BooklogPost p
       set p.bookmarkCount = p.bookmarkCount + 1,
           p.updatedAt = :now
     where p.id = :postId
       and p.status = :published
""")
    int increaseBookmarkCount(@Param("postId") Long postId,
                              @Param("published") BooklogStatus published,
                              @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update BooklogPost p
       set p.bookmarkCount = case when p.bookmarkCount > 0 then p.bookmarkCount - 1 else 0 end,
           p.updatedAt = :now
     where p.id = :postId
       and p.status = :published
""")
    int decreaseBookmarkCount(@Param("postId") Long postId,
                              @Param("published") BooklogStatus published,
                              @Param("now") LocalDateTime now);

    @Query("""
    select p.bookmarkCount
    from BooklogPost p
    where p.id = :postId
      and p.status = :published
""")
    Long findBookmarkCount(@Param("postId") Long postId,
                           @Param("published") BooklogStatus published);
}


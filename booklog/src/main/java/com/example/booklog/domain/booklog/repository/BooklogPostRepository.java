package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
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
}


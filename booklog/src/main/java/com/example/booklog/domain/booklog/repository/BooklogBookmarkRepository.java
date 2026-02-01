package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BooklogBookmarkRepository extends JpaRepository<BooklogBookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("""
        select b.postId, count(b)
        from BooklogBookmark b
        where b.postId in :postIds
        group by b.postId
    """)
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("""
        select b.postId
        from BooklogBookmark b
        where b.userId = :userId
          and b.postId in :postIds
    """)
    List<Long> findBookmarkedPostIdsByUserIdInPostIds(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds
    );

    @Query("""
        select b.postId
        from BooklogBookmark b
        where b.userId = :userId
        order by b.id desc
    """)
    List<Long> findMyBookmarkedPostIds(
            @Param("userId") Long userId,
            Pageable pageable
    );

    void deleteByUserIdAndPostId(Long userId, Long postId);

    Optional<BooklogBookmark> findByUserIdAndPostId(Long userId, Long postId);
}
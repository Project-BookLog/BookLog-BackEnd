package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BooklogBookmarkRepository extends JpaRepository<BooklogBookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("""
        select b.postId, count(b)
        from BooklogBookmark b
        where b.postId in :postIds
        group by b.postId
    """)
    List<Object[]> countByPostIds(List<Long> postIds);

    void deleteByUserIdAndPostId(Long userId, Long postId);
}

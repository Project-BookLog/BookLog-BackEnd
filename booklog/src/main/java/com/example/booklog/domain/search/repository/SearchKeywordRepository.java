package com.example.booklog.domain.search.repository;

import com.example.booklog.domain.search.entity.SearchKeyword;
import com.example.booklog.domain.users.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 검색어 레포지토리
 */
@Repository
public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    /**
     * 사용자별 최근 검색어 조회 (최신순)
     */
    @Query("SELECT sk FROM SearchKeyword sk " +
           "WHERE sk.user.id = :userId " +
           "ORDER BY sk.createdAt DESC")
    List<SearchKeyword> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자의 특정 검색어 조회 (중복 체크용)
     */
    @Query("SELECT sk FROM SearchKeyword sk " +
           "WHERE sk.user.id = :userId AND sk.keyword = :keyword")
    Optional<SearchKeyword> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 사용자의 검색어 개수 조회
     */
    @Query("SELECT COUNT(sk) FROM SearchKeyword sk WHERE sk.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 가장 오래된 검색어 삭제
     */
    @Modifying
    @Query("DELETE FROM SearchKeyword sk " +
           "WHERE sk.id IN (" +
           "  SELECT s.id FROM SearchKeyword s " +
           "  WHERE s.user.id = :userId " +
           "  ORDER BY s.createdAt ASC " +
           ")")
    void deleteOldestByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 특정 검색어 삭제
     */
    @Modifying
    @Query("DELETE FROM SearchKeyword sk WHERE sk.user.id = :userId AND sk.keyword = :keyword")
    void deleteByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}


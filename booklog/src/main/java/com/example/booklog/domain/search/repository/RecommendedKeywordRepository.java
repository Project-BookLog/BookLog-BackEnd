package com.example.booklog.domain.search.repository;

import com.example.booklog.domain.search.entity.RecommendedKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 추천 검색어 레포지토리
 */
@Repository
public interface RecommendedKeywordRepository extends JpaRepository<RecommendedKeyword, Long> {

    /**
     * 활성화된 추천 검색어 조회 (우선순위 오름차순)
     */
    @Query("SELECT rk FROM RecommendedKeyword rk " +
           "WHERE rk.isActive = true " +
           "ORDER BY rk.priority ASC, rk.id ASC")
    List<RecommendedKeyword> findAllActiveOrderByPriority();
}


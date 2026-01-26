package com.example.booklog.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 검색어 엔티티
 * 운영자가 관리하는 추천 검색어 목록
 *
 * [설계 고려사항]
 * 1. 로그인 여부와 무관하게 모든 사용자에게 노출
 * 2. 우선순위(priority)를 통해 노출 순서 제어
 * 3. 활성화 여부(isActive)로 관리 편의성 제공
 * 4. 인기 검색어 / 큐레이션 / 이벤트 검색어 등 다양한 타입 지원
 */
@Entity
@Table(
    name = "recommended_keywords",
    indexes = {
        @Index(name = "idx_recommended_keywords_active_priority", columnList = "is_active, priority ASC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "keyword", length = 100, nullable = false)
    private String keyword;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private RecommendationType type = RecommendationType.CURATED;

    @Column(name = "description", length = 200)
    private String description;

    @Builder
    public RecommendedKeyword(String keyword, Integer priority, Boolean isActive,
                              RecommendationType type, String description) {
        validateKeyword(keyword);
        this.keyword = keyword.trim();
        this.priority = priority != null ? priority : 0;
        this.isActive = isActive != null ? isActive : true;
        this.type = type != null ? type : RecommendationType.CURATED;
        this.description = description;
    }

    /**
     * 검색어 유효성 검증
     */
    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        if (keyword.trim().length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이내로 입력해주세요.");
        }
    }

    /**
     * 추천 검색어 활성화/비활성화
     */
    public void updateActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 우선순위 변경
     */
    public void updatePriority(int priority) {
        this.priority = priority;
    }

    /**
     * 추천 검색어 타입
     */
    public enum RecommendationType {
        CURATED,    // 큐레이션 (운영자 추천)
        POPULAR,    // 인기 검색어
        EVENT       // 이벤트/프로모션
    }
}


package com.example.booklog.domain.search.entity;

import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색어 엔티티
 * 사용자의 최근 검색어를 저장
 *
 * [설계 고려사항]
 * 1. 동일한 검색어 재검색 시 기존 레코드 삭제 후 재생성 (중복 제거 + 최신순 유지)
 * 2. 사용자별 최대 10개 제한 (애플리케이션 레벨에서 제어)
 * 3. BaseEntity 상속으로 createdAt 활용 (최신순 정렬)
 */
@Entity
@Table(
    name = "search_keywords",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_search_keywords_user_keyword",
            columnNames = {"user_id", "keyword"}
        )
    },
    indexes = {
        @Index(name = "idx_search_keywords_user_created", columnList = "user_id, created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_search_keyword_user"))
    private Users user;

    @Column(name = "keyword", length = 100, nullable = false)
    private String keyword;

    @Builder
    public SearchKeyword(Users user, String keyword) {
        validateKeyword(keyword);
        this.user = user;
        this.keyword = keyword.trim();
    }

    /**
     * 검색어 유효성 검증
     */
    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus.SEARCH_KEYWORD_REQUIRED);
        }

        if (keyword.trim().length() > 100) {
            throw new GeneralException(ErrorStatus.SEARCH_KEYWORD_TOO_LONG);
        }
    }
}


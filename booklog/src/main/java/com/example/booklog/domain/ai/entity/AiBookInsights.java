package com.example.booklog.domain.ai.entity;

import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_book_insights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiBookInsights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_book_insights_user")
    )
    private Users user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_book_id", //AI 인사이트 생성의 기준이 되는 "내 책(UserBook)
            nullable = false, // 서재 기반 AI 추천/분석이라는 요구사항의 핵심 기준
            foreignKey = @ForeignKey(name = "fk_ai_book_insights_user_book")
    )
    private UserBooks userBook;

    @Column(name = "insight_type", length = 20, nullable = false) //AI 인사이트 유형
    private String insightType;


    @Column(name = "result_text", columnDefinition = "TEXT", nullable = false) //사용자에게 실제로 노출되는 AI 결과 텍스트
    private String resultText;

    /**
     * AI 결과 생성 시 함께 산출된 메타데이터 (JSON)
     * - 태그, 키워드, 점수, 신뢰도 등 구조화 데이터
     * - 추천 고도화 및 필터링에 활용
     * - ERD의 "결과에 대한 태그/키워드"
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * AI 인사이트 생성 시각
     * - 결과 정렬 및 최신 인사이트 조회 기준
     * - 로그성 데이터이므로 수정되지 않음
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiBookInsights(
            Users user,
            UserBooks userBook,
            String insightType,
            String resultText,
            String metadata
    ) {
        this.user = user;
        this.userBook = userBook;
        this.insightType = insightType;
        this.resultText = resultText;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }
}

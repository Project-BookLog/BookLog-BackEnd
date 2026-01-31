package com.example.booklog.domain.onboarding.entity;

import com.example.booklog.domain.onboarding.entity.enums.*;
import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저 독서 취향 프로필 엔터티
 * - 온보딩 응답 데이터를 유저별로 저장
 * - 모든 필드는 nullable (사용자가 스킵 가능)
 * - PATCH 방식으로 부분 업데이트 지원
 * - GPT 기반 도서 추천 분석에 활용
 */
@Entity
@Table(name = "user_reading_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserReadingProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_reading_profile_user"))
    private Users user;

    // 1단계: 독서 방식
    @Enumerated(EnumType.STRING)
    @Column(name = "reader_type", length = 30)
    private ReaderType readerType;

    // 2단계: 선호 분위기 (최대 2개)
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_mood_1", length = 30)
    private PreferredMood preferredMood1;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_mood_2", length = 30)
    private PreferredMood preferredMood2;

    // 3단계: 문장 스타일
    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_breath", length = 30)
    private SentenceBreath sentenceBreath;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_texture", length = 30)
    private ExpressionTexture expressionTexture;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_direction", length = 30)
    private ExpressionDirection expressionDirection;

    // 4단계: 독서 순간
    @Enumerated(EnumType.STRING)
    @Column(name = "reading_moment", length = 30)
    private ReadingMoment readingMoment;

    // 메타 정보
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserReadingProfile(Users user) {
        this.user = user;
        // @MapsId를 사용하므로 userId는 JPA가 자동으로 설정
        // 생성자에서 직접 설정하면 영속성 컨텍스트 오류 발생
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 부분 업데이트 메서드
     * null이 아닌 필드만 업데이트
     */
    public void updateProfile(
            ReaderType readerType,
            PreferredMood preferredMood1,
            PreferredMood preferredMood2,
            SentenceBreath sentenceBreath,
            ExpressionTexture expressionTexture,
            ExpressionDirection expressionDirection,
            ReadingMoment readingMoment
    ) {
        if (readerType != null) {
            this.readerType = readerType;
        }
        if (preferredMood1 != null) {
            this.preferredMood1 = preferredMood1;
        }
        if (preferredMood2 != null) {
            this.preferredMood2 = preferredMood2;
        }
        if (sentenceBreath != null) {
            this.sentenceBreath = sentenceBreath;
        }
        if (expressionTexture != null) {
            this.expressionTexture = expressionTexture;
        }
        if (expressionDirection != null) {
            this.expressionDirection = expressionDirection;
        }
        if (readingMoment != null) {
            this.readingMoment = readingMoment;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 명시적 null 업데이트 메서드 (필드 초기화용)
     */
    public void clearProfile() {
        this.readerType = null;
        this.preferredMood1 = null;
        this.preferredMood2 = null;
        this.sentenceBreath = null;
        this.expressionTexture = null;
        this.expressionDirection = null;
        this.readingMoment = null;
        this.updatedAt = LocalDateTime.now();
    }
}


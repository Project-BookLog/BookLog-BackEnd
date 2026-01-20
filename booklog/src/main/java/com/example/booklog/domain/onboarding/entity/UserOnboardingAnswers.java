package com.example.booklog.domain.onboarding.entity;

import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_onboarding_answers")
@IdClass(UserOnboardingAnswers.UserOnboardingAnswerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingAnswers {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "question_id") //어떤 질문에 대한 답(검증, 조회 편의)
    private Long questionId;

    @Id
    @Column(name = "option_id") //어떤 선택지를 골랐는지 (실제 답)
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_user_onboarding_answers_user"))
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_user_onboarding_answers_question"))
    private OnboardingQuestions question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_user_onboarding_answers_option"))
    private OnboardingOptions option;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @Builder
    public UserOnboardingAnswers(Users user, OnboardingQuestions question, OnboardingOptions option) {
        this.userId = user.getId();
        this.questionId = question.getId();
        this.optionId = option.getId();
        this.user = user;
        this.question = question;
        this.option = option;
        this.answeredAt = LocalDateTime.now();
    }

    // 복합키 클래스
    public static class UserOnboardingAnswerId implements Serializable {
        private Long userId;
        private Long questionId;
        private Long optionId;

        public UserOnboardingAnswerId() {}

        public UserOnboardingAnswerId(Long userId, Long questionId, Long optionId) {
            this.userId = userId;
            this.questionId = questionId;
            this.optionId = optionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserOnboardingAnswerId that = (UserOnboardingAnswerId) o;
            return Objects.equals(userId, that.userId) &&
                   Objects.equals(questionId, that.questionId) &&
                   Objects.equals(optionId, that.optionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, questionId, optionId);
        }
    }
}


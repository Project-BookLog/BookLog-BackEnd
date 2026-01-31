package com.example.booklog.domain.onboarding.dto;

import com.example.booklog.domain.onboarding.entity.*;
import com.example.booklog.domain.onboarding.entity.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 온보딩 프로필 응답 DTO
 */
@Schema(description = "온보딩 프로필 응답")
@Getter
@Builder
@AllArgsConstructor
public class OnboardingProfileResponse {

    @Schema(description = "독서 방식", example = "BEGINNER_READER")
    private ReaderType readerType;

    @Schema(description = "선호 분위기 1", example = "CALM")
    private PreferredMood preferredMood1;

    @Schema(description = "선호 분위기 2", example = "WARM")
    private PreferredMood preferredMood2;

    @Schema(description = "문장 호흡", example = "CONCISE")
    private SentenceBreath sentenceBreath;

    @Schema(description = "표현 질감", example = "PLAIN")
    private ExpressionTexture expressionTexture;

    @Schema(description = "표현 방향", example = "DIRECT")
    private ExpressionDirection expressionDirection;

    @Schema(description = "독서 순간", example = "ROUTINE_TRANSITION")
    private ReadingMoment readingMoment;

    @Schema(description = "프로필 최종 업데이트 시각", example = "2026-01-31T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "온보딩 완료 여부", example = "true")
    private Boolean isCompleted;

    @Schema(description = "온보딩 완료 시각", example = "2026-01-31T10:35:00")
    private LocalDateTime completedAt;

    public static OnboardingProfileResponse from(UserReadingProfile profile, UserOnboardingStatus status) {
        return OnboardingProfileResponse.builder()
                .readerType(profile.getReaderType())
                .preferredMood1(profile.getPreferredMood1())
                .preferredMood2(profile.getPreferredMood2())
                .sentenceBreath(profile.getSentenceBreath())
                .expressionTexture(profile.getExpressionTexture())
                .expressionDirection(profile.getExpressionDirection())
                .readingMoment(profile.getReadingMoment())
                .updatedAt(profile.getUpdatedAt())
                .isCompleted(status != null ? status.getIsCompleted() : false)
                .completedAt(status != null ? status.getCompletedAt() : null)
                .build();
    }

    /**
     * 빈 온보딩 프로필 응답 생성
     * - 온보딩을 하지 않은 사용자를 위한 기본 응답
     */
    public static OnboardingProfileResponse empty() {
        return OnboardingProfileResponse.builder()
                .readerType(null)
                .preferredMood1(null)
                .preferredMood2(null)
                .sentenceBreath(null)
                .expressionTexture(null)
                .expressionDirection(null)
                .readingMoment(null)
                .updatedAt(null)
                .isCompleted(false)
                .completedAt(null)
                .build();
    }
}


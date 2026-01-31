package com.example.booklog.domain.onboarding.dto;

import com.example.booklog.domain.onboarding.entity.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 온보딩 응답 저장 요청 DTO
 * - 모든 필드는 optional (부분 업데이트 지원)
 * - null이 전달되지 않은 필드는 업데이트하지 않음
 * - 명시적으로 null을 전달하면 해당 필드를 null로 업데이트 (향후 확장 가능)
 */
@Schema(description = "온보딩 응답 저장 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOnboardingAnswersRequest {

    @Schema(description = "독서 방식 (BEGINNER_READER: 독서 입문자, PROFESSIONAL_READER: 프로 다독러)", example = "BEGINNER_READER", nullable = true)
    private ReaderType readerType;

    @Schema(description = "선호 분위기 1 (WARM: 따뜻한, CALM: 잔잔한, COOL: 서늘한, DREAMY: 몽환적인, CHEERFUL: 유쾌한, DARK: 어두운)", example = "CALM", nullable = true)
    private PreferredMood preferredMood1;

    @Schema(description = "선호 분위기 2 (최대 2개까지 선택 가능)", example = "WARM", nullable = true)
    private PreferredMood preferredMood2;

    @Schema(description = "문장 호흡 (CONCISE: 간결한, ELABORATE: 화려한)", example = "CONCISE", nullable = true)
    private SentenceBreath sentenceBreath;

    @Schema(description = "표현 질감 (PLAIN: 담백한, DELICATE: 섬세한)", example = "PLAIN", nullable = true)
    private ExpressionTexture expressionTexture;

    @Schema(description = "표현 방향 (DIRECT: 직설적, METAPHORICAL: 은유적)", example = "DIRECT", nullable = true)
    private ExpressionDirection expressionDirection;

    @Schema(description = "독서 순간 (ROUTINE_TRANSITION: 일상 전환, INTELLECTUAL_EXPLORATION: 지적인 탐구, IMMERSIVE_FLOW: 압도적 몰입, LINGERING_AFTERTASTE: 깊은 여운)", example = "ROUTINE_TRANSITION", nullable = true)
    private ReadingMoment readingMoment;
}


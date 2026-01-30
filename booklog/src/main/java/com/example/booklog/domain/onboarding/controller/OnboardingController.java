package com.example.booklog.domain.onboarding.controller;

import com.example.booklog.domain.onboarding.dto.OnboardingProfileResponse;
import com.example.booklog.domain.onboarding.dto.UpdateOnboardingAnswersRequest;
import com.example.booklog.domain.onboarding.service.OnboardingService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 온보딩 API 컨트롤러
 * - JWT 인증 기반으로 사용자 식별
 * - userId는 Security Context에서 추출
 */
@Tag(name = "Onboarding", description = "온보딩 (독서 취향 프로필) API")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * 온보딩 응답 저장 (부분 업데이트)
     * PATCH /api/v1/onboarding/answers
     *
     * @param userDetails JWT 인증된 사용자 정보
     * @param request 온보딩 응답 데이터 (모든 필드 optional)
     * @return 업데이트된 온보딩 프로필
     */
    @Operation(
            summary = "온보딩 응답 저장",
            description = "사용자의 독서 취향 프로필을 부분 업데이트합니다. 모든 필드는 optional이며, 전달되지 않은 필드는 기존 값을 유지합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "온보딩 응답 저장 성공",
                    content = @Content(schema = @Schema(implementation = OnboardingProfileResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/answers")
    public ResponseEntity<OnboardingProfileResponse> updateOnboardingAnswers(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateOnboardingAnswersRequest request
    ) {
        Long userId = userDetails.getUserId();
        OnboardingProfileResponse response = onboardingService.updateOnboardingAnswers(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 온보딩 완료 처리
     * PATCH /api/v1/onboarding/complete
     *
     * @param userDetails JWT 인증된 사용자 정보
     * @return 완료된 온보딩 프로필
     */
    @Operation(
            summary = "온보딩 완료 처리",
            description = "온보딩이 완료되었음을 명시적으로 저장합니다. 이미 완료된 경우에도 멱등성을 유지합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "온보딩 완료 처리 성공",
                    content = @Content(schema = @Schema(implementation = OnboardingProfileResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/complete")
    public ResponseEntity<OnboardingProfileResponse> completeOnboarding(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        OnboardingProfileResponse response = onboardingService.completeOnboarding(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 온보딩 프로필 조회
     * GET /api/v1/onboarding/profile
     *
     * @param userDetails JWT 인증된 사용자 정보
     * @return 온보딩 프로필
     */
    @Operation(
            summary = "온보딩 프로필 조회",
            description = "현재 사용자의 독서 취향 프로필을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "온보딩 프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = OnboardingProfileResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/profile")
    public ResponseEntity<OnboardingProfileResponse> getOnboardingProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        OnboardingProfileResponse response = onboardingService.getOnboardingProfile(userId);
        return ResponseEntity.ok(response);
    }
}


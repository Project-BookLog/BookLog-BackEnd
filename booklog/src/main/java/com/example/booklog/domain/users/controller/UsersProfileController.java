package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.UserProfileResponse;
import com.example.booklog.domain.users.service.UserProfileService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import com.example.booklog.global.common.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "유저 프로필",
        description = "다른 유저 프로필(요약/카운트/팔로잉 여부) 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UsersProfileController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "다른 유저 프로필 조회",
            description = """
                    다른 유저의 프로필 정보를 조회합니다.
                    - 인증: Access Token(Bearer)
                    - PathVariable: userId (조회 대상 유저)
                    - 응답:
                      - 유저 기본 정보(닉네임/이메일/프로필 이미지 등)
                      - 카운트(팔로워/팔로잉/저장한 책/완독/북로그/북마크)
                      - 팔로잉 여부(isFollowing): 로그인한 사용자(me)가 대상 유저를 팔로우 중인지 여부
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대상 유저를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패(토큰 없음/만료/유효하지 않음)"
            )
    })
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "조회 대상 유저 ID", example = "42", required = true)
            @PathVariable Long userId
    ) {
        Long meId = principal.getUserId();               // 로그인한 사용자 ID
        UserProfileResponse data = userProfileService.getProfile(meId, userId); // 조회 대상 userId

        return ApiResponse.onSuccess(SuccessStatus.OK, data);
    }
}

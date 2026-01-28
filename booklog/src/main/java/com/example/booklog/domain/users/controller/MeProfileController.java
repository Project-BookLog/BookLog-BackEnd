// =====================================
// [마이페이지] MeProfileController
// =====================================
package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MeProfileResponse;
import com.example.booklog.domain.users.dto.MeProfileUpdateRequest;
import com.example.booklog.domain.users.service.MeProfileService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name="마이페이지(Me) - Profile", description="내 프로필 조회/수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeProfileController {

    private final MeProfileService meProfileService;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    프로필 편집 화면 진입 시 내 프로필 정보를 조회합니다.
                    - 인증: Access Token(Bearer)
                    - 응답: 닉네임/공개토글/프로필 이미지 등
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode="200", description="성공"),
            @ApiResponse(responseCode="401", description="인증 실패")
    })
    @GetMapping("/profile")
    public MeProfileResponse getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return meProfileService.getMyProfile(userDetails.getUserId());
    }

    @Operation(
            summary = "내 프로필 수정",
            description = """
                    내 프로필 정보를 수정합니다.
                    - 인증: Access Token(Bearer)
                    - Body: nickname/isShelfPublic/isBooklogPublic 중 변경할 값만 전달
                    - 응답: 수정 반영된 프로필 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode="200", description="성공"),
            @ApiResponse(responseCode="400", description="요청값 오류(닉네임 정책 등)"),
            @ApiResponse(responseCode="401", description="인증 실패")
    })
    @PatchMapping("/profile")
    public MeProfileResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MeProfileUpdateRequest req
    ) {
        return meProfileService.updateProfile(userDetails.getUserId(), req);
    }
}

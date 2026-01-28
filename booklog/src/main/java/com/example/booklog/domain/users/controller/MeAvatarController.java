// =====================================
// [마이페이지] MeAvatarController
// =====================================
package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MeAvatarUpdateResponse;
import com.example.booklog.domain.users.service.MeAvatarService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name="마이페이지(Me) - Avatar", description="프로필 이미지 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
@Profile("!local") // 기존 정책 유지 (local에서는 Swagger에도 안 뜸)
public class MeAvatarController {

    private final MeAvatarService meAvatarService;

    @Operation(
            summary = "프로필 사진 업로드",
            description = """
                    내 프로필 사진을 업로드하고 URL을 갱신합니다.
                    - 인증: Access Token(Bearer)
                    - Content-Type: multipart/form-data
                    - Part: file(필수)
                    - 응답: 업로드된 이미지 URL 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode="200", description="성공"),
            @ApiResponse(responseCode="400", description="파일 누락/형식 오류"),
            @ApiResponse(responseCode="401", description="인증 실패")
    })
    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeAvatarUpdateResponse updateAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        return meAvatarService.updateAvatar(userDetails.getUserId(), file);
    }
}

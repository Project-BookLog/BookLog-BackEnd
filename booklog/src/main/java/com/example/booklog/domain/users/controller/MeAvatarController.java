package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MeAvatarUpdateResponse;
import com.example.booklog.domain.users.service.MeAvatarService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
@Profile("!local") // ✅ local 프로필에서는 빈 등록 X → Swagger에도 안 뜸
public class MeAvatarController {

    private final MeAvatarService meAvatarService;

    /**
     * [로컬 테스트 불가]
     * local 프로필에서는 AmazonS3Manager(@Profile("!local"))가 비활성화되어 업로드가 동작하지 않음.
     * 실제 dev 인스턴스에서 테스트
     */
    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeAvatarUpdateResponse updateAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        return meAvatarService.updateAvatar(userDetails.getUserId(), file);
    }
}

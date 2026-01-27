package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MeProfileResponse;
import com.example.booklog.domain.users.dto.MeProfileUpdateRequest;
import com.example.booklog.domain.users.service.MeProfileService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeProfileController {

    private final MeProfileService meProfileService;

    // 프로필 편집 화면 진입 시 데이터 로드용 (추가 추천)
    @GetMapping("/profile")
    public MeProfileResponse getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return meProfileService.getMyProfile(userDetails.getUserId());
    }

    // 기존 프로필 편집(PATCH)
    @PatchMapping("/profile")
    public MeProfileResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MeProfileUpdateRequest req
    ) {
        return meProfileService.updateProfile(userDetails.getUserId(), req);
    }
}

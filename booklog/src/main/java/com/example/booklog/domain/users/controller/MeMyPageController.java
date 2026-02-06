package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MyPageResponse;
import com.example.booklog.domain.users.service.MyPageService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import com.example.booklog.global.common.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Tag(name = "마이페이지 - 키스크린", description = "마이페이지 대시보드(키스크린) 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeMyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "키스크린 조회", description = "month=YYYY-MM")
    @GetMapping("/mypage")
    public ApiResponse<MyPageResponse> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        MyPageResponse res = myPageService.getMyPage(userDetails.getUserId(), month);
        return ApiResponse.onSuccess(SuccessStatus.OK, res);
    }
}

package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.MyPageKeyScreenResponse;
import com.example.booklog.domain.users.service.MyPageKeyScreenFacadeService;
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

@Tag(name = "마이페이지 - 키스크린", description = "마이페이지 대시보드(키스크린) 통합 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MyPageKeyScreenController {

    private final MyPageKeyScreenFacadeService facadeService;

    @Operation(summary = "마이페이지 키스크린(대시보드) 통합 조회", description = "month=YYYY-MM (없으면 이번 달)")
    @GetMapping("/mypage")
    public ApiResponse<MyPageKeyScreenResponse> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        Long userId = userDetails.getUserId();
        String email = userDetails.getUsername();

        return ApiResponse.onSuccess(SuccessStatus.OK,
                facadeService.getMyPage(userId, email, month)
        );
    }
}

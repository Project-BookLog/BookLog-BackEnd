package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.KeyScreenInsightResponse;
import com.example.booklog.domain.users.service.KeyScreenInsightFacadeService;
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

@Tag(name = "마이페이지 - 키스크린", description = "키스크린 독서 취향 분석 문구")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me/key-screen")
public class MeKeyScreenController {

    private final KeyScreenInsightFacadeService keyScreenInsightFacadeService;

    @Operation(summary = "키스크린 독서 취향 분석 문구", description = "month=YYYY-MM")
    @GetMapping("/insight")
    public ApiResponse<KeyScreenInsightResponse> getKeyScreenInsight(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        KeyScreenInsightResponse res =
                keyScreenInsightFacadeService.getKeyScreenInsight(userDetails.getUserId(), month);

        return ApiResponse.onSuccess(SuccessStatus.OK, res);
    }
}

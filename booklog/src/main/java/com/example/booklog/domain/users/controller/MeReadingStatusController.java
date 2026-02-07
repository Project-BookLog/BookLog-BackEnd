package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.ReadingStatusResponse;
import com.example.booklog.domain.users.service.ReadingStatusService;
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

@Tag(name = "마이페이지 - 독서 현황", description = "월간 독서 현황 조회(캘린더/현황 카드용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeReadingStatusController {

    private final ReadingStatusService readingStatusService;

    @Operation(summary = "독서 현황(월) 조회", description = "month=YYYY-MM")
    @GetMapping("/reading-status")
    public ApiResponse<ReadingStatusResponse> getReadingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        ReadingStatusResponse res =
                readingStatusService.getMonthlyReadingStatus(userDetails.getUserId(), month);

        return ApiResponse.onSuccess(SuccessStatus.OK, res);
    }
}

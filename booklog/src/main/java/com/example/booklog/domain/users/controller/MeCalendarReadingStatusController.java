// controller
package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.CalendarReadingStatusResponse;
import com.example.booklog.domain.users.service.CalendarReadingStatusService;
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

@Tag(name = "마이페이지 - 캘린더(독서현황 AI 멘트)", description = "캘린더 페이지 상단 AI 멘트(월별) 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me/reading-calendar")
public class MeCalendarReadingStatusController {

    private final CalendarReadingStatusService calendarReadingStatusService;

    @Operation(summary = "캘린더 AI 멘트 조회", description = "month=YYYY-MM")
    @GetMapping("/insight")
    public ApiResponse<CalendarReadingStatusResponse> getCalendarReadingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        CalendarReadingStatusResponse res =
                calendarReadingStatusService.getCalendarReadingStatus(userDetails.getUserId(), month);

        return ApiResponse.onSuccess(SuccessStatus.OK, res);
    }
}

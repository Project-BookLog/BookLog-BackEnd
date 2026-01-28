// =====================================
// [마이페이지] MeReadingCalendarController
// =====================================
package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.ReadingCalendarResponse;
import com.example.booklog.domain.users.service.ReadingCalendarService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지(Me) - Reading Calendar", description = "마이페이지 독서 캘린더 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeReadingCalendarController {

    private final ReadingCalendarService readingCalendarService;

    @Operation(
            summary = "독서 캘린더 조회",
            description = """
                    month=YYYY-MM (없으면 현재달).
                    날짜별 최신 독서기록의 책 썸네일 1개 반환
                    - 인증: Access Token(Bearer)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode="200", description="성공"),
            @ApiResponse(responseCode="400", description="month 형식 오류(YYYY-MM)"),
            @ApiResponse(responseCode="401", description="인증 실패")
    })
    @GetMapping("/reading-calendar")
    public ReadingCalendarResponse getReadingCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String month
    ) {
        return readingCalendarService.getCalendar(userDetails.getUserId(), month);
    }
}

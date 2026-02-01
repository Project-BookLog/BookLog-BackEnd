package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.users.service.UserBooklogReadService;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지(Me) - Booklogs", description = "내 북로그/북마크 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeBooklogController {

    private final UserBooklogReadService userBooklogReadService;

    @Operation(summary = "내 북로그 조회")
    @GetMapping("/booklogs")
    public ApiResponse<BooklogFeedResponse> myBooklogs(
            @AuthenticationPrincipal CustomUserDetails me,
            @ParameterObject Pageable pageable
    ) {
        BooklogFeedResponse res = userBooklogReadService.getMyBooklogs(me.getUserId(), pageable);
        return ApiResponse.onSuccess(AuthSuccessCode.READ_SUCCESS, res);
    }

    @Operation(summary = "내 북마크한 북로그 조회")
    @GetMapping("/booklogs/bookmarks")
    public ApiResponse<BooklogFeedResponse> myBookmarkedBooklogs(
            @AuthenticationPrincipal CustomUserDetails me,
            @ParameterObject Pageable pageable
    ) {
        BooklogFeedResponse res = userBooklogReadService.getMyBookmarkedBooklogs(me.getUserId(), pageable);
        return ApiResponse.onSuccess(AuthSuccessCode.READ_SUCCESS, res);
    }
}
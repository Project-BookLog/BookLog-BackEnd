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

@Tag(name = "유저(User) - Booklogs", description = "다른 유저 북로그 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserBooklogController {

    private final UserBooklogReadService userBooklogReadService;

    @Operation(summary = "다른 유저 공개 북로그 조회")
    @GetMapping("/{userId}/booklogs")
    public ApiResponse<BooklogFeedResponse> getUserBooklogs(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long userId,
            @ParameterObject Pageable pageable
    ) {
        BooklogFeedResponse res = userBooklogReadService.getUserPublicBooklogs(
                me.getUserId(),
                userId,
                pageable
        );
        return ApiResponse.onSuccess(AuthSuccessCode.READ_SUCCESS, res);
    }
}
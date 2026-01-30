package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.service.UserFollowService;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    /** 팔로우 */
    @PostMapping("/api/v1/users/{userId}/follow")
    public ApiResponse<FollowActionResponse> follow(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.FOLLOW_SUCCESS,
                userFollowService.follow(me.getUserId(), userId)
        );
    }

    /** 언팔로우 */
    @DeleteMapping("/api/v1/users/{userId}/follow")
    public ApiResponse<FollowActionResponse> unfollow(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.UNFOLLOW_SUCCESS,
                userFollowService.unfollow(me.getUserId(), userId)
        );
    }

    /**
     * 맞팔 친구 목록(무한 스크롤)
     *
     * cursor 기반 무한 스크롤:
     * - 첫 호출: cursor 없이 요청
     * - 응답의 nextCursor를 다음 요청의 cursor로 전달
     *
     * GET /api/v1/me/friends/mutual?size=20
     * GET /api/v1/me/friends/mutual?size=20&cursor=95
     */
    @GetMapping("/api/v1/me/friends/mutual")
    public ApiResponse<MutualFriendsResponse> mutualFriends(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                userFollowService.getMutualFriends(me.getUserId(), cursor, size)
        );
    }
}

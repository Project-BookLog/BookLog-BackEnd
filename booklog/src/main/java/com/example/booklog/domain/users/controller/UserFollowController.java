package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.service.UserFollowService;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팔로우/친구", description = "유저 팔로우/언팔로우 및 맞팔(친구) 목록 조회 API")
@RestController
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    @Operation(
            summary = "유저 팔로우",
            description = """
                로그인한 사용자가 특정 유저를 팔로우합니다.

                - 팔로우 여부는 users 테이블의 컬럼이 아니라,
                  user_follows 테이블의 (follower_id, followee_id) 행 존재로 판단합니다.
                - 멱등 처리: 이미 팔로우 중이라면 새로 생성하지 않고 그대로 성공 응답합니다.
                - isMutual: 상대가 나를 팔로우 중이면 true(맞팔/친구)

                ✅ 프론트에서 필요한 값
                - PathVariable userId: 팔로우 대상 유저 ID
                - Authorization: Bearer AccessToken (필수)
                """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팔로우 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FollowActionResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "자기 자신 팔로우 불가"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대상 유저 없음"
            )
    })
    @PostMapping("/api/v1/users/{userId}/follow")
    public ApiResponse<FollowActionResponse> follow(
            @Parameter(hidden = true) // Authorization에서 파생되는 값이라 Swagger에 노출 숨김
            @AuthenticationPrincipal CustomUserDetails me,

            @Parameter(description = "팔로우 대상 유저 ID", example = "12", required = true)
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.FOLLOW_SUCCESS,
                userFollowService.follow(me.getUserId(), userId)
        );
    }

    @Operation(
            summary = "유저 언팔로우",
            description = """
                로그인한 사용자가 특정 유저를 언팔로우합니다.

                - user_follows 테이블에서 (follower_id=나, followee_id=상대) 행을 삭제합니다.
                - 멱등 처리: 이미 언팔로우 상태여도 성공 응답으로 처리할 수 있습니다(정책에 따라).
                - 언팔로우 후 isMutual은 항상 false 입니다.

                ✅ 프론트에서 필요한 값
                - PathVariable userId: 언팔로우 대상 유저 ID
                - Authorization: Bearer AccessToken (필수)
                """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "언팔로우 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FollowActionResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "자기 자신 언팔로우 불가"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대상 유저 없음"
            )
    })
    @DeleteMapping("/api/v1/users/{userId}/follow")
    public ApiResponse<FollowActionResponse> unfollow(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails me,

            @Parameter(description = "언팔로우 대상 유저 ID", example = "12", required = true)
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.UNFOLLOW_SUCCESS,
                userFollowService.unfollow(me.getUserId(), userId)
        );
    }

    @Operation(
            summary = "맞팔(친구) 목록 조회 - 무한 스크롤",
            description = """
                로그인한 사용자의 '맞팔(서로 팔로우)' 친구 목록을 조회합니다.

                ✅ 무한 스크롤 방식(cursor 기반)
                - 첫 호출: cursor 없이 요청합니다.
                  예) GET /api/v1/me/friends/mutual?size=20
                - 응답으로 내려온 nextCursor를 다음 호출의 cursor로 전달합니다.
                  예) GET /api/v1/me/friends/mutual?size=20&cursor=95
                - hasNext가 false이면 더 이상 불러올 데이터가 없습니다.

                ✅ cursor 의미
                - 현재 응답에서 받은 items의 마지막 userId를 다음 요청 cursor로 사용합니다.
                - 정렬이 userId DESC 기준이라 cursor는 "마지막 userId보다 작은 값(< cursor)"을 다음으로 가져옵니다.

                ✅ 프론트에서 필요한 값
                - size: 한 번에 가져올 개수(기본 20)
                - cursor: 다음 페이지 시작점(처음은 생략, 다음부터는 nextCursor 사용)
                - Authorization: Bearer AccessToken (필수)
                """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MutualFriendsResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 파라미터 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @GetMapping("/api/v1/me/friends/mutual")
    public ApiResponse<MutualFriendsResponse> mutualFriends(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails me,

            @Parameter(description = "무한스크롤 커서(마지막 userId). 첫 호출은 생략.", example = "95")
            @RequestParam(required = false) Long cursor,

            @Parameter(description = "가져올 개수(기본 20). hasNext 판단을 위해 내부적으로 size+1 조회할 수 있음", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                userFollowService.getMutualFriends(me.getUserId(), cursor, size)
        );
    }
}

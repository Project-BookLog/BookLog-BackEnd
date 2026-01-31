package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.service.FriendsReadingRankingService;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "내 친구(맞팔) 독서 랭킹",
        description = "맞팔 친구 기준 월간 독서 랭킹 조회 API (Top3 / 전체보기-무한스크롤)"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me/friends")
public class MeFriendsController {

    private final FriendsReadingRankingService friendsReadingRankingService;

    @Operation(
            summary = "친구 독서 랭킹 Top3 조회",
            description = """
                    맞팔(상호 팔로우) 친구 중, 특정 월(month)의 독서 랭킹 상위 3명만 반환합니다.

                    - 사용처: 마이페이지 요약 화면 / 상단 고정 카드 영역
                    - 페이징/커서 없음: 항상 Top3만 내려줍니다.

                    요청 예:
                    - GET /api/v1/me/friends/reading-ranking/top3?month=2026-01

                    파라미터:
                    - month (필수): YYYY-MM
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FriendReadingRankingTop3Response.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 형식 오류 (month 형식 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/reading-ranking/top3")
    public ApiResponse<FriendReadingRankingTop3Response> getReadingRankingTop3(
            @AuthenticationPrincipal CustomUserDetails me,
            @Parameter(description = "조회 월 (YYYY-MM)", example = "2026-01", required = true)
            @RequestParam String month
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                friendsReadingRankingService.getTop3(me.getUserId(), month)
        );
    }

    @Operation(
            summary = "친구 독서 랭킹 전체보기 조회 (Top3 포함 + 무한 스크롤)",
            description = """
                    맞팔(상호 팔로우) 친구의 월간 독서 랭킹을 '커서 기반 무한 스크롤'로 조회합니다.

                    ✅ 무한 스크롤(커서 기반) 규칙
                    - 이 API는 page 기반이 아니라 cursor 기반입니다.
                    - 서버는 응답에 nextCursor(= 마지막으로 내려준 item의 rank)를 내려줍니다.
                    - 프론트는 다음 요청 시 cursor에 nextCursor를 넣어서 이어서 조회합니다.

                    ✅ 호출 흐름
                    1) 첫 호출(초기 로딩):
                       - GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20
                       - cursor 미전달(또는 0) 시:
                         - top3: 상단 고정 영역용으로 포함
                         - items: 4등부터 size개
                    2) 다음 호출(스크롤 추가 로딩):
                       - GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20&cursor={nextCursor}
                       - items만 이어서 반환 (top3는 빈 리스트)

                    ✅ 응답 필드 의미
                    - top3: 첫 호출(cursor 없음/0)일 때만 포함 (이후 호출은 빈 리스트)
                    - items: 무한 스크롤로 append할 리스트(4등부터)
                    - nextCursor: 다음 요청 cursor로 사용할 값(마지막 item의 rank)
                    - hasNext: 추가 데이터 존재 여부
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FriendReadingRankingInfiniteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 형식 오류 (month/size/cursor 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/reading-ranking")
    public ApiResponse<FriendReadingRankingInfiniteResponse> getReadingRankingInfinite(
            @AuthenticationPrincipal CustomUserDetails me,
            @Parameter(description = "조회 월 (YYYY-MM)", example = "2026-01", required = true)
            @RequestParam String month,

            @Parameter(
                    description = "커서(마지막으로 받은 rank). 첫 호출은 생략 또는 0, 이후 호출부터 nextCursor를 넣어 요청",
                    example = "23"
            )
            @RequestParam(required = false) Integer cursor,

            @Parameter(description = "한 번에 가져올 개수 (기본 20)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                friendsReadingRankingService.getInfinite(me.getUserId(), month, cursor, size)
        );
    }
}

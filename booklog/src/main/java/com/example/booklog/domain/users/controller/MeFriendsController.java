package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.service.FriendsReadingRankingService;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 내 친구(맞팔) 독서 랭킹 API
 *
 * ✅ 무한 스크롤(커서 기반) 동작 방식
 * - 이 API는 "page" 기반이 아니라 "cursor" 기반으로 다음 데이터를 이어서 조회합니다.
 * - 서버는 응답에 nextCursor(= 마지막으로 내려준 rank 값)를 내려주고,
 *   프론트는 다음 요청 때 그 값을 cursor로 다시 보내야 합니다.
 *
 * ✅ 요청/응답 흐름 예시
 * 1) 첫 요청(초기 로딩)
 *    GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20
 *    - cursor를 보내지 않으면(또는 cursor=0) top3를 포함해 내려줍니다.
 *    - items는 4등부터 size개 내려줍니다.
 *
 * 2) 다음 요청(스크롤로 추가 로딩)
 *    GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20&cursor={nextCursor}
 *    - 이전 응답의 nextCursor 값을 cursor로 보내면,
 *      해당 rank 다음(= afterRank)부터 이어서 items를 내려줍니다.
 *
 * ✅ 파라미터 설명
 * - month (필수): YYYY-MM (예: 2026-01)
 * - size (선택): 한 번에 가져올 개수, 기본 20
 * - cursor (선택): 커서(마지막 rank). 첫 호출은 생략/0, 다음 호출부터 응답의 nextCursor를 넣어 요청
 *
 * ✅ 응답 필드
 * - top3:
 *   - 첫 호출(cursor 없음/0)일 때만 포함됩니다. (전체보기 화면에서 상단 고정 영역용)
 *   - 이후 호출(cursor 존재)에서는 빈 리스트로 내려옵니다. (프론트가 이미 갖고 있으므로)
 * - items: 무한 스크롤로 append할 리스트(4등부터)
 * - nextCursor: 다음 요청에 넣을 커서(마지막 item의 rank). hasNext=false면 null일 수 있음
 * - hasNext: 다음 페이지(추가 데이터)가 있는지 여부
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me/friends")
public class MeFriendsController {

    private final FriendsReadingRankingService friendsReadingRankingService;

    /**
     * Top3만 조회 (요약 화면/상단 카드 전용)
     *
     * - 랭킹 Top3(맞팔 친구 기준)를 반환합니다.
     * - 페이지네이션/커서 없이 항상 Top3만 내려줍니다.
     *
     * 요청 예:
     * GET /api/v1/me/friends/reading-ranking/top3?month=2026-01
     */
    @GetMapping("/reading-ranking/top3")
    public ApiResponse<FriendReadingRankingTop3Response> getReadingRankingTop3(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam String month
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                friendsReadingRankingService.getTop3(me.getUserId(), month)
        );
    }

    /**
     * 전체보기 조회 (Top3 포함 + 무한 스크롤)
     *
     * - 첫 호출(cursor 없음/0)에는 top3 + items(4등부터)를 내려줍니다.
     * - 이후 호출(cursor 존재)에는 items만 이어서 내려줍니다.
     * - nextCursor를 프론트가 저장했다가 다음 요청의 cursor로 넣어야 무한 스크롤이 동작합니다.
     *
     * 요청 예:
     * 1) 첫 호출:
     *    GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20
     *
     * 2) 다음 호출:
     *    GET /api/v1/me/friends/reading-ranking?month=2026-01&size=20&cursor=23
     */
    @GetMapping("/reading-ranking")
    public ApiResponse<FriendReadingRankingInfiniteResponse> getReadingRankingInfinite(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam String month,
            @RequestParam(required = false) Integer cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.READ_SUCCESS,
                friendsReadingRankingService.getInfinite(me.getUserId(), month, cursor, size)
        );
    }
}
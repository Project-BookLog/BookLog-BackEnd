package com.example.booklog.global.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode {
    // 인증 관련 성공 코드
    SIGNUP_SUCCESS("AUTH_S001", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS("AUTH_S002", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS("AUTH_S003", "로그아웃되었습니다."),
    TOKEN_REFRESH_SUCCESS("AUTH_S004", "토큰이 갱신되었습니다."),

    // ✅ 공통/조회 성공 (랭킹 등 일반 API에서도 사용)
    READ_SUCCESS("AUTH_S100", "조회에 성공했습니다."),

    //친구 관련 성공
    FOLLOW_SUCCESS("AUTH_S101", "팔로우에 성공했습니다."),
    UNFOLLOW_SUCCESS("AUTH_S102", "언팔로우에 성공했습니다.");

    private final String code;
    private final String message;
}

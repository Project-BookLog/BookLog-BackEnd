package com.example.booklog.global.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {
    // 인증 관련 에러
    NOT_FOUND("AUTH_001", "사용자를 찾을 수 없습니다."),
    INVALID("AUTH_002", "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED("AUTH_003", "인증되지 않은 사용자입니다."),
    FORBIDDEN("AUTH_004", "권한이 없습니다."),
    TOKEN_EXPIRED("AUTH_005", "토큰이 만료되었습니다."),
    INVALID_TOKEN("AUTH_006", "유효하지 않은 토큰입니다."),
    DUPLICATE_EMAIL("AUTH_007", "이미 존재하는 이메일입니다."),
    INVALID_EMAIL_FORMAT("AUTH_008", "올바르지 않은 이메일 형식입니다."),
    INVALID_PASSWORD("AUTH_009", "비밀번호가 올바르지 않습니다. 회원탈퇴를 진행할 수 없습니다."),
    KAKAO_API_ERROR("AUTH_010", "카카오 로그인 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요."),
    KAKAO_TOKEN_INVALID("AUTH_011", "카카오 액세스 토큰이 유효하지 않거나 만료되었습니다. 카카오 개발자 도구에서 새 토큰을 발급받아주세요.");

    private final String code;
    private final String message;
}

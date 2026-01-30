package com.example.booklog.global.common.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode {
    // 일반 에러 코드
    BAD_REQUEST("G001", "잘못된 요청입니다."),
    UNAUTHORIZED("G002", "인증되지 않은 사용자입니다."),
    FORBIDDEN("G003", "권한이 없습니다."),
    NOT_FOUND("G004", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("G005", "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED("G006", "허용되지 않은 메서드입니다."),
    VALIDATION_ERROR("G007", "입력값 검증에 실패했습니다.");

    private final String code;
    private final String message;
}

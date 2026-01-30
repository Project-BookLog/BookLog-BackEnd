package com.example.booklog.global.common.apiPayload.code.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus {
    OK(HttpStatus.OK, "S000", "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "S001", "생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

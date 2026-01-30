package com.example.booklog.global.common.apiPayload.code.generalStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralSuccessCode {

    OK(HttpStatus.OK, "COMMON200", "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "리소스가 성공적으로 생성되었습니다."),
    UPDATED(HttpStatus.OK, "COMMON202", "리소스가 성공적으로 수정되었습니다."),
    DELETED(HttpStatus.OK, "COMMON203", "리소스가 성공적으로 삭제되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON204", "요청이 접수되었습니다.");

    private final HttpStatus httpStatus;  // ex: 200 OK
    private final String code;            // ex: COMMON200
    private final String message;         // ex: "요청이 성공적으로 처리되었습니다."
}
package com.example.booklog.global.common.apiPayload.exception;

import com.example.booklog.global.common.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


//프로젝트 exception을 의미한다
@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private final BaseErrorCode code;
}

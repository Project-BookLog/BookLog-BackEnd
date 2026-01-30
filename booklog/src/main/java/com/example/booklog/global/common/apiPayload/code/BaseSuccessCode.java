package com.example.booklog.global.common.apiPayload.code;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode {

    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();

}

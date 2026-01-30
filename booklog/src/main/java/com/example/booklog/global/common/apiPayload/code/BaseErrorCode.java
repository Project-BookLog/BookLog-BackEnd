package com.example.booklog.global.common.apiPayload.code;


import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();

}

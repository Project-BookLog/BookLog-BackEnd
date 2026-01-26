package com.example.booklog.domain.library.shelves.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽기 상태")
public enum ReadingStatus {
    TO_READ,   //읽을 예정
    READING,    //읽는 중
    COMPLETED,  //완독
    STOPPED     //중단
}


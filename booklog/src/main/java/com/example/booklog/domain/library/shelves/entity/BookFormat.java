package com.example.booklog.domain.library.shelves.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "책 종류/매체")
public enum BookFormat { //format
    PAPER,      //종이책
    EBOOK,      //전자책
    AUDIO       //오디오
}


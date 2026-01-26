package com.example.booklog.domain.library.shelves.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "도서 정렬 방법")
public enum UserBookSort {
    LATEST,
    OLDEST,
    TITLE,
    AUTHOR
}

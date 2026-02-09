package com.example.booklog.domain.library.shelves.dto;

import java.util.List;

public record CurrentReadingResponse(
        long count, // 읽는 중 총 개수
        List<UserBookListItemResponse> items // 홈 카드 리스트
) {}
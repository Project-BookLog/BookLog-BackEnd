package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SimilarTopicPostItemResponse {

    private Long postId;

    private String bookTitle;

    private String excerpt;

    // "1일 전"은 프론트가 계산
    private LocalDateTime createdAt;

    private long viewCount;
    private long bookmarkCount;

    // 오른쪽 작은 썸네일
    private String thumbnailImageUrl;

}

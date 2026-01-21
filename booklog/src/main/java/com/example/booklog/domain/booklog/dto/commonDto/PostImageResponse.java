package com.example.booklog.domain.booklog.dto.commonDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostImageResponse {
    private Long imageId;
    private String imageUrl;
    private int order;
}

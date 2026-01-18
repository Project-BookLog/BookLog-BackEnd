package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BooklogRecommendationResponse {
    private List<SimilarBookCardResponse> similarBooks;
    private List<SimilarTopicPostItemResponse> popularPosts;
}

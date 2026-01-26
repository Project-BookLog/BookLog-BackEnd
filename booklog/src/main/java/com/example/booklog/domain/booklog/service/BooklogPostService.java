package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.controller.BooklogPostController;
import com.example.booklog.domain.booklog.dto.*;
import org.springframework.data.domain.Pageable;


public interface BooklogPostService {

    BooklogPostCreateResponse create(Long userId, BooklogPostCreateRequest request);

    BooklogFeedResponse getFeed(Long userId, BooklogFeedQuery query, Pageable pageable);

    BooklogDetailResponse getDetail(Long userId, Long postId);

    BooklogRecommendationResponse getRecommendations(Long userId, Long postId);

    void softDelete(Long userId, Long postId);

    BookmarkToggleResult toggleBookmark(Long userId, Long postId);
}

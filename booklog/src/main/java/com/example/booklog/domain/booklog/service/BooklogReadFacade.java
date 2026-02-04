package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.dto.BooklogFeedQuery;
import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.booklog.dto.BooklogRecommendationResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;


import java.util.Collection;
import java.util.List;


public interface BooklogReadFacade {

    AuthorView findAuthorSummary(Long userId);
    AuthorView findAuthorDetail(Long userId, Long viewerId);
    List<AuthorView> findAuthorSummariesByIds(Collection<Long> userIds);
    BookView findBook(Long bookId);
    List<BookView> findBooksByIds(Collection<Long> bookIds);
    List<TagView> findTagsByPostId(Long postId);
    List<TagView> findTagsByTagIds(List<Long> tagIds);
    boolean isBookmarkedByMe(Long userId, Long postId);
    Slice<BooklogPost> findFeedPostsSlice(BooklogFeedQuery query, Pageable pageable);
    BooklogRecommendationResponse buildRecommendations(Long postId);
    void validateCreateRequest(Long userId, Long bookId);
    BooklogFeedResponse assembleFeedCards(Long viewerId, Slice<BooklogPost> slice);
}

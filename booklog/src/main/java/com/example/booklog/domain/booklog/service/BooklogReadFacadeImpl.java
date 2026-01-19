package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.dto.BooklogFeedQuery;
import com.example.booklog.domain.booklog.dto.BooklogRecommendationResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BooklogReadFacadeImpl implements BooklogReadFacade {

    @Override
    public AuthorView findAuthorSummary(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public AuthorView findAuthorDetail(Long userId, Long viewerId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public BookView findBook(Long bookId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<TagView> findTagsByPostId(Long postId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isBookmarkedByMe(Long userId, Long postId) {
        return false; // TODO
    }

    @Override
    public Slice<BooklogPost> findFeedPostsSlice(BooklogFeedQuery query, Pageable pageable) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public BooklogRecommendationResponse buildRecommendations(Long postId) {
        throw new UnsupportedOperationException("TODO");
    }
}

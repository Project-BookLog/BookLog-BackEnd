package com.example.booklog.domain.booklog.view;

import java.time.LocalDateTime;

public interface PopularPostItemView {

    Long getPostId();
    String getBookTitle();
    String getExcerpt();
    LocalDateTime getCreatedAt();
    Long getViewCount();
    Long getBookmarkCount();
    String getThumbnailImageUrl();
}

package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkToggleResult {
    private boolean bookmarkedByMe;
    private long bookmarkCount;
}
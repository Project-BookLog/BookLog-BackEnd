package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BooklogFeedResponse {

    private List<BooklogPostCardResponse> items;
    private boolean hasNext;
}

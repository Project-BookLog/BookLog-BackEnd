package com.example.booklog.domain.booklog.dto.commonDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookSummaryResponse {

    private Long bookId;
    private String title;
    private String authorName;
    private String publisher;
    private String coverImageUrl;
}

package com.example.booklog.domain.booklog.dto;

import com.example.booklog.domain.booklog.dto.commonDto.AuthorDetailResponse;
import com.example.booklog.domain.booklog.dto.commonDto.BookSummaryResponse;
import com.example.booklog.domain.booklog.dto.commonDto.PostImageResponse;
import com.example.booklog.domain.booklog.dto.commonDto.TagChipResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BooklogDetailResponse {
    private Long postId;

    // 공통조각(상세형): 이메일/팔로우 버튼을 위해 더 많은 정보
    private AuthorDetailResponse author;

    private BookSummaryResponse book;

    private List<TagChipResponse> tags;

    private String content;

    private long viewCount;

    private boolean bookmarkedByMe;
    private long bookmarkCount;

    private List<PostImageResponse> images;

    private LocalDateTime createdAt;
}

package com.example.booklog.domain.booklog.dto;

import com.example.booklog.domain.booklog.dto.commonDto.AuthorSummaryResponse;
import com.example.booklog.domain.booklog.dto.commonDto.BookSummaryResponse;
import com.example.booklog.domain.booklog.dto.commonDto.PostImageResponse;
import com.example.booklog.domain.booklog.dto.commonDto.TagChipResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BooklogPostCardResponse {

    // 게시글 식별
    private Long postId;

    // 작성자 요약 정보 (프로필 이미지 + 닉네임)
    private AuthorSummaryResponse author;

    // 작성 시각 (계산은 프론트에서)
    private LocalDateTime createdAt;

    // 조회수
    private long viewCount;

    // 북마크 수
    private long bookmarkCount;

    // 내가 북마크했는지 여부
    private boolean bookmarkedByMe;

    // 책 요약 정보 (표지 + 제목/저자/출판사)
    private BookSummaryResponse book;

    // 게시글 이미지들 (최대 8장, 순서 포함)
    private List<PostImageResponse> images;

    // 본문 미리보기
    private String excerpt;

    // 태그 칩들
    private List<TagChipResponse> tags;
}

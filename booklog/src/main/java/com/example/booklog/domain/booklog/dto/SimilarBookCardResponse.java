package com.example.booklog.domain.booklog.dto;

import com.example.booklog.domain.booklog.dto.commonDto.TagChipResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SimilarBookCardResponse {

    private Long bookId;
    private String title;
    private String authorName;
    private String publisher;
    private String coverImageUrl;

    private List<TagChipResponse> tags;

}

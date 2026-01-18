package com.example.booklog.domain.booklog.dto.commonDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorSummaryResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl; // nullable
}

package com.example.booklog.domain.booklog.dto.commonDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorDetailResponse {

    private Long userId;
    private String nickname;
    private String email;           // 상세 화면에 노출
    private String profileImageUrl;
    private boolean followedByMe;   // 팔로우 버튼 상태
}

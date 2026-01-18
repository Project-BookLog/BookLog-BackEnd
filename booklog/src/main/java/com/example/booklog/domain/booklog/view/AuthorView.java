package com.example.booklog.domain.booklog.view;

public interface AuthorView {

    Long getUserId();
    String getNickname();
    String getEmail();           // 상세에서만 필요하면 null 가능
    String getProfileImageUrl();
    Boolean getFollowedByMe();   // 상세에서만 필요하면 null 가
}

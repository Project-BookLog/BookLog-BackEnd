package com.example.booklog.global.auth.dto;

import java.util.Map;

/**
 * OAuth2 사용자 정보 인터페이스
 * 각 OAuth2 제공자의 사용자 정보를 추상화
 */
public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
    String getProfileImageUrl();
    Map<String, Object> getAttributes();
}

package com.example.booklog.global.auth.security;

import com.example.booklog.domain.users.entity.AuthAccounts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * OAuth2 로그인 시 사용되는 사용자 정보 클래스
 */
@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final AuthAccounts account;
    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(account.getRole().name())
        );
    }

    @Override
    public String getName() {
        return account.getProviderId();
    }

    public Long getUserId() {
        return account.getUser().getId();
    }

    public AuthAccounts getAccount() {
        return account;
    }
}

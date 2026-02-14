package com.example.booklog.global.auth.security;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccountsRepository authAccountsRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username은 "email:provider" 형식 (예: "test@naver.com:KAKAO")
        String[] parts = username.split(":", 2);

        if (parts.length != 2) {
            log.error("❌ 잘못된 username 형식: {}", username);
            throw new AuthException(AuthErrorCode.NOT_FOUND);
        }

        String email = parts[0];
        String providerStr = parts[1];

        try {
            AuthProvider provider = AuthProvider.valueOf(providerStr);
            log.debug("🔍 사용자 조회: email={}, provider={}", email, provider);

            AuthAccounts account = authAccountsRepository
                    .findByEmailAndProvider(email, provider)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

            log.info("✅ 사용자 조회 성공: email={}, provider={}", email, provider);
            return new CustomUserDetails(account);

        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 provider: {}", providerStr);
            throw new AuthException(AuthErrorCode.NOT_FOUND);
        }
    }
}

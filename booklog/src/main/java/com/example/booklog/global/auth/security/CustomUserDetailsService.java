package com.example.booklog.global.auth.security;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccountsRepository authAccountsRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username(이메일)으로 LOCAL provider의 AuthAccounts 조회
        AuthAccounts account = authAccountsRepository
                .findByEmailAndProvider(username, AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // CustomUserDetails 반환
        return new CustomUserDetails(account);
    }
}

package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.global.auth.Repository.AuthAccountsRepository;
import com.example.booklog.global.auth.converter.AuthConverter;
import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.auth.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthQueryServiceImpl implements AuthQueryService {

    private final AuthAccountsRepository authAccountsRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public AuthResDTO.LoginDTO login(AuthReqDTO.@Valid LoginDTO dto) {
        // 1. LOCAL provider로 AuthAccounts 조회
        AuthAccounts account = authAccountsRepository
                .findByEmailAndProvider(dto.email(), AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // 2. 비밀번호 검증
        if (!encoder.matches(dto.password(), account.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID);
        }

        // 3. 마지막 로그인 시간 업데이트
        account.updateLastLogin();

        // 4. CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(account);

        // 5. JWT 액세스 토큰 발급
        String accessToken = jwtUtil.createAccessToken(userDetails);

        // 6. 응답 DTO 반환
        return AuthConverter.toLoginDTO(account, accessToken);
    }
}

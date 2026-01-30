package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.global.auth.entity.RefreshToken;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;
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

        // 6. JWT 리프레시 토큰 발급
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        // 7. 기존 RefreshToken 삭제 후 새로 저장
        refreshTokenRepository.findByEmail(dto.email())
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(refreshToken)
                .email(dto.email())
                .expiryDate(java.time.LocalDateTime.now().plus(jwtUtil.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 8. 응답 DTO 반환
        return AuthConverter.toLoginDTO(account, accessToken, refreshToken, jwtUtil.getAccessExpirationMillis() / 1000);
    }

    @Override
    @Transactional
    public AuthResDTO.LoginDTO refreshToken(AuthReqDTO.@Valid RefreshTokenDTO dto) {
        // 1. RefreshToken 유효성 검증
        if (!jwtUtil.isValid(dto.refreshToken())) {
            throw new AuthException(AuthErrorCode.INVALID);
        }

        // 2. DB에서 RefreshToken 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(dto.refreshToken())
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // 3. 만료 여부 확인
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException(AuthErrorCode.INVALID);
        }

        // 4. 이메일로 사용자 조회
        String email = jwtUtil.getEmail(dto.refreshToken());
        AuthAccounts account = authAccountsRepository
                .findByEmailAndProvider(email, AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // 5. CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(account);

        // 6. 새로운 AccessToken 생성
        String newAccessToken = jwtUtil.createAccessToken(userDetails);

        // 7. 새로운 RefreshToken 생성
        String newRefreshToken = jwtUtil.createRefreshToken(userDetails);

        // 8. DB의 RefreshToken 업데이트
        storedToken.updateToken(newRefreshToken, java.time.LocalDateTime.now().plus(jwtUtil.getRefreshExpiration()));
        refreshTokenRepository.save(storedToken);

        // 9. 응답 DTO 반환
        return AuthConverter.toLoginDTO(account, newAccessToken, newRefreshToken, jwtUtil.getAccessExpirationMillis() / 1000);
    }
}

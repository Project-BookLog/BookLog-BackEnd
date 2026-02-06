package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.UserSettings;
import com.example.booklog.domain.users.repository.UserSettingsRepository;
import com.example.booklog.global.auth.entity.RefreshToken;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.repository.RefreshTokenRepository;
import com.example.booklog.global.auth.converter.AuthConverter;
import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.enums.Role;
import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandServiceImpl implements AuthCommandService {

    private final PasswordEncoder passwordEncoder;
    private final AuthAccountsRepository authAccountsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSettingsRepository userSettingsRepository;

    // 회원가입
    @Override
    @Transactional
    public AuthResDTO.JoinDTO signup(AuthReqDTO.JoinDTO dto) {
        // 1. 이메일 중복 체크 (LOCAL provider)
        if (authAccountsRepository.findByEmailAndProvider(dto.email(), AuthProvider.LOCAL).isPresent()) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(dto.password());

        // 3. AuthAccounts + Users 생성
        AuthAccounts account = AuthConverter.toAuthAccount(dto, encryptedPassword, Role.ROLE_USER);

        // 4. DB 저장 (cascade로 Users도 함께 저장됨)
        AuthAccounts savedAccount = authAccountsRepository.save(account);

        // 5. UserSettings 생성 (서재/북로그 공개 설정 기본값: true)
        UserSettings userSettings = UserSettings.builder()
                .user(savedAccount.getUser())
                .isShelfPublic(true)
                .isPostPublic(true)
                .build();
        userSettingsRepository.save(userSettings);

        // 6. 응답 DTO 반환
        return AuthConverter.toJoinDTO(savedAccount);
    }

    // 로그아웃
    @Override
    @Transactional
    public AuthResDTO.LogoutDTO logout(AuthReqDTO.LogoutDTO dto) {
        // 1. Refresh Token 존재 여부 확인
        RefreshToken refreshToken = refreshTokenRepository.findByToken(dto.refreshToken())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

        // 2. Refresh Token DB에서 삭제
        refreshTokenRepository.delete(refreshToken);

        // 3. 성공 응답 반환
        return AuthResDTO.LogoutDTO.builder()
                .message("로그아웃이 완료되었습니다.")
                .build();
    }
}

package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.UserSettings;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UserSettingsRepository;
import com.example.booklog.domain.users.repository.UsersRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandServiceImpl implements AuthCommandService {

    private final PasswordEncoder passwordEncoder;
    private final AuthAccountsRepository authAccountsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UsersRepository usersRepository;

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

    // 회원탈퇴 (인앱 로그인 + 소셜 로그인 통합)
    @Override
    @Transactional
    public AuthResDTO.DeleteAccountDTO deleteAccount(Long userId, AuthReqDTO.DeleteAccountDTO dto) {
        log.info("회원탈퇴 시도: userId={}", userId);

        // 1. 사용자 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // 2. AuthAccounts 조회
        AuthAccounts authAccount = authAccountsRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        // 3. 비밀번호 검증 (LOCAL provider인 경우에만)
        if (authAccount.getProvider() == AuthProvider.LOCAL) {
            // LOCAL 사용자는 비밀번호 필수
            if (dto.password() == null || dto.password().isBlank()) {
                throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
            }
            // 비밀번호 일치 확인
            if (!passwordEncoder.matches(dto.password(), authAccount.getPassword())) {
                throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
            }
        }
        // 소셜 로그인은 비밀번호 검증 생략

        // 4. 사용자 관련 데이터 삭제
        // 4-1. RefreshToken 삭제
        refreshTokenRepository.findByEmail(authAccount.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        log.info("RefreshToken 삭제 완료: userId={}", userId);

        // 4-2. UserSettings 삭제
        userSettingsRepository.findById(userId)
                .ifPresent(userSettingsRepository::delete);
        log.info("UserSettings 삭제 완료: userId={}", userId);

        // 4-3. AuthAccounts 삭제
        authAccountsRepository.delete(authAccount);
        log.info("AuthAccounts 삭제 완료: userId={}", userId);

        // 4-4. Users 삭제 (마지막에 삭제, 외래키 관계 때문에)
        usersRepository.delete(user);
        log.info("Users 삭제 완료: userId={}", userId);

        // 5. 성공 응답 반환
        return AuthConverter.toDeleteAccountDTO();
    }
}

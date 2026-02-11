package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.UserSettings;
import com.example.booklog.domain.users.entity.UserStatus;
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
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.auth.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthQueryServiceImpl implements AuthQueryService {

    private final AuthAccountsRepository authAccountsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    private final RestTemplate restTemplate = new RestTemplate();

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

    @Override
    @Transactional
    public AuthResDTO.LoginDTO kakaoLogin(AuthReqDTO.@Valid KakaoLoginDTO dto) {
        log.info("=== 카카오 로그인 API 호출 시작 ===");
        log.info("받은 카카오 토큰 (앞 20자): {}", dto.kakaoAccessToken().substring(0, Math.min(20, dto.kakaoAccessToken().length())));

        // 1. 카카오 API로 사용자 정보 조회
        String kakaoUserInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(dto.kakaoAccessToken());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response;
        try {
            log.info("카카오 API 호출 시도: {}", kakaoUserInfoUrl);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> temp = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(kakaoUserInfoUrl, HttpMethod.GET, entity, Map.class);
            response = temp;

            log.info("카카오 API 응답 상태: {}", response.getStatusCode());

            // 응답 상태 코드 확인
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("카카오 API 호출 실패: HTTP Status {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new AuthException(AuthErrorCode.KAKAO_API_ERROR);
            }
        } catch (AuthException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("카카오 API 호출 실패 (HTTP 에러): Status={}, Message={}, Body={}",
                    e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_INVALID);
        } catch (Exception e) {
            log.error("카카오 API 호출 실패 (예외): Type={}, Message={}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_INVALID);
        }

        Map<String, Object> attributes = response.getBody();
        if (attributes == null || attributes.isEmpty()) {
            log.error("카카오 API 응답이 비어있습니다.");
            throw new AuthException(AuthErrorCode.KAKAO_API_ERROR);
        }

        log.info("카카오 API 응답 데이터: {}", attributes);

        // 2. 카카오 사용자 정보 파싱
        String providerId = String.valueOf(attributes.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;

        log.info("카카오 로그인 시도: providerId={}, email={}, nickname={}", providerId, email, nickname);

        if (email == null || email.isEmpty()) {
            log.error("카카오 계정에 이메일 정보가 없습니다. providerId={}", providerId);
            throw new AuthException(AuthErrorCode.KAKAO_API_ERROR);
        }

        // ...existing code...

        // 3. 기존 계정 조회 또는 신규 생성
        AuthAccounts authAccount = authAccountsRepository
                .findByProviderIdAndProvider(providerId, AuthProvider.KAKAO)
                .orElseGet(() -> createKakaoAccount(providerId, email, nickname, profileImageUrl));

        // 4. 기존 계정이면 마지막 로그인 시간 및 프로필 업데이트
        authAccount.updateLastLogin();
        authAccount.updateProfile(email, nickname, profileImageUrl);
        authAccountsRepository.save(authAccount);

        // 5. CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(authAccount);

        // 6. JWT 액세스 토큰 발급
        String accessToken = jwtUtil.createAccessToken(userDetails);

        // 7. JWT 리프레시 토큰 발급
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        // 8. 기존 RefreshToken 삭제 후 새로 저장
        refreshTokenRepository.findByEmail(email)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(refreshToken)
                .email(email)
                .expiryDate(java.time.LocalDateTime.now().plus(jwtUtil.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 9. 응답 DTO 반환
        return AuthConverter.toLoginDTO(authAccount, accessToken, refreshToken, jwtUtil.getAccessExpirationMillis() / 1000);
    }

    /**
     * 카카오 신규 계정 생성
     */
    private AuthAccounts createKakaoAccount(String providerId, String email, String nickname, String profileImageUrl) {
        log.info("신규 카카오 사용자 등록: providerId={}", providerId);

        // Users 엔티티 생성
        Users newUser = Users.builder()
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .status(UserStatus.ACTIVE)
                .build();
        usersRepository.save(newUser);

        // AuthAccounts 생성
        AuthAccounts newAuthAccount = AuthAccounts.builder()
                .user(newUser)
                .provider(AuthProvider.KAKAO)
                .providerId(providerId)
                .email(email)
                .displayName(nickname)
                .profileImageUrl(profileImageUrl)
                .role(Role.ROLE_USER)
                .build();
        authAccountsRepository.save(newAuthAccount);

        // UserSettings 생성 (서재/북로그 공개 설정 기본값: true)
        UserSettings userSettings = UserSettings.builder()
                .user(newUser)
                .isShelfPublic(true)
                .isPostPublic(true)
                .build();
        userSettingsRepository.save(userSettings);

        return newAuthAccount;
    }
}

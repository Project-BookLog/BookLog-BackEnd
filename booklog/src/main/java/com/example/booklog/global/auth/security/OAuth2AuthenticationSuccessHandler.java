package com.example.booklog.global.auth.security;

import com.example.booklog.global.auth.entity.RefreshToken;
import com.example.booklog.global.auth.repository.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 처리 핸들러
 * JWT 토큰을 생성하고 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.frontend-redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getUserId();
        String email = oAuth2User.getAccount().getEmail();

        log.info("OAuth2 로그인 성공: userId={}, email={}", userId, email);

        // CustomUserDetails 생성 (role 정보 포함)
        CustomUserDetails userDetails = new CustomUserDetails(oAuth2User.getAccount());

        // JWT 토큰 생성 (issuedAt 포함, 매번 다른 토큰 생성)
        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        log.info("생성된 액세스 토큰 (앞 30자): {}", accessToken.substring(0, Math.min(30, accessToken.length())));
        log.info("생성된 리프레시 토큰 (앞 30자): {}", refreshToken.substring(0, Math.min(30, refreshToken.length())));

        // Refresh Token DB에 저장
        refreshTokenRepository.findByEmail(email)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .email(email)
                .expiryDate(java.time.LocalDateTime.now().plus(jwtUtil.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        log.info("OAuth2 로그인 리다이렉트: targetUrl={}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

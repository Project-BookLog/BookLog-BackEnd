package com.example.booklog.global.auth.security;

import com.example.booklog.domain.users.entity.AuthAccounts;
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

        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            Long userId = oAuth2User.getUserId();
            AuthAccounts account = oAuth2User.getAccount();
            String email = account.getEmail();
            String provider = account.getProvider().name();  // ✅ provider 추출

            log.info("OAuth2 로그인 성공: userId={}, email={}, provider={}", userId, email, provider);

            // JWT 토큰 생성 (email + provider 포함)
            String accessToken = jwtUtil.generateAccessToken(email, provider);
            String refreshToken = jwtUtil.generateRefreshToken(email, provider);

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

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 예외 발생: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication processing failed");
        }
    }
}

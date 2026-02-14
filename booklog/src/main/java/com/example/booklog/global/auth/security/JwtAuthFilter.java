package com.example.booklog.global.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 1. 공통 및 문서 관련 (인증 불필요)
        if (path.equals("/") || path.equals("/health") || path.equals("/favicon.ico") ||
                path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        // 2. OAuth2 내부 처리 경로 (인증 불필요)
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return true;
        }

        // 3. AuthController 중 '인증이 필요 없는' 특정 경로들
        return path.equals("/api/v1/auth/sign-up") ||   // 회원가입
                path.equals("/api/v1/auth/login") ||     // 일반 로그인
                path.equals("/api/v1/auth/refresh") ||   // 토큰 갱신
                path.equals("/api/v1/auth/kakao/login") || // 카카오 테스트용
                path.equals("/api/v1/auth/kakao/redirect") || // 카카오 리다이렉트
                path.equals("/api/v1/auth/callback");  // OAuth2 콜백
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        log.info("🔍 JWT 필터 진입: {} {}, Authorization 헤더: {}",
                request.getMethod(), requestPath,
                authHeader != null ? "Bearer " + authHeader.substring(7, Math.min(authHeader.length(), 20)) + "..." : "없음");

        try {
            // 토큰 가져오기
            String token = request.getHeader("Authorization");

            // token이 없거나 Bearer가 아니면 넘기기
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("⚠️ Authorization 헤더가 없거나 Bearer 형식이 아님: path={}", requestPath);
                filterChain.doFilter(request, response);
                return;
            }

            // Bearer이면 추출
            token = token.replace("Bearer ", "");
            log.debug("📝 토큰 추출 완료: {}...", token.substring(0, Math.min(token.length(), 20)));

            // AccessToken 검증하기: 올바른 토큰이면
            if (jwtUtil.isValid(token)) {
                // 토큰에서 이메일과 provider 추출
                String email = jwtUtil.getEmail(token);
                String provider = jwtUtil.getProvider(token);

                if (email == null || email.isEmpty()) {
                    log.error("❌ 토큰에서 이메일 추출 실패: token={}...", token.substring(0, Math.min(token.length(), 20)));
                    filterChain.doFilter(request, response);
                    return;
                }

                log.info("✅ 토큰 검증 성공: email={}, provider={}", email, provider);

                // 인증 객체 생성
                UserDetails user;

                if (provider != null && !provider.isEmpty()) {
                    // ✅ 새 토큰: email + provider로 조회
                    log.debug("🔍 DB에서 사용자 조회 시도: email={}, provider={}", email, provider);
                    String username = email + ":" + provider;
                    user = customUserDetailsService.loadUserByUsername(username);
                    log.info("✅ DB에서 사용자 조회 성공: email={}, provider={}", email, provider);
                } else {
                    // ⚠️ 기존 토큰: email만으로 조회 (하위 호환성)
                    log.warn("⚠️ 구 버전 토큰 감지 (provider 없음): email={}", email);
                    log.debug("🔍 DB에서 사용자 조회 시도 (이메일만): email={}", email);
                    user = customUserDetailsService.loadUserByUsername(email + ":LOCAL");  // 기본값 LOCAL
                    log.info("✅ DB에서 사용자 조회 성공 (기본 LOCAL): email={}", email);
                }

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                // 인증 완료 후 SecurityContextHolder에 넣기
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("✅ SecurityContext 인증 설정 완료: email={}, provider={}, authorities={}",
                        email, provider, user.getAuthorities());
            } else {
                log.error("❌ 토큰 검증 실패 (유효하지 않은 토큰): path={}, token={}...",
                        requestPath, token.substring(0, Math.min(token.length(), 20)));
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 서버 로그(web.stdout.log)에서 실제 에러 원인을 바로 확인할 수 있습니다.
            log.error("💥 JWT 필터 에러 발생: path={}, exceptionType={}, message={}",
                    requestPath, e.getClass().getSimpleName(), e.getMessage());

            // AuthException인 경우 더 자세히 로그
            if (e.getClass().getSimpleName().contains("Auth")) {
                log.error("🔍 인증 관련 에러 상세: ", e);
            }

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", "UNAUTHORIZED");
            errorResponse.put("message", "인증에 실패했습니다: " + e.getMessage());

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }
}

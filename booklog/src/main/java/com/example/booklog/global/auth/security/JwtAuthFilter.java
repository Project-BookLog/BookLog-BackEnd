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
        if (path.equals("/") || path.equals("/health") ||
                path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        // 2. OAuth2 내부 처리 경로 (인증 불필요)
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            return true;
        }

        // 3. AuthController 중 '인증이 필요 없는' 특정 경로들
        // startsWith("/api/v1/auth/")를 지우고 아래처럼 상세하게 적습니다.
        return path.equals("/api/v1/auth/sign-up") ||   // 회원가입
                path.equals("/api/v1/auth/login") ||     // 일반 로그인
                path.equals("/api/v1/auth/refresh") ||   // 토큰 갱신
                // path.equals("/api/v1/auth/kakao/login") || // 카카오 테스트용 - 주석처리
                // path.equals("/api/v1/auth/kakao/redirect") || // 카카오 리다이렉트 - 주석처리
                path.equals("/api/v1/auth/callback");  // OAuth2 콜백
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("요청 들어옴: {} {}, Authorization 헤더: {}",
                request.getMethod(), request.getRequestURI(), request.getHeader("Authorization"));

        try {
            // 토큰 가져오기
            String token = request.getHeader("Authorization");
            // token이 없거나 Bearer가 아니면 넘기기
            if (token == null || !token.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            // Bearer이면 추출
            token = token.replace("Bearer ", "");
            // AccessToken 검증하기: 올바른 토큰이면
            if (jwtUtil.isValid(token)) {
                // 토큰에서 이메일 추출
                String email = jwtUtil.getEmail(token);
                // 인증 객체 생성: 이메일로 찾아온 뒤, 인증 객체 생성
                UserDetails user = customUserDetailsService.loadUserByUsername(email);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                // 인증 완료 후 SecurityContextHolder에 넣기
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 서버 로그(web.stdout.log)에서 실제 에러 원인을 바로 확인할 수 있습니다.
            log.error("JWT 필터 에러 발생: {}", e.getMessage(), e);

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

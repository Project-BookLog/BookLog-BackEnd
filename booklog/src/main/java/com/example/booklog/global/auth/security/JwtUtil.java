package com.example.booklog.global.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret,
            @Value("${jwt.token.expiration.access}") Long accessExpiration,
            @Value("${jwt.token.expiration.refresh}") Long refreshExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
        this.refreshExpiration = Duration.ofMillis(refreshExpiration);
    }

    // AccessToken 생성
    public String createAccessToken(CustomUserDetails user) {
        return createToken(user, accessExpiration);
    }

    // RefreshToken 생성
    public String createRefreshToken(CustomUserDetails user) {
        return createToken(user, refreshExpiration);
    }

    // OAuth2 로그인용 AccessToken 생성 (이메일 + provider)
    public String generateAccessToken(String email, String provider) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", "ROLE_USER")
                .claim("email", email)
                .claim("provider", provider)  // ✅ provider 정보 추가
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpiration)))
                .signWith(secretKey)
                .compact();
    }

    // OAuth2 로그인용 RefreshToken 생성 (이메일 + provider)
    public String generateRefreshToken(String email, String provider) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", "ROLE_USER")
                .claim("email", email)
                .claim("provider", provider)  // ✅ provider 정보 추가
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshExpiration)))
                .signWith(secretKey)
                .compact();
    }

    // RefreshToken 만료 시간 가져오기
    public Duration getRefreshExpiration() {
        return refreshExpiration;
    }

    // AccessToken 만료 시간 가져오기 (밀리초)
    public Long getAccessExpirationMillis() {
        return accessExpiration.toMillis();
    }

    /** 토큰에서 이메일 가져오기
     *
     * @param token 유저 정보를 추출할 토큰
     * @return 유저 이메일을 토큰에서 추출합니다
     */
    public String getEmail(String token) {
        try {
            String email = getClaims(token).getPayload().getSubject(); // Parsing해서 Subject 가져오기
            log.debug("📧 토큰에서 이메일 추출 성공: {}", email);
            return email;
        } catch (JwtException e) {
            log.error("❌ 토큰에서 이메일 추출 실패: type={}, message={}, token={}...",
                    e.getClass().getSimpleName(), e.getMessage(),
                    token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");
            return null;
        }
    }

    /** 토큰에서 provider 가져오기
     *
     * @param token provider 정보를 추출할 토큰
     * @return provider (KAKAO, LOCAL, GOOGLE 등)
     */
    public String getProvider(String token) {
        try {
            Claims claims = getClaims(token).getPayload();
            String provider = claims.get("provider", String.class);
            log.debug("🔑 토큰에서 provider 추출 성공: {}", provider);
            return provider;
        } catch (JwtException e) {
            log.error("❌ 토큰에서 provider 추출 실패: type={}, message={}",
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /** 토큰 유효성 확인
     *
     * @param token 유효한지 확인할 토큰
     * @return True, False 반환합니다
     */
    public boolean isValid(String token) {
        try {
            Jws<Claims> claims = getClaims(token);
            Date expiration = claims.getPayload().getExpiration();
            log.debug("✅ 토큰 검증 성공: subject={}, expiration={}",
                    claims.getPayload().getSubject(), expiration);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("❌ 토큰 만료됨: expiration={}, token={}...",
                    e.getClaims().getExpiration(),
                    token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("❌ 토큰 서명 검증 실패 (잘못된 Secret Key): token={}...",
                    token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("❌ 토큰 형식이 잘못됨: message={}, token={}...",
                    e.getMessage(),
                    token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");
            return false;
        } catch (JwtException e) {
            log.error("❌ 토큰 검증 실패: type={}, message={}, token={}...",
                    e.getClass().getSimpleName(), e.getMessage(),
                    token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");
            return false;
        }
    }

    // 토큰 생성
    private String createToken(CustomUserDetails user, Duration expiration) {
        Instant now = Instant.now();

        // 인가 정보
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // provider 정보 추출
        String provider = user.getAccount().getProvider().name();

        return Jwts.builder()
                .subject(user.getUsername()) // User 이메일을 Subject로
                .claim("role", authorities)
                .claim("email", user.getUsername())
                .claim("provider", provider)  // ✅ provider 정보 추가
                .issuedAt(Date.from(now)) // 언제 발급한지
                .expiration(Date.from(now.plus(expiration))) // 언제까지 유효한지
                .signWith(secretKey) // sign할 Key
                .compact();
    }

    // 토큰 정보 가져오기
    private Jws<Claims> getClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token);
    }
}
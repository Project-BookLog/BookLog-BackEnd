package com.example.booklog.global.auth.controller;

import com.example.booklog.global.auth.security.JwtUtil;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import com.example.booklog.global.common.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Tag(name = "디버그", description = "토큰 검증 디버그 API (개발용)")
public class TokenDebugController {

    private final JwtUtil jwtUtil;

    @GetMapping("/token")
    @Operation(
            summary = "JWT 토큰 검증 디버그",
            description = """
                    개발용 디버그 엔드포인트입니다.
                    Authorization 헤더의 JWT 토큰을 검증하고 결과를 반환합니다.
                    
                    **사용법:**
                    1. Authorization 헤더에 `Bearer <token>` 형식으로 토큰 전달
                    2. 응답에서 토큰 검증 결과 확인
                    3. 서버 로그에서 상세한 에러 원인 확인
                    
                    **주의:** 프로덕션 환경에서는 이 엔드포인트를 제거하세요.
                    """
    )
    public ApiResponse<Map<String, Object>> debugToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                result.put("error", "Authorization 헤더가 없거나 Bearer 형식이 아닙니다");
                result.put("authHeader", authHeader);
                log.warn("🔍 디버그: Authorization 헤더 누락 또는 잘못된 형식");
                return ApiResponse.onSuccess(SuccessStatus.OK, result);
            }

            String token = authHeader.replace("Bearer ", "");
            result.put("tokenPrefix", token.substring(0, Math.min(token.length(), 30)) + "...");

            // 토큰 유효성 검증
            boolean isValid = jwtUtil.isValid(token);
            result.put("isValid", isValid);

            if (isValid) {
                // 토큰에서 이메일 추출
                String email = jwtUtil.getEmail(token);
                result.put("email", email);
                result.put("emailExtracted", email != null && !email.isEmpty());

                log.info("🔍 디버그: 토큰 검증 성공 - email={}", email);
            } else {
                result.put("error", "토큰 검증 실패 - 서버 로그에서 상세 원인을 확인하세요");
                log.error("🔍 디버그: 토큰 검증 실패 - 서버 로그 확인 필요");
            }

            // 토큰 만료 시간 정보 (참고용)
            result.put("accessExpirationMs", jwtUtil.getAccessExpirationMillis());

        } catch (Exception e) {
            result.put("error", "예외 발생: " + e.getMessage());
            result.put("exceptionType", e.getClass().getSimpleName());
            log.error("🔍 디버그: 예외 발생 - type={}, message={}", e.getClass().getSimpleName(), e.getMessage(), e);
        }

        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }

    @GetMapping("/health")
    @Operation(summary = "서버 상태 확인", description = "서버가 정상 작동 중인지 확인합니다.")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "서버가 정상 작동 중입니다");
        return ApiResponse.onSuccess(SuccessStatus.OK, result);
    }
}

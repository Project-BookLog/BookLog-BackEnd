package com.example.booklog.global.auth.controller;

import com.example.booklog.global.auth.security.JwtUtil;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import com.example.booklog.global.common.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(value = "/token-test-page")
    @Operation(
            summary = "토큰 검증 테스트 페이지",
            description = "브라우저에서 직접 토큰을 입력하고 검증할 수 있는 HTML 페이지를 반환합니다."
    )
    public ResponseEntity<String> tokenTestPage() {
        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>JWT 토큰 검증 테스트</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            max-width: 800px;
                            margin: 50px auto;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                        }
                        .container {
                            background: white;
                            padding: 30px;
                            border-radius: 10px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                        }
                        h1 {
                            color: #333;
                            text-align: center;
                            margin-bottom: 10px;
                        }
                        .subtitle {
                            text-align: center;
                            color: #666;
                            margin-bottom: 30px;
                        }
                        .input-group {
                            margin-bottom: 20px;
                        }
                        label {
                            display: block;
                            margin-bottom: 8px;
                            color: #555;
                            font-weight: bold;
                        }
                        textarea {
                            width: 100%;
                            min-height: 100px;
                            padding: 12px;
                            border: 2px solid #ddd;
                            border-radius: 5px;
                            font-family: monospace;
                            font-size: 14px;
                            resize: vertical;
                        }
                        textarea:focus {
                            outline: none;
                            border-color: #667eea;
                        }
                        button {
                            width: 100%;
                            padding: 15px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            border: none;
                            border-radius: 5px;
                            font-size: 16px;
                            font-weight: bold;
                            cursor: pointer;
                            transition: transform 0.2s;
                        }
                        button:hover {
                            transform: translateY(-2px);
                        }
                        button:active {
                            transform: translateY(0);
                        }
                        .result {
                            margin-top: 30px;
                            padding: 20px;
                            border-radius: 5px;
                            display: none;
                        }
                        .result.success {
                            background: #d4edda;
                            border: 2px solid #28a745;
                            display: block;
                        }
                        .result.error {
                            background: #f8d7da;
                            border: 2px solid #dc3545;
                            display: block;
                        }
                        .result-title {
                            font-weight: bold;
                            margin-bottom: 10px;
                            font-size: 18px;
                        }
                        .result-content {
                            background: white;
                            padding: 15px;
                            border-radius: 5px;
                            margin-top: 10px;
                            font-family: monospace;
                            white-space: pre-wrap;
                            word-break: break-all;
                        }
                        .loading {
                            text-align: center;
                            color: #667eea;
                            display: none;
                            margin-top: 20px;
                        }
                        .info-box {
                            background: #e7f3ff;
                            border-left: 4px solid #2196F3;
                            padding: 15px;
                            margin-bottom: 20px;
                            border-radius: 5px;
                        }
                        .info-box h3 {
                            margin-top: 0;
                            color: #1976D2;
                        }
                        .info-box ul {
                            margin: 10px 0;
                            padding-left: 20px;
                        }
                        .info-box li {
                            margin: 5px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🔍 JWT 토큰 검증 테스트</h1>
                        <p class="subtitle">401 에러 원인을 파악하기 위한 디버그 도구</p>
                        
                        <div class="info-box">
                            <h3>📝 사용 방법</h3>
                            <ul>
                                <li>1. 카카오 로그인 후 받은 <strong>accessToken</strong>을 아래에 붙여넣기</li>
                                <li>2. "토큰 검증하기" 버튼 클릭</li>
                                <li>3. 결과 확인:
                                    <ul>
                                        <li><strong>isValid: true</strong> → 토큰이 유효함 (다른 문제 있음)</li>
                                        <li><strong>isValid: false</strong> → 토큰 문제 (만료, 서명 오류 등)</li>
                                    </ul>
                                </li>
                            </ul>
                        </div>
                        
                        <div class="input-group">
                            <label for="token">JWT Access Token:</label>
                            <textarea 
                                id="token" 
                                placeholder="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IlJPTEVfVVNFUiIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSIsImlhdCI6MTcwODU5Mjg2MCwiZXhwIjoxNzA4NTk2NDYwfQ...."
                            ></textarea>
                        </div>
                        
                        <button onclick="verifyToken()">🔍 토큰 검증하기</button>
                        
                        <div class="loading" id="loading">
                            <p>검증 중...</p>
                        </div>
                        
                        <div class="result" id="result"></div>
                    </div>
                    
                    <script>
                        async function verifyToken() {
                            const token = document.getElementById('token').value.trim();
                            const resultDiv = document.getElementById('result');
                            const loadingDiv = document.getElementById('loading');
                            
                            if (!token) {
                                resultDiv.className = 'result error';
                                resultDiv.innerHTML = '<div class="result-title">❌ 에러</div><div class="result-content">토큰을 입력해주세요!</div>';
                                return;
                            }
                            
                            // 로딩 표시
                            loadingDiv.style.display = 'block';
                            resultDiv.style.display = 'none';
                            
                            try {
                                const response = await fetch('/api/v1/debug/token', {
                                    method: 'GET',
                                    headers: {
                                        'Authorization': 'Bearer ' + token
                                    }
                                });
                                
                                const data = await response.json();
                                
                                loadingDiv.style.display = 'none';
                                
                                if (data.success && data.data) {
                                    const isValid = data.data.isValid;
                                    
                                    if (isValid) {
                                        resultDiv.className = 'result success';
                                        resultDiv.innerHTML = `
                                            <div class="result-title">✅ 토큰 검증 성공!</div>
                                            <div class="result-content">${JSON.stringify(data.data, null, 2)}</div>
                                            <p style="margin-top: 15px; color: #155724;">
                                                <strong>결론:</strong> 토큰은 유효합니다. 401 에러는 다른 원인입니다.<br>
                                                → 백엔드 개발자에게 이 결과를 공유해주세요.
                                            </p>
                                        `;
                                    } else {
                                        resultDiv.className = 'result error';
                                        resultDiv.innerHTML = `
                                            <div class="result-title">❌ 토큰 검증 실패</div>
                                            <div class="result-content">${JSON.stringify(data.data, null, 2)}</div>
                                            <p style="margin-top: 15px; color: #721c24;">
                                                <strong>원인:</strong> ${data.data.error || '토큰이 유효하지 않습니다'}<br>
                                                → 백엔드 개발자에게 서버 로그 확인 요청하세요.
                                            </p>
                                        `;
                                    }
                                } else {
                                    resultDiv.className = 'result error';
                                    resultDiv.innerHTML = `
                                        <div class="result-title">❌ 응답 오류</div>
                                        <div class="result-content">${JSON.stringify(data, null, 2)}</div>
                                    `;
                                }
                            } catch (error) {
                                loadingDiv.style.display = 'none';
                                resultDiv.className = 'result error';
                                resultDiv.innerHTML = `
                                    <div class="result-title">❌ 네트워크 에러</div>
                                    <div class="result-content">${error.message}</div>
                                `;
                            }
                        }
                        
                        // Enter 키로도 검증 가능
                        document.getElementById('token').addEventListener('keypress', function(e) {
                            if (e.key === 'Enter' && e.ctrlKey) {
                                verifyToken();
                            }
                        });
                    </script>
                </body>
                </html>
                """;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}

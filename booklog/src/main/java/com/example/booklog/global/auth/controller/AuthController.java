package com.example.booklog.global.auth.controller;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.security.CustomUserDetails;
import com.example.booklog.global.auth.service.AuthCommandService;
import com.example.booklog.global.auth.service.AuthQueryService;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
 // import jakarta.servlet.http.HttpServletResponse; // 카카오 소셜 로그인 주석처리로 미사용
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
// import org.springframework.beans.factory.annotation.Value; // 카카오 소셜 로그인 주석처리로 미사용
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// import java.io.IOException; // 카카오 소셜 로그인 주석처리로 미사용

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

    // 카카오 소셜 로그인 주석처리로 미사용
    // @Value("${SERVER_DOMAIN:http://localhost:8080}")
    // private String serverDomain;

    // 회원가입
    @PostMapping("/sign-up")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    public ApiResponse<AuthResDTO.JoinDTO> signUp(
            @RequestBody @Valid AuthReqDTO.JoinDTO dto
    ){
        return ApiResponse.onSuccess(
                AuthSuccessCode.SIGNUP_SUCCESS,
                authCommandService.signup(dto)
        );
    }

    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 액세스 토큰과 리프레시 토큰을 발급받습니다.")
    public ApiResponse<AuthResDTO.LoginDTO> login(
            @RequestBody @Valid AuthReqDTO.LoginDTO dto
    ){
        return ApiResponse.onSuccess(
                AuthSuccessCode.LOGIN_SUCCESS,
                authQueryService.login(dto)
        );
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.")
    public ApiResponse<AuthResDTO.LoginDTO> refreshToken(
            @RequestBody @Valid AuthReqDTO.RefreshTokenDTO dto
    ){
        return ApiResponse.onSuccess(
                AuthSuccessCode.LOGIN_SUCCESS,
                authQueryService.refreshToken(dto)
        );
    }

    // ⚠️ 카카오 소셜 로그인 API - 에러로 인해 주석처리
    /*
    @PostMapping("/kakao/login")
    @Operation(
            summary = "[사용 금지] 카카오 소셜 로그인 테스트",
            description = """
                    ⚠️ **프론트엔드는 이 API를 사용하지 마세요!**
                    
                    이 API는 Swagger 테스트 전용입니다.
                    
                    **올바른 사용 방법:**
                    1. `GET /api/v1/auth/kakao/redirect` 호출
                    2. 카카오 로그인 진행
                    3. 콜백 URL에서 `accessToken`과 `refreshToken` 파싱
                    4. **바로 localStorage에 저장하고 사용**
                    
                    **이 API를 사용하면 안 되는 이유:**
                    - 콜백으로 이미 유효한 JWT 토큰을 받았습니다
                    - 다시 검증할 필요가 없습니다
                    - 401 에러만 발생합니다
                    
                    **Swagger 테스트용:**
                    - 콜백으로 받은 JWT 토큰을 여기에 넣으면 토큰이 재발급됩니다
                    """
    )
    public ApiResponse<AuthResDTO.LoginDTO> kakaoLogin(
            @RequestBody @Valid AuthReqDTO.KakaoLoginDTO dto
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.LOGIN_SUCCESS,
                authQueryService.kakaoLogin(dto)
        );
    }

    // 카카오 OAuth2 리다이렉트 (프론트엔드용)
    @GetMapping("/kakao/redirect")
    @Operation(
            summary = "카카오 OAuth2 리다이렉트 (프론트엔드용)",
            description = "카카오 로그인 페이지로 리다이렉트합니다. 프론트엔드에서 window.location.href로 호출하세요."
    )
    public void kakaoRedirect(HttpServletResponse response) throws IOException {
        String redirectUrl = serverDomain + "/oauth2/authorization/kakao";
        response.sendRedirect(redirectUrl);
    }
    */

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하여 로그아웃합니다. 인앱 로그인과 소셜 로그인 모두 사용 가능합니다.")
    public ApiResponse<AuthResDTO.LogoutDTO> logout(
            @RequestBody @Valid AuthReqDTO.LogoutDTO dto
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.LOGOUT_SUCCESS,
                authCommandService.logout(dto)
        );
    }

    // 회원탈퇴
    @DeleteMapping("/account")
    @Operation(
            summary = "회원탈퇴",
            description = "사용자 계정을 삭제합니다. 인앱 로그인 사용자는 비밀번호 검증이 필요하며, 소셜 로그인 사용자는 비밀번호 검증 없이 탈퇴가 가능합니다. 모든 사용자 데이터가 영구적으로 삭제됩니다."
    )
    public ApiResponse<AuthResDTO.DeleteAccountDTO> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AuthReqDTO.DeleteAccountDTO dto
    ) {
        return ApiResponse.onSuccess(
                AuthSuccessCode.DELETE_ACCOUNT_SUCCESS,
                authCommandService.deleteAccount(userDetails.getUserId(), dto)
        );
    }

}

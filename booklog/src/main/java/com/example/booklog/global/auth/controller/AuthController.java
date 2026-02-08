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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

    @Value("${SERVER_DOMAIN:http://localhost:8080}")
    private String serverDomain;

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

    // 카카오 소셜 로그인 (OAuth2 리다이렉트)
    @GetMapping("/kakao/login")
    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2 로그인 페이지로 리다이렉트합니다.")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        String redirectUrl = serverDomain + "/oauth2/authorization/kakao";
        response.sendRedirect(redirectUrl);
    }

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

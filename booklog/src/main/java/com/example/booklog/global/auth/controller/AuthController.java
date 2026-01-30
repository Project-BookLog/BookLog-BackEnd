package com.example.booklog.global.auth.controller;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.service.AuthCommandService;
import com.example.booklog.global.auth.service.AuthQueryService;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

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

}

package com.example.booklog.global.auth.controller;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.auth.service.AuthCommandService;
import com.example.booklog.global.auth.service.AuthQueryService;
import com.example.booklog.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

    // 회원가입
    @PostMapping("/sign-up")
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
    public ApiResponse<AuthResDTO.LoginDTO> login(
            @RequestBody @Valid AuthReqDTO.LoginDTO dto
    ){
        return ApiResponse.onSuccess(
                AuthSuccessCode.LOGIN_SUCCESS,
                authQueryService.login(dto)
        );
    }

}

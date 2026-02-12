package com.example.booklog.global.auth.service;

import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import jakarta.validation.Valid;

public interface AuthQueryService {

    AuthResDTO.LoginDTO login(AuthReqDTO.@Valid LoginDTO dto);

    AuthResDTO.LoginDTO refreshToken(AuthReqDTO.@Valid RefreshTokenDTO dto);

    // 카카오 소셜 로그인 - 에러로 인해 주석처리
    // AuthResDTO.LoginDTO kakaoLogin(AuthReqDTO.@Valid KakaoLoginDTO dto);
}

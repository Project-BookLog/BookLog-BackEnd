package com.example.booklog.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthReqDTO {

    // 로그인
    @Schema(name = "AuthLoginRequest")
    public record LoginDTO(
            @NotBlank
            String email,
            @NotBlank
            String password
    ) {}

    // 회원가입
    @Schema(name = "AuthJoinRequest")
    public record JoinDTO(
            @NotBlank
            String name,
            @NotBlank
            String email,
            @NotBlank
            String password
    ) {}

    // 토큰 갱신
    @Schema(name = "AuthRefreshToeknRequest")
    public record RefreshTokenDTO(
            @NotBlank
            String refreshToken
    ) {}
}

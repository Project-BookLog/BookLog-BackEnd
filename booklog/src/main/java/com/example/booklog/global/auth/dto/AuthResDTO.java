package com.example.booklog.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AuthResDTO {

    // 회원가입
    @Builder
    @Schema(name = "AuthJoinResponse", description = "회원가입 응답")
    public record JoinDTO(

            String email,

            String message
    ){}

    // 로그인
    @Builder
    @Schema(name = "AuthLoginResponse", description = "로그인 응답")
    public record LoginDTO(

            String accessToken,

            String refreshToken,

            String tokenType,

            Long expiresIn
    ){}
}

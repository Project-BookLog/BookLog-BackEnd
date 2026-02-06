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

    // 로그아웃
    @Builder
    @Schema(name = "AuthLogoutResponse", description = "로그아웃 응답")
    public record LogoutDTO(

            String message
    ){}

    // 회원탈퇴
    @Builder
    @Schema(name = "AuthDeleteAccountResponse", description = "회원탈퇴 응답")
    public record DeleteAccountDTO(

            String message
    ){}
}

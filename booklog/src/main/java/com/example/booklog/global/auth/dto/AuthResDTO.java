package com.example.booklog.global.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class AuthResDTO {

    // 회원가입
    @Builder
    public record JoinDTO(
            String email,
            String message
    ){}

    // 로그인
    @Builder
    public record LoginDTO(
            String accessToken,
            String tokenType,
            Long expiresIn
    ){}
}

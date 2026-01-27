package com.example.booklog.global.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class AuthResDTO {

    // 회원가입
    @Builder
    public record JoinDTO(
            Long userId,
            String email
    ){}

    // 로그인
    @Builder
    public record LoginDTO(
            Long userId,
            String accessToken
    ){}
}

package com.example.booklog.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthReqDTO {

    // 로그인
    public record LoginDTO(
            @NotBlank
            String email,
            @NotBlank
            String password
    ) {}

    // 회원가입
    public record JoinDTO(
            @NotBlank
            String name,
            @NotBlank
            String email,
            @NotBlank
            String password
    ) {}
}

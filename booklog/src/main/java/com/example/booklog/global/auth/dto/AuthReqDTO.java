package com.example.booklog.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthReqDTO {

    // 로그인
    @Schema(name = "AuthLoginRequest")
    public record LoginDTO(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            String password
    ) {}

    // 회원가입
    @Schema(name = "AuthJoinRequest")
    public record JoinDTO(
            @NotBlank(message = "이름은 필수 입력 항목입니다.")
            String name,

            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                    message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 최소 1개 이상 포함해야 합니다."
            )
            String password
    ) {}

    // 토큰 갱신
    @Schema(name = "AuthRefreshToeknRequest")
    public record RefreshTokenDTO(
            @NotBlank
            String refreshToken
    ) {}
}

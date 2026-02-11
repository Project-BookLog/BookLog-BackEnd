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

    // 로그아웃
    @Schema(name = "AuthLogoutRequest")
    public record LogoutDTO(
            @NotBlank(message = "리프레시 토큰을 입력해주세요.")
            String refreshToken
    ) {}

    // 회원탈퇴
    @Schema(name = "AuthDeleteAccountRequest", description = "회원탈퇴 요청")
    public record DeleteAccountDTO(
            @Schema(description = "비밀번호 (인앱 로그인 사용자만 필수, 소셜 로그인은 null 또는 빈 문자열 가능)", example = "Test1234!@")
            String password
    ) {}

    // 카카오 소셜 로그인 (테스트용)
    @Schema(name = "AuthKakaoLoginRequest", description = "카카오 소셜 로그인 테스트 요청")
    public record KakaoLoginDTO(
            @NotBlank(message = "카카오 액세스 토큰을 입력해주세요.")
            @Schema(description = "카카오 액세스 토큰", example = "kakao_access_token_here")
            String kakaoAccessToken
    ) {}
}

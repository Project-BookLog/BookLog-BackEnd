package com.example.booklog.global.auth.converter;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.UserStatus;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.enums.Role;

public class AuthConverter {

    // DTO, Encrypted Password, Role -> AuthAccounts + Users
    public static AuthAccounts toAuthAccount(
            AuthReqDTO.JoinDTO dto,
            String encryptedPassword,
            Role role
    ) {
        // Users 생성
        Users user = Users.builder()
                .nickname(dto.name())
                .status(UserStatus.ACTIVE)
                .build();

        // AuthAccounts 생성 (LOCAL provider)
        return AuthAccounts.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .providerId(dto.email())  // LOCAL은 email을 providerId로 사용
                .email(dto.email())
                .displayName(dto.name())
                .password(encryptedPassword)
                .role(role)
                .build();
    }

    // AuthAccounts, AccessToken, RefreshToken -> LoginDTO
    public static AuthResDTO.LoginDTO toLoginDTO(AuthAccounts account, String accessToken, String refreshToken, Long expiresIn) {
        return AuthResDTO.LoginDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    // AuthAccounts -> JoinDTO
    public static AuthResDTO.JoinDTO toJoinDTO(AuthAccounts account) {
        return AuthResDTO.JoinDTO.builder()
                .email(account.getEmail())
                .message("회원가입이 완료되었습니다. 로그인해주세요.")
                .build();
    }
}

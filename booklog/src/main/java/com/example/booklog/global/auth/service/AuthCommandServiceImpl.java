package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.converter.AuthConverter;
import com.example.booklog.global.auth.dto.AuthReqDTO;
import com.example.booklog.global.auth.dto.AuthResDTO;
import com.example.booklog.global.auth.enums.Role;
import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandServiceImpl implements AuthCommandService {

    private final PasswordEncoder passwordEncoder;
    private final AuthAccountsRepository authAccountsRepository;

    // 회원가입
    @Override
    @Transactional
    public AuthResDTO.JoinDTO signup(AuthReqDTO.JoinDTO dto) {
        // 1. 이메일 중복 체크 (LOCAL provider)
        if (authAccountsRepository.findByEmailAndProvider(dto.email(), AuthProvider.LOCAL).isPresent()) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(dto.password());

        // 3. AuthAccounts + Users 생성
        AuthAccounts account = AuthConverter.toAuthAccount(dto, encryptedPassword, Role.ROLE_USER);

        // 4. DB 저장 (cascade로 Users도 함께 저장됨)
        AuthAccounts savedAccount = authAccountsRepository.save(account);

        // 5. 응답 DTO 반환
        return AuthConverter.toJoinDTO(savedAccount);
    }
}

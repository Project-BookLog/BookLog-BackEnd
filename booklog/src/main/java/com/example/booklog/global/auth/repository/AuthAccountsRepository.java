package com.example.booklog.global.auth.repository;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthAccountsRepository extends JpaRepository<AuthAccounts, Long> {

    // 이메일과 provider로 조회 (로그인용)
    Optional<AuthAccounts> findByEmailAndProvider(String email, AuthProvider provider);

    // 이메일로 조회 (중복 체크용)
    Optional<AuthAccounts> findByEmail(String email);

    // user로 모든 인증 수단 조회
    List<AuthAccounts> findByUser(Users user);

    // provider_id와 provider로 조회 (OAuth용)
    Optional<AuthAccounts> findByProviderIdAndProvider(String providerId, AuthProvider provider);
}

package com.example.booklog.global.auth.repository;

import com.example.booklog.global.auth.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByEmail(String email);

    @Modifying
    @Transactional
    void deleteAllByEmail(String email);
}

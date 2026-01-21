package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
/*
    // (선택) 이메일로 조회 - 로그인/중복체크 등에 사용
    Optional<Users> findByEmail(String email);

    // (선택) 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    // (선택) 이메일 중복 체크
    boolean existsByEmail(String email);

 */
}

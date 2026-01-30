package com.example.booklog.domain.onboarding.repository;

import com.example.booklog.domain.onboarding.entity.UserOnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOnboardingStatusRepository extends JpaRepository<UserOnboardingStatus, Long> {

    Optional<UserOnboardingStatus> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}


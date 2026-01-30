package com.example.booklog.domain.onboarding.repository;

import com.example.booklog.domain.onboarding.entity.UserReadingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReadingProfileRepository extends JpaRepository<UserReadingProfile, Long> {

    Optional<UserReadingProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}


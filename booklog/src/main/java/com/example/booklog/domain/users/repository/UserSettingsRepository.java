package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {}
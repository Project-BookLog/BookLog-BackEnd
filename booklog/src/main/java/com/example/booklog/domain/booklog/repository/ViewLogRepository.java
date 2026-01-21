package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {
}

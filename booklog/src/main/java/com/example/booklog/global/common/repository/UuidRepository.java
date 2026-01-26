package com.example.booklog.global.common.repository;

import com.example.booklog.global.common.Uuid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UuidRepository extends JpaRepository<Uuid, Long> {
}

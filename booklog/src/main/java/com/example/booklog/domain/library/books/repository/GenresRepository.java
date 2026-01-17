package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.common.Genres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenresRepository extends JpaRepository<Genres, Long> {
    Optional<Genres> findByName(String name);
}

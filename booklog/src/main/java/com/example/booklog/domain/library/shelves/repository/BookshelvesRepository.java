package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookshelvesRepository extends JpaRepository<Bookshelves, Long> {}

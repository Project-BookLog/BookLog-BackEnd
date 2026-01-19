package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.Books;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface BooksRepository extends JpaRepository<Books, Long> {
    Optional<Books> findByIsbn13(String isbn13);
    Optional<Books> findByDetailUrl(String detailUrl);
}

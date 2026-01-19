package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.AuthorAwards;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRewardRepository extends JpaRepository<AuthorAwards, Long> {
    List<AuthorAwards> findAllByAuthor_Id(Long authorId);
    void deleteAllByAuthor_Id(Long authorId);
}

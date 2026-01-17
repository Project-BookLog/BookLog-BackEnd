package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.common.AuthorRewards;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRewardRepository extends JpaRepository<AuthorRewards, Long> {
    List<AuthorRewards> findAllByAuthor_AuthorId(Long authorId);
    void deleteAllByAuthor_AuthorId(Long authorId);
}

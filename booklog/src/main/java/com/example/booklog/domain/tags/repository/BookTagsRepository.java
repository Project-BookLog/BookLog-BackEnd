package com.example.booklog.domain.tags.mapping;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookTagsRepository extends JpaRepository<BookTags, BookTags.BookTagId> {

    @Query("select bt from BookTags bt join fetch bt.tag where bt.book.id = :bookId")
    List<BookTags> findAllByBookId(@Param("bookId") Long bookId);
}

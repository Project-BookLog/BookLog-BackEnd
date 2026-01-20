package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.Authors;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorsRepository extends JpaRepository<Authors, Long> {

    Optional<Authors> findByName(String name);

    // ✅ wikidata_id가 String이므로 String으로 변경
    Optional<Authors> findByWikidataId(String wikidataId);
}

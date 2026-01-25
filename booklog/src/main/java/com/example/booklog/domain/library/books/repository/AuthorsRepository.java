package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.Authors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorsRepository extends JpaRepository<Authors, Long> {

    Optional<Authors> findByName(String name);

    // ✅ wikidata_id가 String이므로 String으로 변경
    Optional<Authors> findByWikidataId(String wikidataId);

    /**
     * 작가명으로 검색 (부분 일치)
     * N+1 문제 방지를 위해 Service에서 별도로 책 정보를 조회
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 작가 페이지
     */
    @Query("SELECT DISTINCT a FROM Authors a " +
           "WHERE a.name LIKE %:keyword%")
    Page<Authors> searchByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 작가명으로 검색 (총 개수)
     *
     * @param keyword 검색 키워드
     * @return 검색된 작가 총 개수
     */
    long countByNameContaining(String keyword);
}

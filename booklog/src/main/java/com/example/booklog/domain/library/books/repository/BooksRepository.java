package com.example.booklog.domain.library.books.repository;

import com.example.booklog.domain.library.books.entity.Books;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface BooksRepository extends JpaRepository<Books, Long> {
    Optional<Books> findByIsbn13(String isbn13);
    Optional<Books> findByIsbn10(String isbn10);
    Optional<Books> findByDetailUrl(String detailUrl);
    Optional<Books> findByTitle(String title);

    /**
     * 홈 화면용 title 일괄 조회
     * Fetch Join으로 BookAuthors와 Authors를 함께 조회
     */
    @Query("SELECT DISTINCT b FROM Books b " +
           "LEFT JOIN FETCH b.bookAuthors ba " +
           "LEFT JOIN FETCH ba.author a " +
           "WHERE b.title IN :titles")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Books> findAllByTitleIn(@Param("titles") List<String> titles);

    /**
     * 특정 작가의 도서 목록 조회 (최신순)
     * Fetch Join으로 BookAuthors와 Authors를 함께 조회하여 N+1 문제 방지
     *
     * QueryHint PASS_DISTINCT_THROUGH를 false로 설정하여
     * SQL에는 DISTINCT를 적용하지 않고 메모리에서만 중복 제거
     *
     * @param authorId 작가 ID
     * @return 도서 목록
     */
    @Query("SELECT DISTINCT b FROM Books b " +
           "JOIN FETCH b.bookAuthors ba " +
           "JOIN FETCH ba.author a " +
           "WHERE a.id = :authorId " +
           "ORDER BY b.publishedDate DESC NULLS LAST, b.id DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Books> findBooksByAuthorId(@Param("authorId") Long authorId);

    /**
     * 여러 작가의 도서를 한 번에 조회 (IN 쿼리 + Fetch Join)
     * Batch 조회로 N+1 문제 방지
     *
     * QueryHint PASS_DISTINCT_THROUGH를 false로 설정하여
     * SQL에는 DISTINCT를 적용하지 않고 메모리에서만 중복 제거
     *
     * @param authorIds 작가 ID 리스트
     * @return 도서 목록 (출판일 최신순)
     */
    @Query("SELECT DISTINCT b FROM Books b " +
           "LEFT JOIN FETCH b.bookAuthors ba " +
           "LEFT JOIN FETCH ba.author a " +
           "WHERE a.id IN :authorIds " +
           "ORDER BY b.publishedDate DESC NULLS LAST, b.id DESC")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Books> findBooksByAuthorIds(@Param("authorIds") List<Long> authorIds);

    /**
     * 태그가 없는 책 조회
     * LEFT JOIN으로 book_tags와 조인하여 book_tags가 null인 책만 조회
     *
     * @return 태그가 없는 책 목록
     */
    @Query("SELECT DISTINCT b FROM Books b " +
           "LEFT JOIN FETCH b.bookAuthors ba " +
           "LEFT JOIN FETCH ba.author a " +
           "WHERE NOT EXISTS (SELECT 1 FROM BookTags bt WHERE bt.book.id = b.id)")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Books> findBooksWithoutTags();

    /**
     * 특정 작가의 도서 개수 확인 (디버깅용)
     */
    @Query("SELECT COUNT(DISTINCT b) FROM Books b " +
           "JOIN b.bookAuthors ba " +
           "WHERE ba.author.id IN :authorIds")
    long countBooksByAuthorIds(@Param("authorIds") List<Long> authorIds);

    /**
     * 도서 제목으로 검색 (페이징 및 정렬 지원)
     *
     * [정렬 옵션]
     * - LATEST: publishedDate DESC, id DESC
     * - OLDEST: publishedDate ASC, id ASC
     * - TITLE: title ASC
     * - AUTHOR: 첫 번째 저자명 ASC (BookAuthors.authorOrder = 1)
     *
     * @param keyword 검색 키워드 (제목 LIKE 검색)
     * @param pageable 페이징 및 정렬 정보
     * @return 도서 페이지
     */
    @Query("""
        SELECT DISTINCT b FROM Books b
        LEFT JOIN FETCH b.bookAuthors ba
        LEFT JOIN FETCH ba.author a
        WHERE b.title LIKE %:keyword%
        """)
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<Books> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 도서 제목 또는 저자명으로 검색 (페이징 및 정렬 지원)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 및 정렬 정보
     * @return 도서 페이지
     */
    @Query("""
        SELECT DISTINCT b FROM Books b
        LEFT JOIN FETCH b.bookAuthors ba
        LEFT JOIN FETCH ba.author a
        WHERE b.title LIKE %:keyword%
        OR a.name LIKE %:keyword%
        """)
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<Books> searchByTitleOrAuthor(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 책 ID로 상세정보 조회 (저자 정보 포함)
     * Fetch Join으로 BookAuthors와 Authors를 함께 조회하여 N+1 문제 방지
     *
     * @param bookId 책 ID
     * @return 책 엔티티 (저자 정보 포함)
     */
    @Query("SELECT DISTINCT b FROM Books b " +
           "LEFT JOIN FETCH b.bookAuthors ba " +
           "LEFT JOIN FETCH ba.author a " +
           "WHERE b.id = :bookId")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Optional<Books> findByIdWithAuthors(@Param("bookId") Long bookId);

    /**
     * ✅ (괄호 제거 + 공백 제거) 정규화 title IN 조회
     * - "언어의 온도(170만부 기념 에디션)" -> "언어의온도"
     * - DB의 title도 동일하게 정규화해서 IN 비교
     *
     * MySQL 8+ (REGEXP_REPLACE 지원) 기준
     */
    @Query(value = """
        SELECT b.*
        FROM books b
        WHERE REPLACE(
                REPLACE(
                    REGEXP_REPLACE(b.title, '\\\\([^\\\\)]*\\\\)', ''),
                ' ', ''),
            '\\t', ''
        ) IN (:normTitles)
        """, nativeQuery = true)
    List<Books> findAllByNormalizedTitleIn(@Param("normTitles") List<String> normTitles);

}



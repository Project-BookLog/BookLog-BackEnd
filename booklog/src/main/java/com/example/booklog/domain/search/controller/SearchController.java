package com.example.booklog.domain.search.controller;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.search.dto.AuthorSearchResponse;
import com.example.booklog.domain.search.service.AuthorSearchService;
import com.example.booklog.domain.search.service.BookSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 검색 API 컨트롤러
 * - 도서 검색: /api/v1/search/books
 * - 작가 검색: /api/v1/search/authors
 * - 통합 검색: /api/v1/search (추후 구현)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final BookSearchService bookSearchService;
    private final AuthorSearchService authorSearchService;

    /**
     * 도서 검색
     * GET /api/v1/search/books?query={검색어}&page={페이지}&size={크기}
     */
    @GetMapping("/books")
    public BookSearchResponse searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookSearchService.searchBooks(query, page, size);
    }

    /**
     * 작가 검색
     * GET /api/v1/search/authors?query={검색어}&page={페이지}&size={크기}
     *
     * @param query 검색어 (작가명)
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 10)
     * @return 작가 검색 결과 (작가 기본 정보 + 대표작 최대 2권)
     */
    @GetMapping("/authors")
    public AuthorSearchResponse searchAuthors(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return authorSearchService.searchAuthors(query, page, size);
    }

    /**
     * 통합 검색 (추후 구현)
     * GET /api/v1/search?query={검색어}&page={페이지}&size={크기}
     */
    // TODO: 통합 검색 구현
    // @GetMapping
    // public IntegratedSearchResponse search(...)

    /**
     * 최근 검색어 조회 (추후 구현)
     * GET /api/v1/search/recent
     */
    // TODO: 최근 검색어 구현
    // @GetMapping("/recent")
    // public RecentSearchResponse getRecentSearches()

    /**
     * 추천 검색어 조회 (추후 구현)
     * GET /api/v1/search/recommendations
     */
    // TODO: 추천 검색어 구현
    // @GetMapping("/recommendations")
    // public RecommendationSearchResponse getRecommendations()
}


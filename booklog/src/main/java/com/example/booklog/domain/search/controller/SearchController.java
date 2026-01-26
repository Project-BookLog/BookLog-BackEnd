package com.example.booklog.domain.search.controller;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.search.dto.*;
import com.example.booklog.domain.search.service.AuthorSearchService;
import com.example.booklog.domain.search.service.BookSearchService;
import com.example.booklog.domain.search.service.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 검색 API 컨트롤러
 * - 도서 검색: /api/v1/search/books
 * - 작가 검색: /api/v1/search/authors
 * - 검색어 저장: /api/v1/search/keywords (POST)
 * - 최근 검색어 조회: /api/v1/search/recent (GET)
 * - 추천 검색어 조회: /api/v1/search/recommendations (GET)
 * - 통합 검색: /api/v1/search (추후 구현)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final BookSearchService bookSearchService;
    private final AuthorSearchService authorSearchService;
    private final SearchKeywordService searchKeywordService;

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
     * 검색어 저장 API
     * POST /api/v1/search/keywords
     *
     * [호출 시점]
     * - 검색 실행 시
     * - 추천 검색어 클릭 시
     * - 최근 검색어 클릭 시
     *
     * [동작 방식]
     * - 동일 검색어가 이미 존재하면 삭제 후 재생성 (최신순 유지)
     * - 최대 10개 제한, 초과 시 가장 오래된 검색어 삭제
     *
     * @param userId 사용자 ID (임시로 @RequestParam 사용, 추후 인증 적용 시 @AuthenticationPrincipal 등으로 변경)
     * @param request 검색어 저장 요청
     */
    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveSearchKeyword(
            @RequestParam Long userId, // TODO: 인증 구현 후 변경 필요
            @RequestBody SearchKeywordSaveRequest request
    ) {
        searchKeywordService.saveSearchKeyword(userId, request.getKeyword());
    }

    /**
     * 최근 검색어 조회 API
     * GET /api/v1/search/recent
     *
     * [호출 시점]
     * - 검색 화면 진입 시 (검색창이 비어 있을 때)
     *
     * [응답 데이터]
     * - 사용자가 이전에 검색 실행을 통해 입력한 검색어 목록
     * - 최신순 정렬
     * - 최대 10개
     *
     * @param userId 사용자 ID (임시로 @RequestParam 사용, 추후 인증 적용 시 변경)
     * @return 최근 검색어 목록
     */
    @GetMapping("/recent")
    public RecentSearchResponse getRecentSearches(
            @RequestParam Long userId // TODO: 인증 구현 후 변경 필요
    ) {
        return searchKeywordService.getRecentSearches(userId);
    }

    /**
     * 추천 검색어 조회 API
     * GET /api/v1/search/recommendations
     *
     * [호출 시점]
     * - 검색 화면 진입 시 (검색창이 비어 있을 때)
     *
     * [응답 데이터]
     * - 운영자가 관리하는 추천 검색어 목록
     * - 우선순위 순으로 정렬
     * - 로그인 여부와 무관하게 사용 가능
     *
     * @return 추천 검색어 목록
     */
    @GetMapping("/recommendations")
    public RecommendationSearchResponse getRecommendations() {
        return searchKeywordService.getRecommendations();
    }

    /**
     * 통합 검색 (추후 구현)
     * GET /api/v1/search?query={검색어}&page={페이지}&size={크기}
     */
    // TODO: 통합 검색 구현
    // @GetMapping
    // public IntegratedSearchResponse search(...)
}


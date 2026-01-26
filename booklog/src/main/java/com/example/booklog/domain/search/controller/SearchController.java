package com.example.booklog.domain.search.controller;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.search.dto.*;
import com.example.booklog.domain.search.service.AuthorSearchService;
import com.example.booklog.domain.search.service.BookSearchService;
import com.example.booklog.domain.search.service.IntegratedSearchService;
import com.example.booklog.domain.search.service.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 통합 검색 API 컨트롤러
 * - 통합 검색: /api/v1/search (GET)
 * - 도서 검색: /api/v1/search/books
 * - 작가 검색: /api/v1/search/authors
 * - 검색어 저장: /api/v1/search/keywords (POST)
 * - 최근 검색어 조회: /api/v1/search/recent (GET)
 * - 추천 검색어 조회: /api/v1/search/recommendations (GET)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final IntegratedSearchService integratedSearchService;
    private final BookSearchService bookSearchService;
    private final AuthorSearchService authorSearchService;
    private final SearchKeywordService searchKeywordService;

    /**
     * 도서 검색
     * GET /api/v1/search/books?query={검색어}&page={페이지}&size={크기}&sort={정렬}
     *
     * [정렬 옵션]
     * - latest: 최신순 (출판일 내림차순) - 기본값
     * - oldest: 오래된순 (출판일 오름차순)
     * - title: 제목순 (가나다순)
     * - author: 저자순 (첫 번째 저자 기준 가나다순)
     *
     * @param query 검색어 (필수)
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (기본값: latest)
     * @return 도서 검색 결과
     */
    @GetMapping("/books")
    public BookSearchResponse searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        BookSortType sortType = BookSortType.from(sort);
        return bookSearchService.searchBooks(query, page, size, sortType);
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
     * 통합 검색 API
     * GET /api/v1/search?query={검색어}&sort={정렬기준}
     *
     * [기능 설명]
     * - 검색어에 대해 "작가"와 "도서"를 통합하여 조회
     * - 전체 탭에서 사용되며, 각 영역별로 제한된 개수(5개)만 표시
     * - 더 많은 결과를 보려면 개별 탭(/books, /authors)으로 이동
     *
     * [응답 구조]
     * - authors: 작가 검색 결과 (요약 정보, 최대 5명)
     * - books: 도서 검색 결과 (기본 정보, 최대 5권)
     * - 각 영역의 totalCount로 전체 개수 파악 가능
     *
     * [캐시 정책]
     * - Cache-Control: max-age=60 (60초)
     * - 동일한 query + sort 조합에 대해 클라이언트 캐시 활용
     *
     * [경계 케이스]
     * - 검색어가 없거나 공백인 경우: 400 Bad Request
     * - 검색 결과가 없는 경우: 빈 배열과 totalCount=0 반환
     * - 정렬 기준이 유효하지 않은 경우: 400 Bad Request
     *
     * @param query 검색어 (필수, 1~100자)
     * @param sort 정렬 기준 (선택, latest|popular, 기본값: latest)
     * @return 통합 검색 응답 (작가 + 도서)
     */
    @GetMapping
    public ResponseEntity<IntegratedSearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        IntegratedSearchResponse response = integratedSearchService.search(query, sort);

        // 캐시 헤더 설정 (60초)
        CacheControl cacheControl = CacheControl.maxAge(60, TimeUnit.SECONDS)
                .cachePublic();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(response);
    }
}


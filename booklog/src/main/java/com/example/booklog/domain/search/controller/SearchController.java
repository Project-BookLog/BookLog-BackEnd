package com.example.booklog.domain.search.controller;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.search.dto.*;
import com.example.booklog.domain.search.service.AuthorSearchService;
import com.example.booklog.domain.search.service.BookSearchService;
import com.example.booklog.domain.search.service.IntegratedSearchService;
import com.example.booklog.domain.search.service.SearchKeywordService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 통합 검색 API 컨트롤러
 * - 통합 검색: /api/v1/search (GET)
 * - 도서 검색: /api/v1/search/books
 * - 작가 검색: /api/v1/search/authors
 * - 검색어 저장: /api/v1/search/keywords (POST)
 * - 최근 검색어 조회: /api/v1/search/recent (GET)
 * - 최근 검색어 단건 삭제: /api/v1/search/recent (DELETE)
 * - 최근 검색어 전체 삭제: /api/v1/search/recent/all (DELETE)
 * - 추천 검색어 조회: /api/v1/search/recommendations (GET)
 */
@Tag(name = "Search", description = "검색 API")
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
            @RequestParam(required = false) String query,
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
    @Operation(
            summary = "작가 검색",
            description = """
                    작가 이름으로 검색하여 작가 정보와 대표작을 조회합니다.
                    
                    **동작 방식:**
                    1. DB에서 작가 검색 (이름 기준)
                    2. 검색 결과 없거나 도서 데이터 없으면 카카오 API에서 자동 임포트
                    3. 각 작가별 대표작 최대 2권 포함
                    4. 페이지네이션 지원
                    
                    **검색 결과 없을 때:**
                    - 빈 배열 반환 (200 OK)
                    - 임포트 실패 시에도 500 에러 대신 빈 배열 반환
                    
                    **요청 방법:**
                    - GET 요청 (Query Parameter 사용)
                    - Body에 JSON 넣지 않음!
                    
                    **예시:**
                    ```
                    GET /api/v1/search/authors?query=김영하&page=1&size=10
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공 (결과 있음 또는 빈 배열)",
                    content = @Content(
                            schema = @Schema(implementation = AuthorSearchResponse.class),
                            examples = @ExampleObject(
                                    name = "검색 성공 예시",
                                    value = """
                                            {
                                              "page": 1,
                                              "size": 10,
                                              "isEnd": true,
                                              "totalCount": 1,
                                              "items": [
                                                {
                                                  "authorId": 123,
                                                  "name": "김영하",
                                                  "profileImageUrl": "https://example.com/profile.jpg",
                                                  "occupation": "소설가",
                                                  "nationality": "대한민국",
                                                  "biography": "작가 소개...",
                                                  "books": [
                                                    {
                                                      "bookId": 456,
                                                      "title": "살인자의 기억법",
                                                      "thumbnail": "https://example.com/book.jpg",
                                                      "isbn13": "9788936433598"
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검색어 길이 초과, 페이지 번호/크기 유효하지 않음)",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "SRCH002",
                                              "message": "검색어는 100자 이내로 입력해주세요."
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/authors")
    public AuthorSearchResponse searchAuthors(
            @Parameter(
                    description = "검색할 작가 이름 (선택, 최대 100자)",
                    example = "김영하"
            )
            @RequestParam(required = false) String query,

            @Parameter(
                    description = "페이지 번호 (1부터 시작)",
                    example = "1"
            )
            @RequestParam(defaultValue = "1") int page,

            @Parameter(
                    description = "페이지 크기 (1-100)",
                    example = "10"
            )
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
     * @param userDetails 인증된 사용자 정보 (JWT 토큰에서 추출)
     * @param request 검색어 저장 요청
     */
    @PostMapping("/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveSearchKeyword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SearchKeywordSaveRequest request
    ) {
        searchKeywordService.saveSearchKeyword(userDetails.getUserId(), request.getKeyword());
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
     * @param userDetails 인증된 사용자 정보 (JWT 토큰에서 추출)
     * @return 최근 검색어 목록
     */
    @Operation(
            summary = "최근 검색어 조회",
            description = "사용자의 최근 검색어 목록을 조회합니다. 최신순으로 정렬되며 최대 10개까지 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecentSearchResponse.class))
            )
    })
    @GetMapping("/recent")
    public RecentSearchResponse getRecentSearches(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return searchKeywordService.getRecentSearches(userDetails.getUserId());
    }

    /**
     * 최근 검색어 단건 삭제 API
     * DELETE /api/v1/search/recent?keyword={검색어}
     *
     * [호출 시점]
     * - 최근 검색어 목록에서 특정 검색어의 X 버튼 클릭 시
     *
     * @param userDetails 인증된 사용자 정보 (JWT 토큰에서 추출)
     * @param keyword 삭제할 검색어
     */
    @Operation(
            summary = "최근 검색어 단건 삭제",
            description = "최근 검색어 목록에서 특정 검색어를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (검색어 누락)")
    })
    @DeleteMapping("/recent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSearchKeyword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 검색어", required = true)
            @RequestParam String keyword
    ) {
        searchKeywordService.deleteSearchKeyword(userDetails.getUserId(), keyword);
    }

    /**
     * 최근 검색어 전체 삭제 API
     * DELETE /api/v1/search/recent/all
     *
     * [호출 시점]
     * - 최근 검색어 목록에서 "전체 삭제" 버튼 클릭 시
     *
     * @param userDetails 인증된 사용자 정보 (JWT 토큰에서 추출)
     */
    @Operation(
            summary = "최근 검색어 전체 삭제",
            description = "사용자의 모든 최근 검색어를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/recent/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllSearchKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        searchKeywordService.deleteAllSearchKeywords(userDetails.getUserId());
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
            @RequestParam(required = false) String query,
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


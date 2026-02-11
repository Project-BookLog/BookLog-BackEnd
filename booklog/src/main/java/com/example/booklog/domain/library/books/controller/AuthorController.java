package com.example.booklog.domain.library.books.controller;

import com.example.booklog.domain.library.books.dto.AuthorDetailResponse;
import com.example.booklog.domain.library.books.service.AuthorQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 작가 API 컨트롤러
 *
 * - 작가 상세정보 조회: GET /api/v1/authors/{authorId}
 */
@Slf4j
@Tag(name = "Authors", description = "작가 정보 API")
@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorQueryService authorQueryService;

    /**
     * 작가 상세정보 조회 API
     * GET /api/v1/authors/{authorId}
     *
     * [기능 설명]
     * - 특정 작가의 상세 정보를 조회합니다
     * - 작가 이미지, 이름, 소개, 도서 목록, 프로필, 수상경력을 포함합니다
     * - DB에 작가 정보가 부족하면 카카오 API를 통해 보완하고, 그래도 부족하면 GPT를 이용합니다
     * - 인증 불필요 (공개 API)
     *
     * [응답 데이터]
     * - authorId: 작가 ID
     * - name: 작가명
     * - profileImageUrl: 작가 프로필 이미지 URL
     * - biography: 작가에 대한 한 줄 소개
     * - books: 해당 작가의 도서 요약 목록 (책 제목, 저자명, 출판사, 썸네일, 취향 정보)
     * - profile: 작가 프로필 (학력, 데뷔, 출생, 직업)
     * - awards: 수상경력 (연도별)
     *
     * [정렬 옵션]
     * - latest: 최신순 (기본값)
     * - oldest: 오래된순
     * - title: 제목순
     * - author: 저자순
     *
     * [에러 케이스]
     * - 404: 작가가 존재하지 않을 경우
     *
     * @param authorId 작가 ID
     * @param sortBy 정렬 기준 (latest, oldest, title, author)
     * @return 작가 상세정보
     */
    @Operation(
            summary = "작가 상세정보 조회",
            description = "특정 작가의 상세 정보를 조회합니다. 도서 목록, 프로필, 수상경력을 포함합니다. " +
                         "DB에 정보가 부족하면 카카오 API 및 GPT를 통해 자동으로 보완됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "작가 상세정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = AuthorDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "작가를 찾을 수 없음",
                    content = @Content
            )
    })
    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorDetailResponse> getAuthorDetail(
            @Parameter(description = "작가 ID", required = true, example = "1")
            @PathVariable Long authorId,

            @Parameter(description = "도서 정렬 기준 (latest: 최신순, oldest: 오래된순, title: 제목순, author: 저자순)",
                      example = "latest")
            @RequestParam(required = false, defaultValue = "latest") String sortBy
    ) {
        log.info("GET /api/v1/authors/{} - 작가 상세정보 조회 요청, sortBy: {}", authorId, sortBy);

        AuthorDetailResponse response = authorQueryService.getAuthorDetail(authorId, sortBy);

        // 캐시 헤더 설정 (10분)
        // 작가 정보는 자주 변경되지 않으므로 캐싱 적용
        CacheControl cacheControl = CacheControl.maxAge(10, TimeUnit.MINUTES)
                .cachePublic();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(response);
    }
}


package com.example.booklog.domain.library.books.controller;

import com.example.booklog.domain.library.books.dto.BookDetailResponse;
import com.example.booklog.domain.library.books.service.BookQueryService;
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
 * 책 API 컨트롤러
 *
 * - 책 상세정보 조회: GET /api/v1/books/{bookId}
 */
@Slf4j
@Tag(name = "Books", description = "책 정보 API")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookQueryService bookQueryService;

    /**
     * 책 상세정보 조회 API
     * GET /api/v1/books/{bookId}
     *
     * [기능 설명]
     * - 특정 책의 상세 정보를 조회합니다
     * - 책 제목, 설명, 출판사, ISBN, 저자 정보 등을 포함합니다
     * - 인증 불필요 (공개 API)
     *
     * [응답 데이터]
     * - bookId: 책 ID
     * - title: 책 제목
     * - description: 책 소개/설명
     * - thumbnailUrl: 썸네일 이미지 URL
     * - publisherName: 출판사명
     * - publishedDate: 출판일
     * - isbn, isbn10, isbn13: ISBN 정보
     * - detailUrl: 카카오 상세 URL
     * - authors: 저자 목록 (저자명, 역할, 프로필 이미지)
     *
     * [에러 케이스]
     * - 404: 책이 존재하지 않을 경우
     *
     * @param bookId 책 ID
     * @return 책 상세정보
     */
    @Operation(
            summary = "책 상세정보 조회",
            description = "특정 책의 상세 정보를 조회합니다. 저자 정보를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "책 상세정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = BookDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content
            )
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<BookDetailResponse> getBookDetail(
            @Parameter(description = "책 ID", required = true, example = "1")
            @PathVariable Long bookId
    ) {
        log.info("GET /api/v1/books/{} - 책 상세정보 조회 요청", bookId);

        BookDetailResponse response = bookQueryService.getBookDetail(bookId);

        // 캐시 헤더 설정 (5분)
        // 책 정보는 자주 변경되지 않으므로 캐싱 적용
        CacheControl cacheControl = CacheControl.maxAge(5, TimeUnit.MINUTES)
                .cachePublic();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(response);
    }
}

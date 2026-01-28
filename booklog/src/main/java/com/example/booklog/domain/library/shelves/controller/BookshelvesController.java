// =====================================
// [서재] BookshelvesController
// =====================================
package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.service.BookshelvesService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "서재(Shelves)", description = "서재 생성/조회(프리뷰3권)/수정/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shelves")
public class BookshelvesController {

    private final BookshelvesService bookshelvesService;

    @Operation(
            summary = "서재 생성",
            description = """
                    새로운 서재를 생성합니다.
                    - 인증: Access Token(Bearer)
                    - Body: name(필수), isPublic(선택), sortOrder(선택)
                    - 응답: shelfId
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @ApiResponse(responseCode = "409", description = "중복 서재명")
    })
    @PostMapping
    public BookshelfCreateResponse create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BookshelfCreateRequest req
    ) {
        return bookshelvesService.create(userDetails.getUserId(), req);
    }

    @Operation(
            summary = "서재 목록 조회(프리뷰 3권 포함)",
            description = """
                    내 서재 목록을 조회합니다.
                    - 인증: Access Token(Bearer)
                    - 응답: 서재 리스트 + 각 서재별 previewBooks(최대 3권)
                      - previewBooks: 썸네일, 제목, 저자명, 출판사(리스트 UI용)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public List<BookshelfListItemResponse> list(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return bookshelvesService.list(userDetails.getUserId());
    }

    @Operation(
            summary = "서재 수정(PATCH)",
            description = """
                    서재의 일부 필드를 변경합니다.
                    - 인증: Access Token(Bearer)
                    - Path: shelfId
                    - Body: name/isPublic/sortOrder 중 변경할 값만 전달
                    - 응답: 204 No Content
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "서재 없음 또는 내 서재 아님"),
            @ApiResponse(responseCode = "409", description = "중복 서재명")
    })
    @PatchMapping("/{shelfId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "shelfId") Long shelfId,
            @RequestBody BookshelfUpdateRequest req
    ) {
        bookshelvesService.update(userDetails.getUserId(), shelfId, req);
    }

    @Operation(
            summary = "서재 삭제(UNASSIGN 고정)",
            description = """
                    서재를 삭제합니다.
                    - 인증: Access Token(Bearer)
                    - Path: shelfId
                    - 정책(UNASSIGN):
                      - bookshelf_items(서재-책 매핑)만 제거 후 서재 삭제
                      - user_books(라이브러리)는 유지
                    - 응답: 204 No Content
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "서재 없음 또는 내 서재 아님")
    })
    @DeleteMapping("/{shelfId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "shelfId") Long shelfId
    ) {
        bookshelvesService.delete(userDetails.getUserId(), shelfId);
    }
}

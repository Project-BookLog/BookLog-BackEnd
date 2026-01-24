package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.service.UserBooksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "저장 도서(UserBooks)",
        description = "유저의 저장 도서(user_books) 및 서재 매핑(bookshelf_items) 관리 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-books")
public class UserBooksController {

    private final UserBooksService userBooksService;

    @Operation(
            summary = "책 저장(내 저장 도서로 추가)",
            description = """
                    book을 유저의 '저장 도서(user_books)'로 추가합니다.

                    - 헤더: X-USER-ID (필수)
                    - Body:
                      - bookId: 필수
                      - shelfId: 선택 (값이 있으면 해당 서재에 추가, A 방식)
                      - status: 선택 (기본 TO_READ)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(생성된 userBookId 반환)"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류/유효성 실패"),
            @ApiResponse(responseCode = "403", description = "내 서재가 아님"),
            @ApiResponse(responseCode = "404", description = "책/서재/유저 없음"),
            @ApiResponse(responseCode = "409", description = "이미 저장한 책(중복)")
    })
    @PostMapping
    public UserBookCreateResponse create(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader(name = "X-USER-ID") Long userId,

            @RequestBody @Valid UserBookCreateRequest req
    ) {
        return userBooksService.create(userId, req);
    }

    @Operation(
            summary = "저장 도서 목록 조회(전체)",
            description = """
                    유저가 저장한 도서를 전체 조회합니다. (페이지네이션 없음)

                    - 헤더: X-USER-ID (필수)
                    - Query:
                      - shelfId: 선택
                      - status: 선택 (TO_READ/READING/DONE/STOPPED)
                      - sort: 선택 (LATEST/OLDEST/TITLE/AUTHOR), 기본 LATEST
                    - 응답: List<UserBookListItemResponse>
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(리스트 반환)"),
            @ApiResponse(responseCode = "400", description = "정렬/상태 값이 허용 범위를 벗어남")
    })
    @GetMapping
    public UserBookListResponse list(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader(name = "X-USER-ID") Long userId,

            @Parameter(description = "서재 ID(선택)", example = "10")
            @RequestParam(name = "shelfId", required = false) Long shelfId,

            @Parameter(description = "상태(선택)", example = "READING")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(description = "정렬(선택). 기본 LATEST", example = "LATEST")
            @RequestParam(name = "sort", required = false, defaultValue = "LATEST") String sort
    ) {
        return userBooksService.listAll(userId, shelfId, status, sort);
    }

    @Operation(
            summary = "저장 도서 삭제(라이브러리 삭제 / 서재에서만 제거)",
            description = """
                    삭제 우선순위

                    1) Body.ids가 있으면: 선택 삭제(라이브러리에서 완전 삭제)
                    2) shelfId가 있으면: 해당 서재에서만 제거(라이브러리 유지)
                    3) status가 있으면: 상태별 전체 삭제(라이브러리에서 완전 삭제)
                    4) 모두 없으면: 전체 삭제(라이브러리 전체 삭제)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(삭제된 개수 반환)"),
            @ApiResponse(responseCode = "400", description = "파라미터 조합/값 오류"),
            @ApiResponse(responseCode = "403", description = "내 서재가 아님"),
            @ApiResponse(responseCode = "404", description = "서재 없음")
    })
    @DeleteMapping
    public int delete(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader(name = "X-USER-ID") Long userId,

            @RequestBody(required = false) DeleteBody body,

            @Parameter(description = "서재 ID(선택). 있으면 해당 서재에서만 제거", example = "10")
            @RequestParam(name = "shelfId", required = false) Long shelfId,

            @Parameter(description = "상태(선택). 있으면 해당 상태를 라이브러리에서 전체 삭제", example = "STOPPED")
            @RequestParam(name = "status", required = false) String status
    ) {
        List<Long> ids = (body == null) ? null : body.ids();
        return userBooksService.delete(userId, ids, shelfId, status);
    }

    @Schema(name = "DeleteBody", description = "저장 도서 삭제 요청 바디")
    public record DeleteBody(
            @ArraySchema(schema = @Schema(description = "선택 삭제할 userBookId 리스트(선택)", example = "[1,2,3]"))
            List<Long> ids
    ) {}

    @Operation(
            summary = "저장 도서 상세 조회",
            description = """
                    userBookId 기준으로 저장 도서 상세 정보를 조회합니다.

                    - 헤더: X-USER-ID (필수)
                    - Path: userBookId
                    - 응답: UserBookDetailResponse
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(상세 반환)"),
            @ApiResponse(responseCode = "404", description = "저장 도서 없음")
    })
    @GetMapping("/{userBookId}")
    public UserBookDetailResponse detail(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader(name = "X-USER-ID") Long userId,

            @Parameter(name = "userBookId", description = "저장 도서 ID(필수)", required = true, example = "100")
            @PathVariable(name = "userBookId") Long userBookId
    ) {
        return userBooksService.detail(userId, userBookId);
    }

    @Operation(
            summary = "도서 총 페이지 입력",
            description = """
                사용자가 직접 입력하는 총 페이지 수(pageCountSnapshot)를 저장합니다.
                - 책 메타(books)와 무관하게 user_books에 스냅샷으로 저장됩니다.
                - 입력 후 progressPercent는 currentPage 기준으로 재계산됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode="204", description="저장 성공", content=@Content),
            @ApiResponse(responseCode="400", description="요청값 오류", content=@Content),
            @ApiResponse(responseCode="404", description="저장 도서 없음/권한 없음", content=@Content)
    })
    @PatchMapping(value = "/{userBookId}/total-page", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveTotalPage(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(description = "저장 도서 ID(user_books.user_book_id)", required = true, example = "101")
            @PathVariable Long userBookId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "총 페이지 입력 요청",
                    content = @Content(
                            schema = @Schema(implementation = TotalPageSaveRequest.class),
                            examples = @ExampleObject(
                                    name = "총 페이지 입력 예시",
                                    value = "{ \"pageCountSnapshot\": 312 }"
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid TotalPageSaveRequest req
    ) {
        userBooksService.saveTotalPage(userId, userBookId, req);
    }


}

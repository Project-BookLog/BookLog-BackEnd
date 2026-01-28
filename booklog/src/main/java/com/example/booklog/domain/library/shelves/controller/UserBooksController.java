// =====================================
// [도서] UserBooksController
// =====================================
package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.ReadingStatus;
import com.example.booklog.domain.library.shelves.entity.UserBookSort;
import com.example.booklog.domain.library.shelves.service.UserBooksService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "도서(UserBooks)",
        description = "유저 저장 도서(user_books) 및 서재 매핑(bookshelf_items) 관리 API"
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
                    - 인증: Access Token(Bearer)
                    - Body:
                      - bookId: 필수
                      - shelfId: 선택 (값이 있으면 해당 서재에 추가, A 방식)
                      - status: 선택 (기본 TO_READ)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(생성된 userBookId 반환)"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류/유효성 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "내 서재가 아님"),
            @ApiResponse(responseCode = "404", description = "책/서재 없음"),
            @ApiResponse(responseCode = "409", description = "이미 저장한 책(중복)")
    })
    @PostMapping
    public UserBookCreateResponse create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserBookCreateRequest req
    ) {
        return userBooksService.create(userDetails.getUserId(), req);
    }

    @Operation(
            summary = "저장 도서 목록 조회",
            description = """
                    유저가 저장한 도서를 조회합니다.
                    - 인증: Access Token(Bearer)
                    - Query:
                      - shelfId: 선택
                      - status: 선택 (TO_READ/READING/DONE/STOPPED)
                      - sort: 선택 (LATEST/OLDEST/TITLE/AUTHOR), 기본 LATEST
                    - 응답: 리스트 UI용 요약 데이터
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "정렬/상태 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public UserBookListResponse list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "shelfId", required = false) Long shelfId,
            @RequestParam(name = "status", required = false) ReadingStatus status,
            @RequestParam(name = "sort", required = false, defaultValue = "LATEST") UserBookSort sort
    ) {
        return userBooksService.listAll(userDetails.getUserId(), shelfId, status, sort);
    }

    @Operation(
            summary = "저장 도서 삭제(선택/조건/전체)",
            description = """
                    삭제 우선순위
                    1) Body.ids가 있으면: 선택 삭제(라이브러리에서 완전 삭제) (ids: userBook의 id)
                    2) shelfId가 있으면: 해당 서재에서만 제거(라이브러리 유지)
                    3) status가 있으면: 상태별 전체 삭제(라이브러리에서 완전 삭제)
                    4) 모두 없으면: 전체 삭제(라이브러리 전체 삭제)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(삭제된 개수 반환)"),
            @ApiResponse(responseCode = "400", description = "파라미터 조합/값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "내 서재가 아님"),
            @ApiResponse(responseCode = "404", description = "서재 없음")
    })
    @DeleteMapping
    public int delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) DeleteBody body,
            @RequestParam(name = "shelfId", required = false) Long shelfId,
            @RequestParam(name = "status", required = false) ReadingStatus status
    ) {
        List<Long> ids = (body == null) ? null : body.ids();
        return userBooksService.delete(userDetails.getUserId(), ids, shelfId, status);
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
                    - 인증: Access Token(Bearer)
                    - Path: userBookId
                    - 응답: 상세 화면용 데이터(책 메타 + 내 상태 + 최근 로그 등)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "저장 도서 없음/권한 없음")
    })
    @GetMapping("/{userBookId}")
    public UserBookDetailResponse detail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "userBookId") Long userBookId
    ) {
        return userBooksService.detail(userDetails.getUserId(), userBookId);
    }

    @Operation(
            summary = "도서 총 페이지 입력",
            description = """
                사용자가 직접 입력하는 총 페이지 수(pageCountSnapshot)를 저장합니다.
                - 인증: Access Token(Bearer)
                - 책 메타(books)와 무관하게 user_books에 스냅샷으로 저장됩니다.
                - 입력 후 progressPercent는 currentPage 기준으로 재계산됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode="204", description="저장 성공", content=@Content),
            @ApiResponse(responseCode="400", description="요청값 오류", content=@Content),
            @ApiResponse(responseCode="401", description="인증 실패", content=@Content),
            @ApiResponse(responseCode="404", description="저장 도서 없음/권한 없음", content=@Content)
    })
    @PatchMapping(value = "/{userBookId}/total-page", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveTotalPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "userBookId") Long userBookId,
            @RequestBody @Valid TotalPageSaveRequest req
    ) {
        userBooksService.saveTotalPage(userDetails.getUserId(), userBookId, req);
    }

    @Operation(
            summary = "저장 도서 수정(상태/책종류 변경, 서재 추가)",
            description = """
            user_books의 일부 필드만 변경합니다.
            - 인증: Access Token(Bearer)
            - status: 읽기 상태 변경
            - format: 책 종류(종이/전자/오디오) 변경
            - shelfId: (A방식) 해당 서재에 '추가' (이동 아님)
            - 응답: 204 No Content
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode="204", description="수정 성공", content=@Content),
            @ApiResponse(responseCode="400", description="요청값 오류", content=@Content),
            @ApiResponse(responseCode="401", description="인증 실패", content=@Content),
            @ApiResponse(responseCode="403", description="내 서재가 아님", content=@Content),
            @ApiResponse(responseCode="404", description="저장 도서/서재 없음", content=@Content)
    })
    @PatchMapping(value = "/{userBookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "userBookId") Long userBookId,
            @RequestBody @Valid UserBookUpdateRequest req
    ) {
        userBooksService.update(userDetails.getUserId(), userBookId, req);
    }
}

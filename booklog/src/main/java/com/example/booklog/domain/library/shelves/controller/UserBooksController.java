package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.service.UserBooksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

                    - 헤더: X-USER-ID (필수, null 불가)
                    - 요청 바디: UserBookCreateRequest (필수, null 불가)
                      - bookId: 필수(null 불가) → books의 PK
                      - shelfId: 선택(null 가능)
                        - 값이 있으면 해당 shelf에 (shelf_id, book_id)로 bookshelf_items 매핑을 '추가'합니다.
                        - 같은 책을 여러 서재에 담을 수 있습니다. (A 방식)
                      - status: 선택(null 가능) → null이면 기본값 TO_READ 처리
                    - 예외:
                      - 이미 저장한 책(user_books에 이미 존재)이면 409(중복)
                      - bookId가 없으면 404(책 없음)
                      - shelfId가 있고 서재가 없거나 내 서재가 아니면 404/403
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
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "저장 도서 생성 요청 바디 (필수, null 불가)",
                    required = true
            )
            @RequestBody @Valid UserBookCreateRequest req
    ) {
        return userBooksService.create(userId, req);
    }

    @Operation(
            summary = "저장 도서 목록 조회(전체)",
            description = """
                    유저가 저장한 도서를 '전체' 조회합니다. (현재는 페이지네이션 없음)

                    - 헤더: X-USER-ID (필수, null 불가)
                    - Query Params:
                      - shelfId: 선택(null 가능)
                        - 값이 있으면: 해당 서재에 담긴 책(bookshelf_items에 매핑된 book)만 필터링합니다.
                      - status: 선택(null 가능) → TO_READ/READING/DONE/STOPPED 필터링
                      - sort: 선택(null 가능, 기본 LATEST)
                        - LATEST/OLDEST/TITLE/AUTHOR
                        - AUTHOR는 현재 임시로 title 기준(추후 author join 정렬로 확장 예정)
                    - 응답: List<UserBookListItemResponse>
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(리스트 반환)"),
            @ApiResponse(responseCode = "400", description = "정렬/상태 값이 허용 범위를 벗어남")
    })
    @GetMapping
    public List<UserBookListItemResponse> list(
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(
                    description = "서재 ID (선택, null 가능). 값이 있으면 해당 서재에 담긴 저장도서만 조회",
                    example = "10"
            )
            @RequestParam(required = false) Long shelfId,

            @Parameter(
                    description = "독서 상태 (선택, null 가능). TO_READ/READING/DONE/STOPPED",
                    example = "READING"
            )
            @RequestParam(required = false) String status,

            @Parameter(
                    description = "정렬 (선택, null 가능). 기본값: LATEST (LATEST/OLDEST/TITLE/AUTHOR)",
                    example = "LATEST"
            )
            @RequestParam(required = false, defaultValue = "LATEST") String sort
    ) {
        return userBooksService.listAll(userId, shelfId, status, sort);
    }

    @Operation(
            summary = "저장 도서 삭제(라이브러리 삭제 / 서재에서만 제거)",
            description = """
                    저장 도서를 삭제합니다. 아래 우선순위로 동작합니다.

                    1) Body.ids가 있으면: '선택 삭제(라이브러리에서 완전 삭제)'
                       - user_books에서 해당 userBookId들을 삭제합니다.
                       - A방식이므로 bookshelf_items는 (user의 모든 서재에서) 해당 book_id 매핑도 함께 정리됩니다.

                    2) shelfId가 있으면: '서재에서만 제거'
                       - bookshelf_items에서 해당 shelf의 매핑만 삭제합니다.
                       - user_books는 유지됩니다(= 라이브러리에는 남아있음).

                    3) status가 있으면: '상태별 전체 삭제(라이브러리에서 완전 삭제)'
                       - user_books에서 해당 status의 책들을 삭제합니다.
                       - bookshelf_items에서도 해당 book_id 매핑을 전부 정리합니다(유저의 모든 서재에서 제거).

                    4) 위 조건이 모두 없으면: '전체 삭제(라이브러리 전체 삭제)'
                       - user_books 전부 삭제 + bookshelf_items 전부 삭제

                    - 헤더: X-USER-ID (필수, null 불가)
                    - RequestBody(DeleteBody):
                      - ids: 선택(null 가능)
                      - body 자체도 선택(null 가능)
                    - Query Params:
                      - shelfId: 선택(null 가능)
                      - status: 선택(null 가능)

                    - 반환값: 삭제된 개수(int)
                      - ids/status/전체삭제: user_books에서 삭제된 row 수
                      - shelfId: bookshelf_items에서 삭제된 row 수
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
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            삭제 요청 바디 (선택, null 가능)
                            - ids: 선택(null 가능). 있으면 해당 userBookId들을 '라이브러리에서' 선택 삭제 
                            """,
                    required = false
            )//ids를 통해서 작제시 전체가 삭제됨
            @RequestBody(required = false) DeleteBody body,

            @Parameter(
                    description = "서재 ID (선택, null 가능). 값이 있으면 해당 서재에서만 책 매핑을 전체 제거(라이브러리는 유지)",
                    example = "10"
            )
            @RequestParam(required = false) Long shelfId,

            @Parameter(
                    description = "상태 (선택, null 가능). 값이 있으면 해당 상태의 저장도서를 라이브러리에서 전체 삭제(서재 매핑도 함께 정리)",
                    example = "STOPPED"
            )
            @RequestParam(required = false) String status
    ) {
        List<Long> ids = (body == null) ? null : body.ids();
        return userBooksService.delete(userId, ids, shelfId, status);
    }

    @Schema(name = "DeleteBody", description = "저장 도서 삭제 요청 바디")
    public record DeleteBody(
            @ArraySchema(
                    schema = @Schema(description = "선택 삭제할 userBookId 리스트 (선택, null 가능)", example = "[1,2,3]")
            )
            List<Long> ids
    ) {}

    @Operation(
            summary = "저장 도서 상세 조회 사용가능성 낮음",
            description = """
                    userBookId 기준으로 저장 도서 상세 정보를 조회합니다.

                    - 헤더: X-USER-ID (필수, null 불가)
                    - Path:
                      - userBookId: 필수(null 불가)
                    - 응답: UserBookDetailResponse
                    - 주의: 내 저장 도서가 아니면 조회 불가(현재는 findByUser_IdAndId)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(상세 반환)"),
            @ApiResponse(responseCode = "404", description = "저장 도서 없음")
    })
    @GetMapping("/{userBookId}")
    public UserBookDetailResponse detail(
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(
                    description = "저장 도서 ID(userBookId) (필수, null 불가)",
                    required = true,
                    example = "100"
            )
            @PathVariable Long userBookId
    ) {
        return userBooksService.detail(userId, userBookId);
    }

    @Operation(
            summary = "저장 도서 수정(상태 변경 + 서재 이동)",
            description = """
        저장 도서(user_books)의 일부 필드만 수정합니다. (PATCH)

        - 헤더: X-USER-ID (필수)
        - Path:
          - userBookId: 필수
        - Body(UserBookUpdateRequest): 변경할 값만 넣습니다.
          - status: 선택(null 가능) → 상태 변경
          - shelfId: 선택(null 가능) → 해당 서재로 이동 (정책: 1권은 1서재만 가능)

        ※ shelfId가 들어오면 기존 서재 매핑은 삭제 후 새 서재로 연결됩니다.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(수정 완료)"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "403", description = "내 서재가 아님"),
            @ApiResponse(responseCode = "404", description = "저장 도서/서재 없음")
    })
    @PatchMapping("/{userBookId}")
    public void update(
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(
                    description = "저장 도서 ID(userBookId) (필수, null 불가)",
                    required = true,
                    example = "100"
            )
            @PathVariable Long userBookId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            수정 요청 바디
                            - status: 선택(null 가능)
                            - shelfId: 선택(null 가능) → 해당 서재에 추가
                            """,
                    required = true
            )
            @RequestBody UserBookUpdateRequest req
    ) {
        userBooksService.update(userId, userBookId, req);
    }
}

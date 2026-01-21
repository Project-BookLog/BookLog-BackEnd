package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.service.BookshelvesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "서재(Shelves)",
        description = "서재 생성/관리 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shelves")
public class BookshelvesController {

    private final BookshelvesService bookshelvesService;

    @Operation(
            summary = "서재 생성",
            description = """
                    새로운 서재를 생성합니다.

                    - 헤더: X-USER-ID (필수, null 불가)
                    - 요청 바디: BookshelfCreateRequest (필수, null 불가)
                      - name: 필수 (null 불가)
                      - isPublic: 필수 (null 불가)
                    - 응답: 생성된 shelfId 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패(예: name 누락/길이 초과 등)"),
            @ApiResponse(responseCode = "401", description = "유저 식별 실패(헤더 누락 등)"),
            @ApiResponse(responseCode = "409", description = "중복 서재명 등 정책 위반(동일 유저의 동일 name)")
    })
    @PostMapping
    public BookshelfCreateResponse create(
            @Parameter(
                    name = "X-USER-ID",
                    description = "유저 식별자 (필수, null 불가)",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-USER-ID") Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "서재 생성 요청 바디 (필수, null 불가)",
                    required = true
            )
            @RequestBody @Valid BookshelfCreateRequest req
    ) {
        return bookshelvesService.create(userId, req);
    }
}

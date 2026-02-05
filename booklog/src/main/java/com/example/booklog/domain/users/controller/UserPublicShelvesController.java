package com.example.booklog.domain.users.controller;

import com.example.booklog.domain.users.dto.UserPublicShelfListResponse;
import com.example.booklog.domain.users.service.UserPublicShelvesService;
import com.example.booklog.domain.users.service.UserPublicShelvesService.PublicShelfBookSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "다른 유저 공개 서재",
        description = "다른 유저의 공개 서재 목록/서재 도서 목록 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/shelves")
public class UserPublicShelvesController {

    private final UserPublicShelvesService userPublicShelvesService;

    @Operation(
            summary = "다른 유저 공개 서재 목록 + 서재별 top3",
            description = """
                    다른 유저의 서재 중 isPublic=true 서재만 반환합니다.
                    각 서재 카드에는 최근 담은 책 3권(사진/출판사/저자)을 포함합니다.
                    - 인증: 필요 없음(공개 서재만 조회)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "유저 없음 또는 공개 서재 없음")
    })
    @GetMapping
    public UserPublicShelfListResponse listPublicShelves(
            @PathVariable Long userId
    ) {
        return userPublicShelvesService.listPublicShelves(userId);
    }

    @Operation(
            summary = "공개 서재의 전체 도서 목록(정렬만)",
            description = """
                    특정 공개 서재의 전체 도서 목록을 반환합니다.
                    - Query:
                      - sort: LATEST/OLDEST/TITLE/AUTHOR (기본 LATEST)
                    - 응답 항목: 사진(thumbnailUrl), 출판사(publisherName), 저자(authorName)
                    - 인증: 필요 없음(공개 서재만 조회)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "서재 없음 또는 비공개")
    })
    @GetMapping("/{shelfId}/books")
    public UserPublicShelfListResponse.UserPublicShelfBooksResponse listPublicShelfBooks(
            @PathVariable Long userId,
            @PathVariable Long shelfId,
            @RequestParam(defaultValue = "LATEST") PublicShelfBookSort sort
    ) {
        return userPublicShelvesService.listPublicShelfBooks(userId, shelfId, sort);
    }
}

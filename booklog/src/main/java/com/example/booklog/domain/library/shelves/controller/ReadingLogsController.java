// =====================================
// [독서기록] ReadingLogsController
// =====================================
package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.ReadingLogResponse;
import com.example.booklog.domain.library.shelves.dto.ReadingLogSaveRequest;
import com.example.booklog.domain.library.shelves.service.ReadingLogsService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "독서 기록(Reading Logs)", description = "독서 기록 저장·수정·삭제 API")
@RestController
@RequiredArgsConstructor
public class ReadingLogsController {

    private final ReadingLogsService readingLogsService;

    @Operation(
            summary = "독서 기록 저장(append)",
            description = """
                    특정 저장 도서(userBookId)에 대해 독서 기록을 추가(append)합니다.
                    - 인증: Access Token(Bearer)
                    - 입력: readDate(날짜), pagesRead(그날 읽은 페이지 수)
                    - 처리: 누적 currentPage는 서버 계산, user_books 최신상태 함께 갱신
                    """
    )
    @ApiResponse(responseCode = "200", description = "저장 성공",
            content = @Content(schema = @Schema(implementation = ReadingLogResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청값 오류/저장 도서 없음", content = @Content)
    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    @PostMapping(value = "/api/v1/user-books/{userBookId}/reading-logs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReadingLogResponse create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userBookId,
            //스웨거(OpenAPI 문서용) RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "독서 기록 저장 요청",
                    content = @Content(
                            schema = @Schema(implementation = ReadingLogSaveRequest.class),
                            examples = @ExampleObject(
                                    name = "기본 예시",
                                    summary = "특정 날짜에 57페이지 읽음",
                                    value = """
                                            {
                                              "readDate": "2026-01-10",
                                              "pagesRead": 57
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.create(userDetails.getUserId(), userBookId, req);
    }

    @Operation(
            summary = "독서 기록 수정",
            description = """
                    특정 독서 기록(logId)의 날짜/읽은 페이지를 수정합니다.
                    - 인증: Access Token(Bearer)
                    - 처리: 중간 기록 수정 시 누적값이 연쇄 변경될 수 있어 user_books 상태 재계산
                    """
    )
    @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = ReadingLogResponse.class)))
    @ApiResponse(responseCode = "404", description = "독서 기록 없음/권한 없음", content = @Content)
    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    @PatchMapping(value = "/api/v1/reading-logs/{logId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReadingLogResponse update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long logId,
            @org.springframework.web.bind.annotation.RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.update(userDetails.getUserId(), logId, req);
    }

    @Operation(
            summary = "독서 기록 삭제",
            description = """
                    특정 독서 기록(logId)을 삭제합니다.
                    - 인증: Access Token(Bearer)
                    - 처리: 삭제 후 해당 userBook 로그 기반으로 user_books 최신상태 재계산
                    """
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
    @ApiResponse(responseCode = "404", description = "독서 기록 없음/권한 없음", content = @Content)
    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    @DeleteMapping("/api/v1/reading-logs/{logId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long logId
    ) {
        readingLogsService.delete(userDetails.getUserId(), logId);
    }
}

package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.ReadingLogResponse;
import com.example.booklog.domain.library.shelves.dto.ReadingLogSaveRequest;
import com.example.booklog.domain.library.shelves.service.ReadingLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "독서 기록", description = "독서 기록(날짜/읽은 페이지) 저장·수정·삭제 API")
@RestController
@RequiredArgsConstructor
public class ReadingLogsController {

    private final ReadingLogsService readingLogsService;

    @Operation(
            summary = "독서 기록 저장",
            description = """
                    특정 저장 도서(userBookId)에 대해 독서 기록을 추가(append)합니다.
                    - UI 입력: readDate(읽은 날짜), pagesRead(그날 읽은 페이지 수)
                    - 서버 처리: currentPage(누적 현재 페이지)는 서버가 계산하여 저장하며,
                      user_books.current_page/progress_percent 등의 최신 상태도 함께 갱신됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "저장 성공",
            content = @Content(schema = @Schema(implementation = ReadingLogResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청값 오류/저장 도서 없음", content = @Content)
    @PostMapping(value = "/api/v1/user-books/{userBookId}/reading-logs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReadingLogResponse create(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(description = "저장 도서 ID(user_books.user_book_id)", required = true, example = "101")
            @PathVariable Long userBookId,

            @RequestBody(
                    required = true,
                    description = "독서 기록 저장 요청",
                    content = @Content(
                            schema = @Schema(implementation = ReadingLogSaveRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "기본 예시",
                                            summary = "특정 날짜에 57페이지 읽음",
                                            value = """
                                                    {
                                                      "readDate": "2026-01-10",
                                                      "pagesRead": 57
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.create(userId, userBookId, req);
    }

    @Operation(
            summary = "독서 기록 수정",
            description = """
                    특정 독서 기록(logId)의 날짜/읽은 페이지를 수정합니다.
                    - UI 입력: readDate, pagesRead
                    - 서버 처리: 중간 기록 수정 시 누적 currentPage가 연쇄 변경될 수 있어,
                      해당 userBook의 로그들을 기준으로 currentPage/user_books 상태를 재계산합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = ReadingLogResponse.class)))
    @ApiResponse(responseCode = "404", description = "독서 기록 없음/권한 없음", content = @Content)
    @PatchMapping(value = "/api/v1/reading-logs/{logId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReadingLogResponse update(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(description = "독서 기록 ID(reading_logs.log_id)", required = true, example = "5001")
            @PathVariable Long logId,

            @RequestBody(
                    required = true,
                    description = "독서 기록 수정 요청",
                    content = @Content(
                            schema = @Schema(implementation = ReadingLogSaveRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "수정 예시",
                                            summary = "페이지 수를 80으로 수정",
                                            value = """
                                                    {
                                                      "readDate": "2026-01-10",
                                                      "pagesRead": 80
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.update(userId, logId, req);
    }

    @Operation(
            summary = "독서 기록 삭제",
            description = """
                    특정 독서 기록(logId)을 삭제합니다.
                    - 서버 처리: 삭제 후 해당 userBook의 로그 기반으로 user_books 최신상태를 재계산합니다.
                    """
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
    @ApiResponse(responseCode = "404", description = "독서 기록 없음/권한 없음", content = @Content)
    @DeleteMapping("/api/v1/reading-logs/{logId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(name = "X-USER-ID", description = "유저 식별자(필수)", required = true, example = "1")
            @RequestHeader("X-USER-ID") Long userId,

            @Parameter(description = "독서 기록 ID(reading_logs.log_id)", required = true, example = "5001")
            @PathVariable Long logId
    ) {
        readingLogsService.delete(userId, logId);
    }
}

package com.example.booklog.domain.library.shelves.controller;

import com.example.booklog.domain.library.shelves.dto.ReadingLogResponse;
import com.example.booklog.domain.library.shelves.dto.ReadingLogSaveRequest;
import com.example.booklog.domain.library.shelves.service.ReadingLogsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReadingLogsController {

    private final ReadingLogsService readingLogsService;

    /** 독서 기록 저장(UI: 읽은 페이지 수 입력) */
    @PostMapping("/api/v1/user-books/{userBookId}/reading-logs")
    public ReadingLogResponse create(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long userBookId,
            @RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.create(userId, userBookId, req);
    }

    /** 독서 기록 수정(UI: 날짜/읽은 페이지 수 수정) */
    @PatchMapping("/api/v1/reading-logs/{logId}")
    public ReadingLogResponse update(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long logId,
            @RequestBody @Valid ReadingLogSaveRequest req
    ) {
        return readingLogsService.update(userId, logId, req);
    }

    /** 독서 기록 삭제 */
    @DeleteMapping("/api/v1/reading-logs/{logId}")
    public void delete(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long logId
    ) {
        readingLogsService.delete(userId, logId);
    }
}

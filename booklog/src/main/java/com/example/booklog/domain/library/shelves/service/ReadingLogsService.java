package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.shelves.dto.ReadingLogResponse;
import com.example.booklog.domain.library.shelves.dto.ReadingLogSaveRequest;
import com.example.booklog.domain.library.shelves.entity.ReadingLogs;
import com.example.booklog.domain.library.shelves.entity.ReadingStatus;
import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.library.shelves.repository.ReadingLogsRepository;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingLogsService {

    private final ReadingLogsRepository readingLogsRepository;
    private final UserBooksRepository userBooksRepository;

    /** POST /api/v1/user-books/{userBookId}/reading-logs */
    @Transactional
    public ReadingLogResponse create(Long userId, Long userBookId, ReadingLogSaveRequest req) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND));


        // prevCurrent 계산 (일단 유지)
        int prevCurrent = readingLogsRepository
                .findTopByUserBook_IdOrderByReadDateDescCreatedAtDesc(userBookId)
                .map(ReadingLogs::getCurrentPage)
                .orElse(0);

        int newCurrent = Math.max(0, prevCurrent + req.pagesRead());

        // total page 있으면 clamp
        Integer total = ub.getPageCountSnapshot();
        if (total != null && total > 0) {
            newCurrent = Math.min(newCurrent, total);
        }

        ReadingLogs saved = readingLogsRepository.save(
                ReadingLogs.builder()
                        .userBook(ub)
                        .readDate(req.readDate())
                        .pagesRead(req.pagesRead())
                        .currentPage(newCurrent)
                        .build()
        );

        // 저장 후 전체 재계산(로그 누적 + user_books)
        recalcLogsAndUserBook(ub);

        return new ReadingLogResponse(
                saved.getId(),
                ub.getId(),
                saved.getReadDate(),
                saved.getPagesRead(),
                saved.getCurrentPage()
        );
    }

    /** PATCH /api/v1/reading-logs/{logId} */
    @Transactional
    public ReadingLogResponse update(Long userId, Long logId, ReadingLogSaveRequest req) {
        ReadingLogs log = readingLogsRepository.findOwned(userId, logId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.READING_LOG_NOT_FOUND_OR_FORBIDDEN));

        UserBooks ub = log.getUserBook();

        // pagesRead/readDate만 수정하고 누적은 전체 재계산
        log.update(req.readDate(), req.pagesRead(), log.getCurrentPage());

        recalcLogsAndUserBook(ub);

        ReadingLogs updated = readingLogsRepository.findById(logId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.READING_LOG_UPDATED_FETCH_FAILED));

        return new ReadingLogResponse(
                updated.getId(),
                ub.getId(),
                updated.getReadDate(),
                updated.getPagesRead(),
                updated.getCurrentPage()
        );
    }

    /** DELETE /api/v1/reading-logs/{logId} */
    @Transactional
    public void delete(Long userId, Long logId) {
        ReadingLogs log = readingLogsRepository.findOwned(userId, logId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.READING_LOG_NOT_FOUND_OR_FORBIDDEN));

        UserBooks ub = log.getUserBook();
        readingLogsRepository.delete(log);

        recalcLogsAndUserBook(ub);
    }

    // ---------------- 내부 로직 ----------------

    private void recalcLogsAndUserBook(UserBooks ub) {
        List<ReadingLogs> logs = readingLogsRepository
                .findByUserBook_IdOrderByReadDateAscCreatedAtAsc(ub.getId());

        int running = 0;
        Integer total = ub.getPageCountSnapshot();

        for (ReadingLogs rl : logs) {
            int pages = (rl.getPagesRead() == null) ? 0 : Math.max(0, rl.getPagesRead());
            running += pages;

            if (total != null && total > 0) {
                running = Math.min(running, total);
            }

            rl.update(rl.getReadDate(), pages, running);
        }

        applyUserBookFromComputed(ub, logs, running);
    }

    private void applyUserBookFromComputed(UserBooks ub, List<ReadingLogs> logs, int currentPageComputed) {
        if (!logs.isEmpty()) {
            ub.setStartDateIfNull(logs.get(0).getReadDate());
        }

        Integer total = ub.getPageCountSnapshot();
        int percent = calcPercent(currentPageComputed, total);
        ub.updateProgress(currentPageComputed, percent);

        if (ub.getStatus() == ReadingStatus.COMPLETED) {
            LocalDate last = logs.isEmpty() ? null : logs.get(logs.size() - 1).getReadDate();
            ub.setEndDate(last);
        } else {
            ub.setEndDate(null);
        }
    }

    private int calcPercent(int current, Integer total) {
        if (total == null || total <= 0) return 0;
        return (int) Math.min(100, Math.round(current * 100.0 / total));
    }

    private void applyStatusChange(UserBooks ub, ReadingStatus newStatus) {
        ub.updateStatus(newStatus);

        if (newStatus == ReadingStatus.READING) {
            ub.setStartDateIfNull(LocalDate.now());
        } else if (newStatus == ReadingStatus.COMPLETED) {
            ub.setStartDateIfNull(LocalDate.now());
            // end_date는 applyUserBookFromComputed에서 마지막 로그 날짜로 세팅됨
        } else {
            // TO_READ/STOPPED 등은 end_date 제거
            ub.setEndDate(null);
        }
    }
}

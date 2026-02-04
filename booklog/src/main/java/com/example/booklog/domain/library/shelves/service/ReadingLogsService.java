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

        // prevCurrent 계산 (기존 유지)
        int prevCurrent = readingLogsRepository
                .findTopByUserBook_IdOrderByReadDateDescCreatedAtDesc(userBookId)
                .map(ReadingLogs::getCurrentPage)
                .orElse(0);

        int pagesRead = (req.pagesRead() == null) ? 0 : Math.max(0, req.pagesRead());
        int newCurrent = Math.max(0, prevCurrent + pagesRead);

        // total page 있으면 clamp
        Integer total = ub.getPageCountSnapshot();
        if (total != null && total > 0) {
            newCurrent = Math.min(newCurrent, total);
        }

        ReadingLogs saved = readingLogsRepository.save(
                ReadingLogs.builder()
                        .userBook(ub)
                        .readDate(req.readDate())
                        .pagesRead(pagesRead)
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

        int pagesRead = (req.pagesRead() == null) ? 0 : Math.max(0, req.pagesRead());

        // readDate/pagesRead만 수정 (currentPage는 전체 재계산에서 다시 덮어씀)
        log.update(req.readDate(), pagesRead, log.getCurrentPage());

        recalcLogsAndUserBook(ub);

        // 영속 상태에서 바로 반환해도 OK (굳이 재조회 필요 없음)
        return new ReadingLogResponse(
                log.getId(),
                ub.getId(),
                log.getReadDate(),
                log.getPagesRead(),
                log.getCurrentPage()
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

            // 각 로그의 누적 currentPage 갱신
            rl.update(rl.getReadDate(), pages, running);
        }

        applyUserBookFromComputed(ub, logs, running);
    }

    private void applyUserBookFromComputed(UserBooks ub, List<ReadingLogs> logs, int currentPageComputed) {
        if (!logs.isEmpty()) {
            ub.setStartDateIfNull(logs.get(0).getReadDate());
        }

        // ✅ updateProgress 제거 대체: currentPage만 세팅하면 progress는 엔티티가 자동 계산
        ub.setCurrentPage(currentPageComputed);

        // COMPLETED이면 endDate = 마지막 로그 날짜, 아니면 null
        if (ub.getStatus() == ReadingStatus.COMPLETED) {
            LocalDate last = logs.isEmpty() ? null : logs.get(logs.size() - 1).getReadDate();
            ub.setEndDate(last);
        } else {
            ub.setEndDate(null);
        }
    }
}

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

        Integer total = ub.getPageCountSnapshot();
        if (total == null || total < 0) {
            throw new GeneralException(ErrorStatus.TOTAL_PAGE_REQUIRED); // 409
        }

        int prevCurrent = readingLogsRepository
                .findTopByUserBook_IdOrderByReadDateDescCreatedAtDesc(userBookId)
                .map(ReadingLogs::getCurrentPage)
                .orElse(0);

        int inputCurrent = Math.max(0, req.currentPage());
        // total 기준 clamp
        inputCurrent = Math.min(inputCurrent, total);

        int pagesRead = inputCurrent - prevCurrent; // 음수 가능

        ReadingLogs saved = readingLogsRepository.save(
                ReadingLogs.builder()
                        .userBook(ub)
                        .readDate(req.readDate())
                        .pagesRead(pagesRead)
                        .currentPage(inputCurrent)
                        .build()
        );

        // 저장 후 전체 재계산(중간 수정/삭제 대비)
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

        Integer total = ub.getPageCountSnapshot();
        if (total == null || total <= 0) {
            throw new GeneralException(ErrorStatus.TOTAL_PAGE_REQUIRED);
        }

        int inputCurrent = Math.max(0, req.currentPage());
        inputCurrent = Math.min(inputCurrent, total);

        // 일단 로그에 absolute currentPage를 반영 (pagesRead는 recalc에서 재계산)
        log.update(req.readDate(), log.getPagesRead(), inputCurrent);

        recalcLogsAndUserBook(ub);

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

        Integer total = ub.getPageCountSnapshot();
        if (total == null || total <= 0) {
            throw new GeneralException(ErrorStatus.TOTAL_PAGE_REQUIRED);
        }

        int prev = 0;

        for (ReadingLogs rl : logs) {
            // null 안전 처리
            int cur = (rl.getCurrentPage() == null) ? 0 : Math.max(0, rl.getCurrentPage());

            // total 기준 clamp
            cur = Math.min(cur, total);

            // ✅ 되돌림 허용: cur < prev여도 OK (pagesRead가 음수가 됨)
            int delta = cur - prev;

            // pagesRead(=delta)는 서버 계산값으로 통일해서 덮어씀
            rl.update(rl.getReadDate(), delta, cur);

            prev = cur;
        }

        // user_books는 "마지막 로그 currentPage"가 최신 상태
        applyUserBookFromComputed(ub, logs, prev);
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

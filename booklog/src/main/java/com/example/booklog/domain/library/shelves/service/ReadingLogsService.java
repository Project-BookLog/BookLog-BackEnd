package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.shelves.dto.ReadingLogResponse;
import com.example.booklog.domain.library.shelves.dto.ReadingLogSaveRequest;
import com.example.booklog.domain.library.shelves.entity.ReadingLogs;
import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.library.shelves.repository.ReadingLogsRepository;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("저장 도서 없음"));

        // ✅ UI 입력은 pagesRead, currentPage는 서버가 계산
        int prevCurrent = readingLogsRepository.findTopByUserBook_IdOrderByReadDateDescCreatedAtDesc(userBookId)
                .map(ReadingLogs::getCurrentPage)
                .orElse(0);

        int newCurrent = Math.max(0, prevCurrent + req.pagesRead());

        ReadingLogs saved = readingLogsRepository.save(
                ReadingLogs.builder()
                        .userBook(ub)
                        .readDate(req.readDate())
                        .pagesRead(req.pagesRead())
                        .currentPage(newCurrent)
                        .build()
        );

        // ✅ 저장 후 user_books 최신상태 재계산
        recalcUserBookFromLogs(ub);

        return new ReadingLogResponse(saved.getId(), ub.getId(), saved.getReadDate(), saved.getPagesRead(), saved.getCurrentPage());
    }

    /** PATCH /api/v1/reading-logs/{logId} */
    @Transactional
    public ReadingLogResponse update(Long userId, Long logId, ReadingLogSaveRequest req) {
        ReadingLogs log = readingLogsRepository.findOwned(userId, logId)
                .orElseThrow(() -> new IllegalArgumentException("독서 기록 없음/권한 없음"));

        // 일단 pagesRead, readDate만 바꾸고
        // currentPage는 “전체 재계산”으로 맞추는 게 안전함(중간 기록 수정 시 누적이 연쇄적으로 바뀜)
        log.update(req.readDate(), req.pagesRead(), log.getCurrentPage());

        // ✅ 수정 후 user_books + 로그 currentPage를 전체 재계산해서 정합성 보장
        UserBooks ub = log.getUserBook();
        recalcLogsAndUserBook(ub);

        // 재계산 후 최신 log 다시 조회(또는 영속 상태 그대로 써도 되지만, 안전하게 다시 찾아도 됨)
        ReadingLogs updated = readingLogsRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("수정된 로그 조회 실패"));

        return new ReadingLogResponse(updated.getId(), ub.getId(), updated.getReadDate(), updated.getPagesRead(), updated.getCurrentPage());
    }

    /** DELETE /api/v1/reading-logs/{logId} */
    @Transactional
    public void delete(Long userId, Long logId) {
        ReadingLogs log = readingLogsRepository.findOwned(userId, logId)
                .orElseThrow(() -> new IllegalArgumentException("독서 기록 없음/권한 없음"));

        UserBooks ub = log.getUserBook();
        readingLogsRepository.delete(log);

        // ✅ 삭제 후 전체 재계산
        recalcLogsAndUserBook(ub);
    }

    // ---------------- 내부 로직 ----------------

    /**
     * logs를 기준으로:
     * - 각 로그의 currentPage를 누적합으로 다시 계산
     * - user_books.current_page, progress_percent, start_date, end_date 갱신
     */
    private void recalcLogsAndUserBook(UserBooks ub) {
        List<ReadingLogs> logs = readingLogsRepository.findByUserBook_IdOrderByReadDateAscCreatedAtAsc(ub.getId());

        int running = 0;
        for (ReadingLogs rl : logs) {
            int pages = (rl.getPagesRead() == null) ? 0 : Math.max(0, rl.getPagesRead());
            running += pages;
            rl.update(rl.getReadDate(), pages, running);
        }

        applyUserBookFromComputed(ub, logs, running);
    }

    /**
     * 빠르게 user_books만 재계산하고 싶을 때도 결국 logs를 기반으로 해야 안전.
     * (여기서는 create에서도 같은 루틴을 사용)
     */
    private void recalcUserBookFromLogs(UserBooks ub) {
        recalcLogsAndUserBook(ub);
    }

    private void applyUserBookFromComputed(UserBooks ub, List<ReadingLogs> logs, int currentPageComputed) {
        // start_date = 첫 기록 날짜(없으면 그대로 유지)
        if (!logs.isEmpty()) {
            ub.setStartDateIfNull(logs.get(0).getReadDate());
        }

        // progress = current / total(= pageCountSnapshot)
        Integer total = ub.getPageCountSnapshot();
        int percent = calcPercent(currentPageComputed, total);

        ub.updateProgress(currentPageComputed, percent);

        // end_date 정책: DONE이면 마지막 기록 날짜, 아니면 null
        if ("DONE".equals(ub.getStatus())) {
            LocalDate last = logs.isEmpty() ? null : logs.get(logs.size() - 1).getReadDate();
            ub.setEndDate(last);
        } else {
            ub.setEndDate(null);
        }
        // BaseEntity의 updatedAt이 자연스럽게 “최근 기록 반영 시각” 역할
    }

    private int calcPercent(int current, Integer total) {
        if (total == null || total <= 0) return 0;
        return (int) Math.min(100, Math.round(current * 100.0 / total));
    }
}

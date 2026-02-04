package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_books",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_books_user_book", columnNames = {"user_id", "book_id"}),
        indexes = {
                @Index(name = "idx_user_books_user", columnList = "user_id"),
                @Index(name = "idx_user_books_status", columnList = "status"),
                @Index(name = "idx_user_books_book", columnList = "book_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBooks extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_books_user"))
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_books_book"))
    private Books book;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReadingStatus status; // TO_READ/READING/COMPLETED/STOPPED

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 20)
    private BookFormat format; // PAPER/EBOOK/AUDIO 등

    @Column(name = "page_count_snapshot")
    private Integer pageCountSnapshot; // 유저가 설정한 총 페이지 수(판본/개인 기준). 진행률 계산 기준값

    @Builder
    public UserBooks(Users user, Books book, ReadingStatus status) {
        this.user = user;
        this.book = book;
        this.status = (status != null) ? status : ReadingStatus.TO_READ;
        this.progressPercent = 0;
    }

    /* =========================
     * 상태/기본 정보 변경
     * ========================= */

    public void updateStatus(ReadingStatus status) {
        this.status = status;
    }

    public void changeStatus(ReadingStatus newStatus) {
        if (newStatus == null) return;

        this.status = newStatus;

        if (newStatus == ReadingStatus.READING) {
            setStartDateIfNull(LocalDate.now());
            this.endDate = null;
        } else if (newStatus == ReadingStatus.COMPLETED) {
            setStartDateIfNull(LocalDate.now());
            this.endDate = LocalDate.now();

            // 완료면 진행률 100 + currentPage는 total이 있으면 total로 맞춤
            if (this.pageCountSnapshot != null) {
                this.currentPage = this.pageCountSnapshot;
            }
            this.progressPercent = 100;
        } else { // TO_READ / STOPPED
            this.endDate = null;
            // 필요하면 TO_READ면 progress 초기화 같은 정책도 여기서 결정
        }
    }

    public void setStartDateIfNull(LocalDate date) {
        if (this.startDate == null) this.startDate = date;
    }

    public void setEndDate(LocalDate date) {
        this.endDate = date;
    }

    public void updateFormat(BookFormat format) {
        this.format = format;
    }

    /* =========================
     * 페이지/진행률 관련
     * ========================= */

    /**
     * (레거시 호환) 기존 코드에서 호출 중이면 유지.
     * 내부적으로는 setTotalPages로 위임.
     */
    public void updatePageCountSnapshot(Integer totalPage) {
        setTotalPages(totalPage);
    }

    /**
     * 총 페이지 설정.
     * - totalPages 유효성 검증
     * - totalPages가 줄어 currentPage가 초과하면 currentPage를 totalPages로 보정(clamp)
     * - 진행률 재계산
     */
    public void setTotalPages(Integer totalPages) {
        if (totalPages == null || totalPages < 1) {
            throw new GeneralException(ErrorStatus.TOTAL_PAGE_INVALID);
        }
        this.pageCountSnapshot = totalPages;

        if (this.currentPage != null && this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }

        recalcProgressPercent();
    }

    /**
     * 현재 페이지 설정.
     * - currentPage 유효성 검증
     * - totalPages가 존재하면 초과 시 보정(clamp)
     * - 진행률 재계산
     */
    public void setCurrentPage(Integer currentPage) {
        if (currentPage == null || currentPage < 0) {
            throw new GeneralException(ErrorStatus.CURRENT_PAGE_INVALID);
        }

        if (this.pageCountSnapshot != null && currentPage > this.pageCountSnapshot) {
            currentPage = this.pageCountSnapshot; // clamp 정책
        }

        this.currentPage = currentPage;
        recalcProgressPercent();
    }

    /**
     * 독서기록 저장/수정 시 user_books에 반영할 때 사용.
     * - currentPage 반영 + 진행률 재계산
     * - 상태가 READING이면 시작일 세팅
     */
    public void applyReadingProgress(Integer currentPage, LocalDate readDate) {
        setCurrentPage(currentPage);

        if (this.status == ReadingStatus.READING) {
            setStartDateIfNull(readDate != null ? readDate : LocalDate.now());
        }
    }

    /**
     * 진행률 재계산 (currentPage / totalPages 기반)
     */
    private void recalcProgressPercent() {
        if (currentPage == null || pageCountSnapshot == null || pageCountSnapshot <= 0) {
            this.progressPercent = 0;
            return;
        }
        this.progressPercent = (int) Math.min(100, Math.round(currentPage * 100.0 / pageCountSnapshot));
    }

    /* =========================
     * (선택) 기존 updateProgress 메서드 제거 권장
     * =========================
     * 아래 메서드는 progressPercent를 임의로 세팅할 수 있어 불일치 위험이 큼.
     * 사용처가 남아있다면 setCurrentPage / setTotalPages로 점진적으로 치환한 뒤 제거하세요.
     */
//    public void updateProgress(Integer currentPage, Integer progressPercent) {
//        this.currentPage = currentPage;
//        this.progressPercent = (progressPercent != null) ? progressPercent : this.progressPercent;
//    }
}

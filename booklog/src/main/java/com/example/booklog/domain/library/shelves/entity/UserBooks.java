package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
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
    private ReadingStatus status; // TO_READ/READING/DONE/STOPPED

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    //도서 저장/상태 변경을 위한 currentPage
    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 20)
    private BookFormat format; // PAPER/EBOOK/AUDIO 등

    @Column(name = "page_count_snapshot")//유저가 설정한 총 페이지 수(판본/개인 기준). 진행률 계산 기준값
    private Integer pageCountSnapshot;

    @Builder
    public UserBooks(Users user, Books book, ReadingStatus status) {
        this.user = user;
        this.book = book;
        this.status = (status != null) ? status : ReadingStatus.TO_READ;
        this.progressPercent = 0;
    }

    public void updateStatus(ReadingStatus status) { this.status = status; }

    public void updateProgress(Integer currentPage, Integer progressPercent) {
        this.currentPage = currentPage;
        this.progressPercent = (progressPercent != null) ? progressPercent : this.progressPercent;
    }

    public void setStartDateIfNull(LocalDate date) { if (this.startDate == null) this.startDate = date; }

    public void setEndDate(LocalDate date) { this.endDate = date; }

    /*
    아래 메소드 3개는 사용자의 페이지 입력관련 메소드
     */
    public void updatePageCountSnapshot(Integer totalPage) {
        if (totalPage == null || totalPage < 1) throw new IllegalArgumentException("총 페이지는 1 이상이어야 합니다.");
        this.pageCountSnapshot = totalPage;
        recalcProgressPercent();
    }

    private void recalcProgressPercent() {
        if (currentPage == null || pageCountSnapshot == null || pageCountSnapshot <= 0) {
            this.progressPercent = 0;
            return;
        }
        this.progressPercent = (int)Math.min(100, Math.round(currentPage * 100.0 / pageCountSnapshot));
    }

    public void updateProgress(Integer currentPage) {
        this.currentPage = currentPage;
        recalcProgressPercent();
    }

    public void updateFormat(BookFormat format) {
        this.format = format;
    }


}

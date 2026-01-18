package com.example.booklog.domain.library.shelves.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reading_logs_user_book"))
    private UserBooks userBook;

    @Column(name = "log_date", nullable = false) // read_date 캘런더 날짜 키
    private LocalDate logDate;

    @Column(name = "pages_read") //그 날 읽은 페이지
    private Integer pagesRead;

    @Column(name = "status_snapshot", length = 20) //status(기록 시점 상태 스냅샷. "그날은 읽는 중 이였다" 등 이력)
    private String statusSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ReadingLogs(UserBooks userBook, LocalDate logDate, Integer pagesRead, String statusSnapshot) {
        this.userBook = userBook;
        this.logDate = logDate != null ? logDate : LocalDate.now();
        this.pagesRead = pagesRead;
        this.statusSnapshot = statusSnapshot;
        this.createdAt = LocalDateTime.now();
    }

    public void updatePagesRead(Integer pagesRead) {
        this.pagesRead = pagesRead;
    }
}


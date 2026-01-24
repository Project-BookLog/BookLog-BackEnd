package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "reading_logs",
        indexes = {
                @Index(name = "idx_reading_logs_userbook_created", columnList = "user_book_id, created_at"),
                @Index(name = "idx_reading_logs_userbook_date", columnList = "user_book_id, read_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReadingLogs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_book_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reading_logs_user_book"))
    private UserBooks userBook;

    @Column(name = "read_date", nullable = false)
    private LocalDate readDate;

    /** 요청 스펙 currentPage 그대로 저장 */
    //누적 현재 페이지
    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    /** 그날 읽은 페이지(달력에 표시할 값) */
    @Column(name = "pages_read", nullable = false)
    private Integer pagesRead;

    //수정 메서드
    public void update(LocalDate readDate, Integer pagesRead, Integer currentPage) {
        this.readDate = readDate;
        this.pagesRead = pagesRead;
        this.currentPage = currentPage;
    }

}

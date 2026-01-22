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

    @Column(name = "status", length = 20, nullable = false)
    private String status; // TO_READ/READING/DONE/STOPPED

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "format", length = 20)
    private String format; // PAPER/EBOOK/AUDIO 등

    @Column(name = "page_count_snapshot")
    private Integer pageCountSnapshot;

    @Builder
    public UserBooks(Users user, Books book, String status) {
        this.user = user;
        this.book = book;
        this.status = (status != null) ? status : "TO_READ";
        this.progressPercent = 0;
    }

    public void updateStatus(String status) { this.status = status; }
    public void updateProgress(Integer currentPage, Integer progressPercent) {
        this.currentPage = currentPage;
        this.progressPercent = (progressPercent != null) ? progressPercent : this.progressPercent;
    }
    public void setStartDateIfNull(LocalDate date) { if (this.startDate == null) this.startDate = date; }
    public void setEndDate(LocalDate date) { this.endDate = date; }
}

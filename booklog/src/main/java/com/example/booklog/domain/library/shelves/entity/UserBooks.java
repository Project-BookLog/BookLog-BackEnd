package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_books", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_book", columnNames = {"user_id", "book_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBooks extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_books_user"))
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_books_book"))
    private Books book;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReadingStatus status;

    @Column(name = "progress", nullable = false) //기존 progress_percent 진행률
    private Integer progress = 0;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20)
    private MediaType mediaType;

    @Builder
    public UserBooks(Users user, Books book, ReadingStatus status,
                     Integer currentPage, Integer totalPages,
                     LocalDate startedAt, MediaType mediaType) {
        this.user = user;
        this.book = book;
        this.status = status != null ? status : ReadingStatus.PLANNING;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.startedAt = startedAt;
        this.mediaType = mediaType;
        this.progress = calculateProgress();
    }

    public void updateReadingProgress(Integer currentPage, Integer totalPages) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.progress = calculateProgress();
    }

    public void updateStatus(ReadingStatus status) {
        this.status = status;
        if (status == ReadingStatus.READING && this.startedAt == null) {
            this.startedAt = LocalDate.now();
        } else if (status == ReadingStatus.COMPLETED) {
            this.completedAt = LocalDate.now();
            this.progress = 100;
        }
    }

    private Integer calculateProgress() {
        if (currentPage == null || totalPages == null || totalPages == 0) {
            return 0;
        }
        return Math.min(100, (currentPage * 100) / totalPages);
    }
}


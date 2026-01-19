package com.example.booklog.domain.library.shelves.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "shelf_books")
@IdClass(ShelfBooks.ShelfBookId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelfBooks {

    @Id
    @Column(name = "shelf_id")
    private Long shelfId;

    @Id
    @Column(name = "user_book_id")
    private Long userBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_shelf_books_shelf"))
    private Shelves shelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_shelf_books_user_book"))
    private UserBooks userBook;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Builder
    public ShelfBooks(Shelves shelf, UserBooks userBook) {
        this.shelfId = shelf.getId();
        this.userBookId = userBook.getId();
        this.shelf = shelf;
        this.userBook = userBook;
        this.addedAt = LocalDateTime.now();
    }

    // 복합키 클래스
    public static class ShelfBookId implements Serializable {
        private Long shelfId;
        private Long userBookId;

        public ShelfBookId() {}

        public ShelfBookId(Long shelfId, Long userBookId) {
            this.shelfId = shelfId;
            this.userBookId = userBookId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShelfBookId that = (ShelfBookId) o;
            return Objects.equals(shelfId, that.shelfId) &&
                   Objects.equals(userBookId, that.userBookId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shelfId, userBookId);
        }
    }
}


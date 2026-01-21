package com.example.booklog.domain.library.books.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "book_authors")
@IdClass(BookAuthors.BookAuthorId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookAuthors {

    @Id
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Id
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private AuthorRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_book_authors_book")
    )
    private Books book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "author_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_book_authors_author")
    )
    private Authors author;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public BookAuthors(Books book, Authors author, AuthorRole role, Integer displayOrder) {
        this.role = role;
        this.displayOrder = (displayOrder != null) ? displayOrder : 0;

        // 연관관계 세팅 + FK/PK 필드(bookId/authorId) 동기화
        if (book != null) setBook(book);
        if (author != null) setAuthor(author);
    }

    public void setBook(Books book) {
        this.book = book;
        if (book != null && book.getId() != null) {
            this.bookId = book.getId();
        }
    }

    public void setAuthor(Authors author) {
        this.author = author;
        if (author != null && author.getId() != null) {
            this.authorId = author.getId();
        }
    }

    // 복합키 클래스
    public static class BookAuthorId implements Serializable {
        private Long bookId;
        private Long authorId;
        private AuthorRole role;

        public BookAuthorId() {}

        public BookAuthorId(Long bookId, Long authorId, AuthorRole role) {
            this.bookId = bookId;
            this.authorId = authorId;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookAuthorId that = (BookAuthorId) o;
            return Objects.equals(bookId, that.bookId)
                    && Objects.equals(authorId, that.authorId)
                    && role == that.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookId, authorId, role);
        }
    }
}

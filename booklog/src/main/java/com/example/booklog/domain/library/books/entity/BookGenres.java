package com.example.booklog.domain.library.books.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "book_genres")
@IdClass(BookGenres.BookGenreId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookGenres {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Id
    @Column(name = "genre_id")
    private Long genreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_book_genres_book"))
    private Books book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_book_genres_genre"))
    private Genres genre;

    @Builder
    public BookGenres(Books book, Genres genre) {
        this.bookId = book.getId();
        this.genreId = genre.getId();
        this.book = book;
        this.genre = genre;
    }

    // 복합키 클래스
    public static class BookGenreId implements Serializable {
        private Long bookId;
        private Long genreId;

        public BookGenreId() {}

        public BookGenreId(Long bookId, Long genreId) {
            this.bookId = bookId;
            this.genreId = genreId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookGenreId that = (BookGenreId) o;
            return Objects.equals(bookId, that.bookId) &&
                   Objects.equals(genreId, that.genreId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookId, genreId);
        }
    }
}


package com.example.booklog.domain.library.books.entity.mapping;


import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.entity.common.Genres;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "book_genres",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_book_genres_book_genre",
                        columnNames = {"book_id", "genre_id"}
                )
        },
        indexes = {
                @Index(name = "idx_book_genres_book", columnList = "book_id"),
                @Index(name = "idx_book_genres_genre", columnList = "genre_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BookGenres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_genre_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Books book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genres genre;

    public void setBook(Books book) {
        this.book = book;
    }
}

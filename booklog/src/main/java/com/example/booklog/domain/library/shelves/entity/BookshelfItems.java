package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.library.books.entity.Books;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bookshelf_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bookshelf_items",
                columnNames = {"shelf_id", "book_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookshelfItems {

    @EmbeddedId
    private BookshelfItemId id;

    @MapsId("shelfId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "shelf_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookshelf_items_shelf")
    )
    private Bookshelves shelf;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "book_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookshelf_items_book")
    )
    private Books book;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public BookshelfItems(Bookshelves shelf, Books book) {
        this.shelf = shelf;
        this.book = book;
        this.id = new BookshelfItemId(shelf.getId(), book.getId());
        this.addedAt = LocalDateTime.now();
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class BookshelfItemId implements Serializable {
        @Column(name = "shelf_id") private Long shelfId;
        @Column(name = "book_id") private Long bookId;
    }
}

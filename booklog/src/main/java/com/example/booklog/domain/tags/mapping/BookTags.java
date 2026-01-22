package com.example.booklog.domain.tags.mapping;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.tags.entity.Tags;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(
        name = "book_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_tags", columnNames = {"book_id", "tag_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookTags {

    @EmbeddedId
    private BookTagId id;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_book_tags_book"))
    private Books book;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_book_tags_tag"))
    private Tags tag;

    public BookTags(Books book, Tags tag) {
        this.id = new BookTagId(book.getId(), tag.getId());
        this.book = book;
        this.tag = tag;
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookTagId implements Serializable {
        @Column(name = "book_id") private Long bookId;
        @Column(name = "tag_id") private Long tagId;
    }
}

package com.example.booklog.domain.library.books.entity.mapping;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.entity.common.AuthorRole;
import com.example.booklog.domain.library.books.entity.common.Authors;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "book_authors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_book_authors_book_author_role",
                        columnNames = {"book_id", "author_id", "role"}
                )
        },
        indexes = {
                @Index(name = "idx_book_authors_book", columnList = "book_id"),
                @Index(name = "idx_book_authors_author", columnList = "author_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BookAuthors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_author_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Books book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Authors author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthorRole role;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // Books 쪽에서 convenience method로 세팅하려고 열어둠
    public void setBook(Books book) {
        this.book = book;
    }
}


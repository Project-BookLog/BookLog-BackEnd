package com.example.booklog.domain.booklog.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "booklog_bookmark",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
public class BooklogBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    protected BooklogBookmark() {}

    private BooklogBookmark(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public static BooklogBookmark of(Long userId, Long postId) {
        return new BooklogBookmark(userId, postId);
    }
}
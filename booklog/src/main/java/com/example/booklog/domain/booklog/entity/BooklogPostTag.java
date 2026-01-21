package com.example.booklog.domain.booklog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "booklog_post_tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_tag", columnNames = {"postId", "tagId"})
        },
        indexes = {
                @Index(name = "idx_post_tag_post", columnList = "postId"),
                @Index(name = "idx_post_tag_tag", columnList = "tagId")
        }
)
public class BooklogPostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // BooklogPost.id (FK)
    @Column(nullable = false)
    private Long postId;

    // Tag.id (FK)
    @Column(nullable = false)
    private Long tagId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private BooklogPostTag(Long postId, Long tagId) {
        this.postId = postId;
        this.tagId = tagId;
        this.createdAt = LocalDateTime.now();
    }

    public static BooklogPostTag of(Long postId, Long tagId) {
        return new BooklogPostTag(postId, tagId);
    }
}

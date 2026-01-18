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
        name = "booklog_post_images",
        indexes = {
                @Index(name = "idx_post_image_post", columnList = "postId")
        }
)
public class BooklogPostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // BooklogPost.id (FK)
    @Column(nullable = false)
    private Long postId;

    // 실제 이미지 접근 URL (S3 등)
    @Column(nullable = false, length = 500)
    private String imageUrl;

    // 이미지 순서 (0,1,2...)
    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private BooklogPostImage(Long postId, String imageUrl, int displayOrder) {
        this.postId = postId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.createdAt = LocalDateTime.now();
    }

    public static BooklogPostImage of(Long postId, String imageUrl, int order) {
        return new BooklogPostImage(postId, imageUrl, order);
    }
}
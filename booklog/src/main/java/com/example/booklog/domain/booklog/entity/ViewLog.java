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
        name = "view_logs",
        indexes = {
                @Index(name = "idx_view_logs_post_created", columnList = "postId, createdAt"),
                @Index(name = "idx_view_logs_user_created", columnList = "userId, createdAt")
        }
)
public class ViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ViewLog(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public static ViewLog of(Long postId, Long userId) {
        return new ViewLog(postId, userId);
    }
}

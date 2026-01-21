package com.example.booklog.domain.booklog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "booklog_posts",
        indexes = {
                @Index(name = "idx_booklog_posts_user_created", columnList = "userId, createdAt"),
                @Index(name = "idx_booklog_posts_status_created", columnList = "status, createdAt")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BooklogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    //피드 작성자 id (FK)
    @Column(nullable = false)
    private Long userId;

    //작성 책 id (FK)
    @Column(nullable = false)
    private Long bookId;


    // 당장은 필요 없으나 추후에 알람이나 기능을 추가할 때 용이하다.
    @Column(length = 100)
    private String title;

    @Lob // db에 맞게 긴 텍스트 타입으로 매핑
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BooklogStatus status;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long commentCount;

    @Column(nullable = false)
    private long bookmarkCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private BooklogPost(Long userId, Long bookId, String title, String content) {
        this.userId = userId;
        this.bookId = bookId;
        this.title = title;
        this.content = content;
        this.status = BooklogStatus.PUBLISHED;
        this.viewCount = 0;
        this.commentCount = 0;
        this.bookmarkCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    public static BooklogPost publish(Long userId, Long bookId, String title, String content) {
        return new BooklogPost(userId, bookId, title, content); // new를 썼을 때보다 직관적으로 파악하기 좋다.
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}

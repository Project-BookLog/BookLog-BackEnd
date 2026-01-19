package com.example.booklog.domain.posts.entity;

import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_views", indexes = {
    @Index(name = "idx_pv_post_time", columnList = "post_id, viewed_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_views_post"))
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_post_views_user"))
    private Users user;

    @Column(name = "session_id", length = 100) //세션 id로 조회 추적함
    private String sessionId;

    @Column(name = "viewed_at", nullable = false) //조회 시점 기록
    private LocalDateTime viewedAt;

    @Builder
    public PostViews(Posts post, Users user, String sessionId) {
        this.post = post;
        this.user = user;
        this.sessionId = sessionId;
        this.viewedAt = LocalDateTime.now();
    }
}


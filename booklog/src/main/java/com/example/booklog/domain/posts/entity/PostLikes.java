package com.example.booklog.domain.posts.entity;

import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "post_likes")
@IdClass(PostLikes.PostLikeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikes {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_post_likes_post"))
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_post_likes_user"))
    private Users user;

    @Column(name = "liked_at", nullable = false)
    private LocalDateTime likedAt;

    @Builder
    public PostLikes(Posts post, Users user) {
        this.postId = post.getId();
        this.userId = user.getId();
        this.post = post;
        this.user = user;
        this.likedAt = LocalDateTime.now();
    }

    // 복합키 클래스
    public static class PostLikeId implements Serializable {
        private Long postId;
        private Long userId;

        public PostLikeId() {}

        public PostLikeId(Long postId, Long userId) {
            this.postId = postId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostLikeId that = (PostLikeId) o;
            return Objects.equals(postId, that.postId) &&
                   Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, userId);
        }
    }
}


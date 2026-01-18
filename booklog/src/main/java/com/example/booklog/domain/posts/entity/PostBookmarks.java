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
@Table(name = "post_bookmarks")
@IdClass(PostBookmarks.PostBookmarkId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmarks {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_post_bookmarks_post"))
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_post_bookmarks_user"))
    private Users user;

    @Column(name = "bookmarked_at", nullable = false) //post_bookmarks 테이블 기준, created_at (북마크 시점)을 의미함
    private LocalDateTime bookmarkedAt;

    @Builder
    public PostBookmarks(Posts post, Users user) {
        this.postId = post.getId();
        this.userId = user.getId();
        this.post = post;
        this.user = user;
        this.bookmarkedAt = LocalDateTime.now();
    }

    // 복합키 클래스
    public static class PostBookmarkId implements Serializable {
        private Long postId;
        private Long userId;

        public PostBookmarkId() {}

        public PostBookmarkId(Long postId, Long userId) {
            this.postId = postId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostBookmarkId that = (PostBookmarkId) o;
            return Objects.equals(postId, that.postId) &&
                   Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, userId);
        }
    }
}


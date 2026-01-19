package com.example.booklog.domain.users.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_follows") //세부적인 구현은 API 설계전에 훑어보면 좋을 것 같습니다
@IdClass(UserFollows.UserFollowId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFollows {

    @Id
    @Column(name = "follower_id") //팔로우 하는 사람
    private Long followerId;

    @Id
    @Column(name = "followee_id") //팔로우 당하는 사람
    private Long followeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_user_follows_follower"))
    private Users follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_user_follows_followee"))
    private Users followee;

    @Column(name = "followed_at", nullable = false)
    private LocalDateTime followedAt;

    @Builder
    public UserFollows(Users follower, Users followee) {
        this.followerId = follower.getId();
        this.followeeId = followee.getId();
        this.follower = follower;
        this.followee = followee;
        this.followedAt = LocalDateTime.now();
    }

    // 복합키 클래스
    public static class UserFollowId implements Serializable {
        private Long followerId;
        private Long followeeId;

        public UserFollowId() {}

        public UserFollowId(Long followerId, Long followeeId) {
            this.followerId = followerId;
            this.followeeId = followeeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserFollowId that = (UserFollowId) o;
            return Objects.equals(followerId, that.followerId) &&
                   Objects.equals(followeeId, that.followeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followeeId);
        }
    }
}


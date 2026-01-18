package com.example.booklog.domain.users.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_settings_user"))
    private Users user;

    @Column(name = "is_shelf_public", nullable = false) //서재 공개 토글
    private Boolean isShelfPublic = true;

    @Column(name = "is_post_public", nullable = false) //북로그 공개 토글
    private Boolean isPostPublic = true;

    @Column(name = "updated_at", nullable = false) //설정 변경 시점 추적
    private LocalDateTime updatedAt;

    @Builder
    public UserSettings(Users user, Boolean isShelfPublic, Boolean isPostPublic) {
        this.user = user;
        this.isShelfPublic = isShelfPublic != null ? isShelfPublic : true;
        this.isPostPublic = isPostPublic != null ? isPostPublic : true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateShelfPublic(Boolean isPublic) {
        this.isShelfPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePostPublic(Boolean isPublic) {
        this.isPostPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }
}


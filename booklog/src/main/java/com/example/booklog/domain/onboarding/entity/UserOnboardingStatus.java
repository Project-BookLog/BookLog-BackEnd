package com.example.booklog.domain.onboarding.entity;

import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_onboarding_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingStatus {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_onboarding_status_user"))
    private Users user;

    @Column(name = "is_completed", nullable = false) //완료 여부(온보딩 재진입, 스킵 처리에 필요함)
    private Boolean isCompleted = false;

    @Column(name = "is_skipped", nullable = false) //스킵 여부
    private Boolean isSkipped = false;

    @Column(name = "completed_at") //완료 시점
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserOnboardingStatus(Users user, Boolean isCompleted, Boolean isSkipped) {
        this.user = user;
        // @MapsId를 사용하므로 userId는 JPA가 자동으로 설정
        this.isCompleted = isCompleted != null ? isCompleted : false;
        this.isSkipped = isSkipped != null ? isSkipped : false;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void skip() {
        this.isSkipped = true;
        this.updatedAt = LocalDateTime.now();
    }
}


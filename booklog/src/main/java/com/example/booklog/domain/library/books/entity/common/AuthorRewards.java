package com.example.booklog.domain.library.books.entity.common;

import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "author_rewards",
        indexes = {
                @Index(name = "idx_author_rewards_author", columnList = "author_id"),
                @Index(name = "idx_author_rewards_qid", columnList = "reward_qid")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_author_rewards_dedupe",
                        columnNames = {"author_id", "reward_qid", "reward_year", "work_qid"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuthorRewards extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    private Long rewardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Authors author;

    @Column(name = "reward_name", nullable = false, length = 255)
    private String rewardName;

    @Column(name = "reward_qid", length = 32)
    private String rewardQid;

    @Column(name = "work_title", length = 255)
    private String workTitle;

    @Column(name = "work_qid", length = 32)
    private String workQid;

    @Column(name = "reward_year")
    private Integer rewardYear;

    @Column(name = "reward_date")
    private LocalDateTime rewardDate;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "reward_rank", length = 50)
    private String rewardRank;

    @Column(name = "given_by", length = 200)
    private String givenBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private Source source;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public enum Source {
        WIKIDATA, MANUAL
    }

    public void touchSyncedNow() {
        this.lastSyncedAt = LocalDateTime.now();
    }
}

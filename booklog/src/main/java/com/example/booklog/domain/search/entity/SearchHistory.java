package com.example.booklog.domain.search.entity;

import com.example.booklog.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


//프론트엔드에서 localStorage로 처리할 수 있는 기능
// -> 따라서, DB에 저장하는 것이 필수가 아니기에, 우선 보류합니다.
@Entity
@Table(name = "search_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_search_history_user"))
    private Users user;

    @Column(name = "keyword", length = 100, nullable = false)
    private String keyword;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    @Builder
    public SearchHistory(Users user, String keyword) {
        this.user = user;
        this.keyword = keyword;
        this.searchedAt = LocalDateTime.now();
    }
}


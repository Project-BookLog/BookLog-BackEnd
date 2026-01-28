package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "bookshelves",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookshelves_user_name", columnNames = {"user_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookshelves extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shelf_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookshelves_user"))
    private Users user;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_order", length = 20, nullable = false)
    private UserBookSort sortOrder; // ERD: VARCHAR(20)

    @Builder
    public Bookshelves(Users user, String name, Boolean isPublic, UserBookSort sortOrder) {
        this.user = user;
        this.name = name;
        this.isPublic = (isPublic != null) ? isPublic : false;
        this.sortOrder = (sortOrder != null) ? sortOrder : UserBookSort.LATEST;
    }

    public void updateName(String name) { this.name = name; }
    public void updatePublic(boolean isPublic) { this.isPublic = isPublic; }
    public void updateSortOrder(UserBookSort sortOrder) { this.sortOrder = sortOrder; }
}

package com.example.booklog.domain.library.shelves.entity;

import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shelves", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_shelf_name", columnNames = {"user_id", "name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shelves extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shelves_user"))
    private Users user;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "display_order", nullable = false) //사용자 커스텀 정렬
    private Integer displayOrder = 0;

    @Builder
    public Shelves(Users user, String name, Boolean isPublic, Integer displayOrder) {
        this.user = user;
        this.name = name;
        this.isPublic = isPublic != null ? isPublic : true;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}


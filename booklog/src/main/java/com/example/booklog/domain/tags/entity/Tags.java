package com.example.booklog.domain.tags.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tag_category_name", columnNames = {"category", "name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private TagCategory category;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Builder
    public Tags(TagCategory category, String name) {
        this.category = category;
        this.name = name;
    }
}


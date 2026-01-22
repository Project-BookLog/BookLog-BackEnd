package com.example.booklog.domain.tags.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_category_name", columnNames = {"category", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private TagCategory category; // MOOD/STYLE/IMMERSION

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Builder
    public Tags(TagCategory category, String name) {
        this.category = category;
        this.name = name;
    }
}

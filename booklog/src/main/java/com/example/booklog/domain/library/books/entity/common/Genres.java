package com.example.booklog.domain.library.books.entity.common;

import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "genres",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_genres_name", columnNames = {"name"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Genres extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}

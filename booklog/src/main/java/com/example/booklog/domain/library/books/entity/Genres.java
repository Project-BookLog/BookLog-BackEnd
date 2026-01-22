package com.example.booklog.domain.library.books.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "genres", uniqueConstraints = {
    @UniqueConstraint(name = "uk_genre_name", columnNames = {"name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genres_id")
    private Long id;

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Builder
    public Genres(String name) {
        this.name = name;
    }
}


package com.example.booklog.domain.library.books.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "author_awards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorAwards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "author_award_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_author_awards_author"))
    private Authors author;

    @Column(name = "year")
    private Integer year;

    @Column(name = "award_name", length = 255, nullable = false)
    private String awardName;

    @Column(name = "work_title", length = 255) //수상작
    private String workTitle;

    @Builder
    public AuthorAwards(Authors author, Integer year, String awardName, String workTitle) {
        this.author = author;
        this.year = year;
        this.awardName = awardName;
        this.workTitle = workTitle;
    }
}


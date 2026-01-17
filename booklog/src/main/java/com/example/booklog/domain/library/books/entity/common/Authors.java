// 파일: src/main/java/com/example/booklog/domain/library/books/entity/common/Authors.java
package com.example.booklog.domain.library.books.entity.common;

import com.example.booklog.domain.library.books.dto.AuthorWikidataEnrichment;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "authors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_authors_name", columnNames = {"name"}),
                @UniqueConstraint(name = "uk_authors_wikidata_id", columnNames = {"wikidata_id"})
        },
        indexes = {
                @Index(name = "idx_authors_name", columnList = "name"),
                @Index(name = "idx_authors_wikidata_id", columnList = "wikidata_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Authors extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "author_id")
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Lob
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 100)
    private String nationality;

    @Column(name = "short_intro", length = 100)
    private String shortIntro;

    @Column(name = "debut_year")
    private Integer debutYear;

    @Column(name = "debut_work_title", length = 100)
    private String debutWorkTitle;

    // ✅ ERD와 일치: VARCHAR(100) / 값은 "Q12345" 형태 그대로 저장
    @Column(name = "wikidata_id", length = 100)
    private String wikidataId;

    @Lob
    @Column(name = "wikidata_raw_json")
    private String wikidataRawJson;

    public boolean hasWikidataId() {
        return wikidataId != null && !wikidataId.isBlank();
    }

    public static Authors ofName(String name) {
        return Authors.builder()
                .name(name == null ? "" : name.trim())
                .build();
    }

    public void applyWikidataEnrichment(AuthorWikidataEnrichment e) {
        if (e == null) return;

        if (e.profileImageUrl() != null) this.profileImageUrl = e.profileImageUrl();
        if (e.bio() != null) this.bio = e.bio();
        if (e.birthDate() != null) this.birthDate = e.birthDate();
        if (e.nationality() != null) this.nationality = e.nationality();
        if (e.shortIntro() != null) this.shortIntro = e.shortIntro();
        if (e.debutYear() != null) this.debutYear = e.debutYear();
        if (e.debutWorkTitle() != null) this.debutWorkTitle = e.debutWorkTitle();

        // ✅ QID는 Long 변환하지 말고 "Q12345" 문자열로 저장
        if (e.wikidataQid() != null && !e.wikidataQid().isBlank()) {
            this.wikidataId = normalizeQid(e.wikidataQid());
        }

        if (e.rawJson() != null) {
            this.wikidataRawJson = e.rawJson();
        }
    }

    private String normalizeQid(String qid) {
        if (qid == null) return null;
        String s = qid.trim();
        if (s.isBlank()) return null;
        if (!s.startsWith("Q")) s = "Q" + s; // "123" 들어오면 "Q123"로 보정
        return s;
    }
}

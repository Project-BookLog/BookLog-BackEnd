package com.example.booklog.domain.library.books.entity;

import com.example.booklog.domain.library.books.dto.AuthorWikidataEnrichment;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "authors") // 추후 수정될 가능성이 있습니다. (작가 정보 찾는 로직)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Authors extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "author_id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "biography", columnDefinition = "TEXT") // 긴 소개
    private String biography;

    @Column(name = "wikidata_id", length = 20)
    private String wikidataId;

    @Column(name = "wikidata_raw_json", columnDefinition = "TEXT")
    private String wikidataRawJson;

    // GPT로 보완된 프로필 정보
    @Column(name = "profile_json", columnDefinition = "TEXT")
    private String profileJson; // JSON 형식으로 education, debut, birthDate, occupations 저장

    @Builder
    public Authors(String name, String profileImageUrl, String biography, String wikidataId, String wikidataRawJson, String profileJson) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.biography = biography;
        this.wikidataId = wikidataId;
        this.wikidataRawJson = wikidataRawJson;
        this.profileJson = profileJson;
    }

    public static Authors ofName(String name) {
        return Authors.builder()
                .name(name)
                .build();
    }

    public void updateProfile(String profileImageUrl, String biography) {
        this.profileImageUrl = profileImageUrl;
        this.biography = biography;
    }

    public void updateProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }

    public boolean hasWikidataId() {
        return this.wikidataId != null && !this.wikidataId.isBlank();
    }

    public void applyWikidataEnrichment(AuthorWikidataEnrichment enrichment) {
        if (enrichment == null) return;

        this.wikidataId = enrichment.wikidataQid();
        this.wikidataRawJson = enrichment.rawJson();

        // profileImageUrl이 없으면 위키데이터의 이미지로 채움
        if ((this.profileImageUrl == null || this.profileImageUrl.isBlank()) && enrichment.profileImageUrl() != null) {
            this.profileImageUrl = enrichment.profileImageUrl();
        }

        // biography가 없으면 bio로 채움
        if ((this.biography == null || this.biography.isBlank()) && enrichment.bio() != null) {
            this.biography = enrichment.bio();
        }
    }
}


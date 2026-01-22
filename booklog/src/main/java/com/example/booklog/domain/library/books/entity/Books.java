package com.example.booklog.domain.library.books.entity;

import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "books",
        indexes = {
                @Index(name = "idx_books_title", columnList = "title"),
                @Index(name = "idx_books_isbn13", columnList = "isbn13")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Books extends BaseEntity {

    /** ERD: book_id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /**
     * 너는 "description"이라는 도메인 이름을 쓰고 싶음.
     * ERD: contents
     */
    @Column(name = "contents", columnDefinition = "TEXT")
    private String description;

    /**
     * 너는 "detailUrl"을 쓰고 싶음.
     * ERD: kakao_url
     */
    @Column(name = "kakao_url", length = 500)
    private String detailUrl;

    /**
     * 너는 isbn 원문을 "isbn"으로 쓰고 싶음.
     * ERD: isbn_raw
     */
    @Column(name = "isbn_raw", length = 40)
    private String isbn;

    @Column(name = "isbn10", length = 20)
    private String isbn10;

    @Column(name = "isbn13", length = 20)
    private String isbn13;

    /** ERD: published_at (DATE) */
    @Column(name = "published_at")
    private LocalDate publishedDate;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** ERD: publisher_name */
    @Column(name = "publisher_name", length = 100)
    private String publisherName;

    /**
     * ERD: source (VARCHAR)
     * 너는 enum(BookSource)을 쓰고 있어서 STRING 저장 가능.
     * 단, ERD가 문자열이면 enum name 그대로 들어가도 괜찮음.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30, nullable = false)
    private BookSource source = BookSource.KAKAO;

    /**
     * 너는 rawData(String JSON)을 쓰고 싶음.
     * ERD: raw_json
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "JSON")
    private String rawData;

    /**
     * 너는 lastSyncedAt을 쓰고 싶음.
     * ERD: synced_at
     */
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    /** ERD에는 book_authors 매핑이 있을 가능성이 높으니 유지 */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookAuthors> bookAuthors = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (lastSyncedAt == null) lastSyncedAt = LocalDateTime.now();
    }

    @Builder
    public Books(String title, String description, String detailUrl,
                 String isbn, String isbn10, String isbn13,
                 LocalDate publishedDate, String thumbnailUrl,
                 String publisherName, BookSource source, String rawData) {
        this.title = title;
        this.description = description;
        this.detailUrl = detailUrl;
        this.isbn = isbn;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.publishedDate = publishedDate;
        this.thumbnailUrl = thumbnailUrl;
        this.publisherName = publisherName;
        this.source = (source != null) ? source : BookSource.KAKAO;
        this.rawData = rawData;
        this.lastSyncedAt = LocalDateTime.now();
    }

    /** 업데이트 시도 동일 (필드명은 네 스타일 유지) */
    public void updateBasicInfo(String title, String description, String thumbnailUrl,
                                String detailUrl, String publisherName, LocalDate publishedDate,
                                String isbn, String isbn10, String isbn13, String rawData) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.detailUrl = detailUrl;
        this.publisherName = publisherName;
        this.publishedDate = publishedDate;
        this.isbn = isbn;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.rawData = rawData;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void replaceBookAuthors(List<BookAuthors> newMappings) {
        this.bookAuthors.clear();
        for (BookAuthors mapping : newMappings) {
            mapping.setBook(this);
            this.bookAuthors.add(mapping);
        }
    }
}

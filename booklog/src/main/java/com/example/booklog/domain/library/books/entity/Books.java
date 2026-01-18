package com.example.booklog.domain.library.books.entity;

import com.example.booklog.domain.library.books.entity.mapping.BookAuthors;
import com.example.booklog.domain.library.books.entity.mapping.BookGenres;
import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
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
                @Index(name = "idx_books_isbn13", columnList = "isbn13"),
                @Index(name = "idx_books_title", columnList = "title"),
                @Index(name = "idx_books_kakao_url", columnList = "kakao_url")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_books_isbn13", columnNames = {"isbn13"})
                // isbn13 없는 경우 중복 방지하려면 아래도 추천
                // , @UniqueConstraint(name = "uk_books_kakao_url", columnNames = {"kakao_url"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Books extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    private String contents;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "kakao_url", length = 500)
    private String kakaoUrl;

    // ✅ (ERD가 100이면 100으로) / 너가 200 유지 원하면 200으로 두면 됨
    @Column(name = "publisher_name", length = 100)
    private String publisherName;

    // ✅ datetime 그대로: DATETIME
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "isbn_raw", length = 40)
    private String isbnRaw;

    @Column(name = "isbn10", length = 20)
    private String isbn10;

    @Column(name = "isbn13", length = 20)
    private String isbn13;

    // ✅ 여기 3개는 “업데이트 ERD에 존재하면 유지”, 없으면 삭제해야 함
    @Column(name = "price")
    private Integer price;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "sale_status", length = 50)
    private String saleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private BookSource source;

    // ✅ ERD가 JSON이면 아래처럼(문자열로 저장하지만 MySQL json 컬럼)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "json")
    private String rawJson;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookAuthors> bookAuthors = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookGenres> bookGenres = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (syncedAt == null) syncedAt = LocalDateTime.now();
    }

    public void updateBasicInfo(
            String title,
            String contents,
            String thumbnailUrl,
            String kakaoUrl,
            String publisherName,
            LocalDateTime publishedAt,
            String isbnRaw,
            String isbn10,
            String isbn13,
            Integer price,
            Integer salePrice,
            String saleStatus,
            String rawJson
    ) {
        this.title = title;
        this.contents = contents;
        this.thumbnailUrl = thumbnailUrl;
        this.kakaoUrl = kakaoUrl;
        this.publisherName = publisherName;
        this.publishedAt = publishedAt;
        this.isbnRaw = isbnRaw;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.price = price;
        this.salePrice = salePrice;
        this.saleStatus = saleStatus;
        this.rawJson = rawJson;
        this.syncedAt = LocalDateTime.now();
    }

    public void addBookAuthor(BookAuthors bookAuthor) {
        this.bookAuthors.add(bookAuthor);
        bookAuthor.setBook(this);
    }

    public void replaceBookAuthors(List<BookAuthors> newMappings) {
        // ✅ orphan removal을 위해 기존 매핑 clear
        this.bookAuthors.clear();

        // ✅ 새로운 매핑 추가
        for (BookAuthors m : newMappings) {
            addBookAuthor(m);
        }
    }

    public void addBookGenre(BookGenres bookGenre) {
        this.bookGenres.add(bookGenre);
        bookGenre.setBook(this);
    }

    public enum BookSource { KAKAO }
}

package com.example.booklog.domain.library.books.entity;

import com.example.booklog.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Books extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT") //기존 contents(책 소개/요약 표시)를 description으로 바꿈
    private String description;

    @Column(name = "detail_url", length = 500) //kako_url(책 상세 링크)입니다
    private String detailUrl;

    @Column(name = "isbn", length = 40) //isbn_raw(원문 보관용)
    private String isbn;

    @Column(name = "isbn10", length = 20)
    private String isbn10;

    @Column(name = "isbn13", length = 20)
    private String isbn13;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "status", length = 50)
    private String status; // 정상/품절/절판

    @Column(name = "price")
    private Integer price;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "publisher_name", length = 255)
    private String publisherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30, nullable = false)
    private BookSource source = BookSource.KAKAO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "JSON") //카카오 응답 원본 보관
    private String rawData;

    @Column(name = "last_synced_at", nullable = false) //캐시 갱신 시점
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookAuthors> bookAuthors = new ArrayList<>();

    @Builder
    public Books(String title, String description, String detailUrl,
                 String isbn, String isbn10, String isbn13,
                 LocalDateTime publishedDate, String thumbnailUrl,
                 String status, Integer price, Integer salePrice,
                 Long publisherId, String publisherName, BookSource source, String rawData) {
        this.title = title;
        this.description = description;
        this.detailUrl = detailUrl;
        this.isbn = isbn;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.publishedDate = publishedDate;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.price = price;
        this.salePrice = salePrice;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        this.source = source != null ? source : BookSource.KAKAO;
        this.rawData = rawData;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public LocalDateTime getPublishedAt() {
        return this.publishedDate;
    }

    public void updateSyncTime() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void updateBookInfo(String title, String description, String thumbnailUrl,
                               Integer price, Integer salePrice, String status) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.salePrice = salePrice;
        this.status = status;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void updateBasicInfo(String title, String description, String thumbnailUrl,
                                String detailUrl, String publisher, LocalDateTime publishedDate,
                                String isbn, String isbn10, String isbn13,
                                Integer price, Integer salePrice, String status, String rawData) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.detailUrl = detailUrl;
        this.isbn = isbn;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.publishedDate = publishedDate;
        this.price = price;
        this.salePrice = salePrice;
        this.status = status;
        this.rawData = rawData;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public List<BookAuthors> getBookAuthors() {
        return bookAuthors;
    }

    public void replaceBookAuthors(List<BookAuthors> newMappings) {
        this.bookAuthors.clear();
        for (BookAuthors mapping : newMappings) {
            mapping.setBook(this);
            this.bookAuthors.add(mapping);
        }
    }
}


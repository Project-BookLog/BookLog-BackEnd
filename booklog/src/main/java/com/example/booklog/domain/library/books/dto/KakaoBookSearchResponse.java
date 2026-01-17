package com.example.booklog.domain.library.books.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoBookSearchResponse {

    private Meta meta;
    private List<Document> documents;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("is_end")
        private boolean isEnd;
        @JsonProperty("pageable_count")
        private int pageableCount;
        @JsonProperty("total_count")
        private int totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private String title;
        private String contents;
        private String isbn;
        private String publisher;
        private String thumbnail;
        private String url;
        private List<String> authors;
        private List<String> translators;

        private Integer price;
        @JsonProperty("sale_price")
        private Integer salePrice;

        // 예: "정상판매", "품절", "절판" 등
        private String status;

        @JsonProperty("datetime")
        private String datetime;
    }
}

package com.example.booklog.domain.library.books.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataSearchResponse {

    private List<SearchItem> search;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchItem {
        private String id;          // QID
        private String label;
        private String description;
    }
}

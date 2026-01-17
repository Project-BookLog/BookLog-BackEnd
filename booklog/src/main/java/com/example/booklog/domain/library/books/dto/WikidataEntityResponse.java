package com.example.booklog.domain.library.books.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityResponse {

    // entities: { "Qxxxx": { ... } }
    private Map<String, Object> entities;
}

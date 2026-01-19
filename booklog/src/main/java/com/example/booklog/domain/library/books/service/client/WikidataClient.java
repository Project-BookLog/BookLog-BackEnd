package com.example.booklog.domain.library.books.service.client;

import com.example.booklog.domain.library.books.dto.WikidataEntityResponse;
import com.example.booklog.domain.library.books.dto.WikidataSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WikidataClient {

    private final @Qualifier("wikidataWebClient") WebClient wikidataWebClient;

    public Mono<WikidataSearchResponse> searchEntity(String name) {
        return wikidataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "wbsearchentities")
                        .queryParam("format", "json")
                        .queryParam("language", "ko")
                        .queryParam("search", name)
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .bodyToMono(WikidataSearchResponse.class);
    }

    public Mono<WikidataEntityResponse> getEntity(String qid) {
        return wikidataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "wbgetentities")
                        .queryParam("format", "json")
                        .queryParam("ids", qid)
                        .queryParam("languages", "ko|en")
                        .queryParam("props", "labels|descriptions|claims|sitelinks")
                        .build())
                .retrieve()
                .bodyToMono(WikidataEntityResponse.class);
    }
}

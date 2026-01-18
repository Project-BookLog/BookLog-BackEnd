package com.example.booklog.domain.library.books.service.client;

import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KakaoBookClient {

    private final WebClient kakaoWebClient;

    public Mono<KakaoBookSearchResponse> search(String query, int page, int size) {
        return kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/search/book")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(KakaoBookSearchResponse.class);
    }
}

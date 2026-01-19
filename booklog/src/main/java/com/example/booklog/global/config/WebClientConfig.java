// 파일: src/main/java/com/example/booklog/global/config/WebClientConfig.java
package com.example.booklog.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kakaoWebClient(@Value("${kakao.book.rest-api-key}") String restApiKey) {
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .build();
    }

    @Bean
    public WebClient wikidataWebClient() {
        return WebClient.builder()
                .baseUrl("https://www.wikidata.org")
                .build();
    }
}

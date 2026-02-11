package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오 API를 통한 작가 정보 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorKakaoImportService {

    private final KakaoBookClient kakaoBookClient;

    /**
     * 카카오 API에서 작가 정보 조회
     *
     * @param authorName 작가명
     * @return 카카오 API에서 조회한 도서 검색 결과
     */
    public KakaoBookSearchResponse searchAuthorBooks(String authorName) {
        try {
            log.info("카카오 API에서 작가 도서 검색 시작 - author: {}", authorName);

            KakaoBookSearchResponse response = kakaoBookClient.search(authorName, 1, 10).block();

            if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
                log.warn("카카오 API에서 작가 정보를 찾을 수 없음 - author: {}", authorName);
                return null;
            }

            log.info("카카오 API에서 작가 도서 검색 완료 - author: {}, 결과: {}건",
                    authorName, response.getDocuments().size());
            return response;

        } catch (Exception e) {
            log.error("카카오 API 호출 실패 - author: {}, error: {}", authorName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 카카오 검색 결과가 비어있는지 확인
     */
    public boolean isEmpty(KakaoBookSearchResponse response) {
        return response == null || response.getDocuments() == null || response.getDocuments().isEmpty();
    }
}


package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.AuthorWikidataEnrichment;
import com.example.booklog.domain.library.books.dto.WikidataSearchResponse;
import com.example.booklog.domain.library.books.entity.Authors;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.service.client.WikidataClient;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorEnrichmentService {

    private final AuthorsRepository authorsRepository;
    private final WikidataClient wikidataClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void enrichAuthorByName(String authorName) {
        log.info("=== 위키데이터 작가 보완 시작 - authorName: {} ===", authorName);

        String normalized = authorName == null ? "" : authorName.trim();
        if (normalized.isBlank()) throw new GeneralException(ErrorStatus.AUTHOR_NAME_REQUIRED);

        Authors author = authorsRepository.findByName(normalized)
                .orElseThrow(() -> new GeneralException(ErrorStatus.AUTHOR_NOT_FOUND));

        log.info("작가 조회 완료 - authorId: {}, hasWikidataId: {}, profileImageUrl: {}",
                author.getId(), author.hasWikidataId(), author.getProfileImageUrl() != null ? "있음" : "없음");

        // wikidataId가 있고 profileImageUrl도 있으면 스킵
        if (author.hasWikidataId() && author.getProfileImageUrl() != null && !author.getProfileImageUrl().isEmpty()) {
            log.info("이미 위키데이터 ID와 프로필 이미지 존재 - wikidataId: {}, 보완 스킵", author.getWikidataId());
            return;
        }

        String qid = null;
        String rawJson = null;

        // wikidataId가 없으면 검색
        if (!author.hasWikidataId()) {
            // 1) QID Search
            log.info("위키데이터 QID 검색 시작...");
            WikidataSearchResponse search = wikidataClient.searchEntity(author.getName()).block();
            Optional<String> qidOpt = pickBestQid(search);

            if (qidOpt.isEmpty()) {
                log.warn("위키데이터에서 작가를 찾을 수 없음");
                return;
            }
            qid = qidOpt.get(); // e.g. "Q12345"
            log.info("위키데이터 QID 찾음 - qid: {}", qid);
        } else {
            // 기존 wikidataId 사용
            qid = author.getWikidataId();
            log.info("기존 위키데이터 ID 사용 - qid: {}", qid);
        }

        // 2) Entity detail 조회 (wikidataRawJson이 없거나 profileImageUrl이 없으면)
        if (author.getWikidataRawJson() == null || author.getWikidataRawJson().isEmpty()
            || author.getProfileImageUrl() == null || author.getProfileImageUrl().isEmpty()) {

            log.info("위키데이터 상세 정보 조회 중...");
            Object entityRes = wikidataClient.getEntity(qid).block();
            rawJson = toJsonQuietly(entityRes);
            log.info("위키데이터 응답 크기: {} bytes", rawJson != null ? rawJson.length() : 0);
        } else {
            rawJson = author.getWikidataRawJson();
            log.info("기존 위키데이터 JSON 사용");
        }

        // 3) 이미지 URL 추출 (profileImageUrl이 없으면)
        String profileImageUrl = author.getProfileImageUrl();
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            log.info("이미지 URL 추출 시도...");

            // 1차: 위키데이터에서 추출
            profileImageUrl = extractImageUrl(rawJson);

            // 2차: 위키데이터에서 못 찾으면 위키피디아 API로 시도
            if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                log.info("위키데이터에서 이미지 없음, 위키피디아 API 시도...");
                profileImageUrl = fetchWikipediaImage(author.getName());
            }

            log.info("최종 이미지 URL: {}", profileImageUrl != null ? profileImageUrl : "null");
        } else {
            log.info("기존 프로필 이미지 사용: {}", profileImageUrl);
        }

        // 4) 최소 보강(처음엔 raw 저장 + summary만)
        String summary = null; // TODO: wikipedia 요약 붙이기

        AuthorWikidataEnrichment enrichment =
                new AuthorWikidataEnrichment(
                        profileImageUrl, summary, null, null, null, null, null,
                        qid, rawJson
                );

        author.applyWikidataEnrichment(enrichment);
        authorsRepository.save(author);

        log.info("=== 위키데이터 작가 보완 완료 - authorId: {}, profileImageUrl: {} ===",
                author.getId(), author.getProfileImageUrl());
    }

    /**
     * 위키데이터 JSON에서 이미지 URL 추출
     */
    private String extractImageUrl(String wikidataRawJson) {
        try {
            if (wikidataRawJson == null || wikidataRawJson.isBlank()) {
                log.warn("위키데이터 JSON이 비어있음");
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(wikidataRawJson);

            // entities > Q??? > claims > P18 (image property)
            com.fasterxml.jackson.databind.JsonNode entities = root.path("entities");
            if (entities.isMissingNode()) {
                log.warn("entities 노드 없음");
                return null;
            }

            // 첫 번째 entity 찾기
            com.fasterxml.jackson.databind.JsonNode entity = entities.elements().hasNext()
                ? entities.elements().next()
                : null;

            if (entity == null) {
                log.warn("entity가 null");
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode p18 = entity.path("claims").path("P18");
            if (!p18.isArray() || p18.isEmpty()) {
                log.warn("P18 (이미지) 속성 없음");
                return null;
            }

            String imageName = p18.get(0).path("mainsnak").path("datavalue").path("value").asText(null);
            if (imageName == null || imageName.isBlank()) {
                log.warn("imageName이 null 또는 비어있음");
                return null;
            }

            log.info("위키데이터 이미지 이름: {}", imageName);

            // Wikimedia Commons URL 생성
            return convertToWikimediaUrl(imageName);

        } catch (Exception e) {
            log.error("이미지 URL 추출 실패", e);
            return null;
        }
    }

    /**
     * 이미지 이름을 Wikimedia Commons URL로 변환
     */
    private String convertToWikimediaUrl(String imageName) {
        try {
            // 공백을 언더스코어로 변환
            String normalized = imageName.replace(" ", "_");

            // MD5 해시의 첫 두 글자를 디렉토리로 사용
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String hex = String.format("%032x", new java.math.BigInteger(1, hash));

            String firstChar = hex.substring(0, 1);
            String firstTwo = hex.substring(0, 2);

            // URL 인코딩
            String encoded = java.net.URLEncoder.encode(normalized, java.nio.charset.StandardCharsets.UTF_8);

            return String.format("https://upload.wikimedia.org/wikipedia/commons/%s/%s/%s",
                    firstChar, firstTwo, encoded);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 위키피디아 API에서 작가 이미지 가져오기
     * 한국어 위키피디아 -> 영어 위키피디아 순으로 시도
     */
    private String fetchWikipediaImage(String authorName) {
        try {
            // 1차: 한국어 위키피디아
            String imageUrl = fetchWikipediaImageFromLang(authorName, "ko");
            if (imageUrl != null) {
                log.info("한국어 위키피디아에서 이미지 찾음: {}", imageUrl);
                return imageUrl;
            }

            // 2차: 영어 위키피디아
            imageUrl = fetchWikipediaImageFromLang(authorName, "en");
            if (imageUrl != null) {
                log.info("영어 위키피디아에서 이미지 찾음: {}", imageUrl);
                return imageUrl;
            }

            log.info("위키피디아에서 이미지를 찾을 수 없음 - author: {}", authorName);
            return null;

        } catch (Exception e) {
            log.warn("위키피디아 이미지 조회 실패 - author: {}, error: {}", authorName, e.getMessage());
            return null;
        }
    }

    /**
     * 특정 언어의 위키피디아에서 이미지 URL 가져오기
     */
    private String fetchWikipediaImageFromLang(String authorName, String lang) {
        try {
            String encodedName = java.net.URLEncoder.encode(authorName, java.nio.charset.StandardCharsets.UTF_8);
            String apiUrl = String.format(
                "https://%s.wikipedia.org/w/api.php?action=query&titles=%s&prop=pageimages&format=json&pithumbsize=500",
                lang, encodedName);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response == null) return null;

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode pages = root.path("query").path("pages");

            if (pages.isMissingNode()) return null;

            // 첫 번째 페이지의 thumbnail 추출
            java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> pageIterator = pages.elements();
            if (pageIterator.hasNext()) {
                com.fasterxml.jackson.databind.JsonNode page = pageIterator.next();
                String thumbnail = page.path("thumbnail").path("source").asText(null);
                if (thumbnail != null && !thumbnail.isEmpty()) {
                    return thumbnail;
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("위키피디아 API 호출 실패 - lang: {}, author: {}, error: {}", lang, authorName, e.getMessage());
            return null;
        }
    }

    private Optional<String> pickBestQid(WikidataSearchResponse res) {
        if (res == null || res.getSearch() == null || res.getSearch().isEmpty()) return Optional.empty();

        return res.getSearch().stream()
                .filter(item -> item.getId() != null && !item.getId().isBlank())
                .sorted((a, b) -> score(b) - score(a))
                .map(WikidataSearchResponse.SearchItem::getId)
                .findFirst();
    }

    private int score(WikidataSearchResponse.SearchItem item) {
        String d = item.getDescription() == null ? "" : item.getDescription();
        int s = 0;
        if (d.contains("작가")) s += 5;
        if (d.contains("소설가")) s += 5;
        if (d.contains("시인")) s += 5;
        if (d.contains("writer")) s += 3;
        if (d.contains("novelist")) s += 3;
        return s;
    }

    private String toJsonQuietly(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

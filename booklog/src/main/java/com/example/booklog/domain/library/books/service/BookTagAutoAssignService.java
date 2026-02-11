package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.mapping.BookTags;
import com.example.booklog.domain.tags.repository.BookTagsRepository;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.example.booklog.global.config.GptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPT 기반 책 태그 자동 할당 서비스
 * - 태그가 없는 책에 자동으로 적절한 태그 할당
 * - GPT가 책 제목, 작가, 설명을 분석하여 태그 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookTagAutoAssignService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final BooksRepository booksRepository;
    private final BookTagsRepository bookTagsRepository;
    private final TagsRepository tagsRepository;
    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 특정 책에 태그 자동 할당
     * 이미 태그가 있으면 스킵
     *
     * @param bookId 책 ID
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void autoAssignTagsForBook(Long bookId) {
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다: " + bookId));

        // 이미 태그가 있으면 스킵
        List<BookTags> existingTags = bookTagsRepository.findAllByBookId(bookId);
        if (!existingTags.isEmpty()) {
            log.info("책에 이미 태그가 있음 - bookId: {}, 태그 개수: {}", bookId, existingTags.size());
            return;
        }

        log.info("=== 책 태그 자동 할당 시작 - bookId: {}, title: {} ===", bookId, book.getTitle());

        try {
            // GPT로 태그 추천
            RecommendedTags recommendedTags = recommendTagsWithGpt(book);

            if (recommendedTags == null) {
                log.warn("GPT 태그 추천 실패 - bookId: {}", bookId);
                return;
            }

            // 태그 할당
            int assignedCount = assignTags(book, recommendedTags);

            log.info("=== 책 태그 자동 할당 완료 - bookId: {}, 할당된 태그: {}개 ===", bookId, assignedCount);

        } catch (Exception e) {
            log.error("책 태그 자동 할당 실패 - bookId: {}, error: {}", bookId, e.getMessage(), e);
        }
    }

    /**
     * 태그가 없는 모든 책에 태그 자동 할당 (배치 작업용)
     */
    @Transactional
    public void autoAssignTagsForBooksWithoutTags() {
        log.info("=== 태그 없는 책 일괄 태그 할당 시작 ===");

        // 태그가 없는 책 조회
        List<Books> booksWithoutTags = booksRepository.findBooksWithoutTags();
        log.info("태그 없는 책 개수: {}", booksWithoutTags.size());

        int successCount = 0;
        int failCount = 0;

        for (Books book : booksWithoutTags) {
            try {
                autoAssignTagsForBook(book.getId());
                successCount++;
            } catch (Exception e) {
                log.error("책 태그 할당 실패 - bookId: {}, error: {}", book.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("=== 태그 없는 책 일괄 태그 할당 완료 - 성공: {}, 실패: {} ===", successCount, failCount);
    }

    /**
     * GPT로 태그 추천
     */
    private RecommendedTags recommendTagsWithGpt(Books book) {
        try {
            String authorName = book.getBookAuthors().isEmpty()
                    ? "알 수 없음"
                    : book.getBookAuthors().get(0).getAuthor().getName();

            String prompt = buildTagRecommendationPrompt(book, authorName);

            log.info("GPT에 태그 추천 요청 - bookId: {}", book.getId());
            String gptResponse = callGptApi(prompt);

            if (gptResponse == null) {
                log.warn("GPT 응답이 null - bookId: {}", book.getId());
                return null;
            }

            log.info("GPT 응답 받음 - bookId: {}, 응답 길이: {}", book.getId(), gptResponse.length());

            return parseGptResponse(gptResponse);

        } catch (Exception e) {
            log.error("GPT 태그 추천 실패 - bookId: {}, error: {}", book.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * GPT API 호출 (GptService 재사용)
     */
    private String callGptApi(String prompt) {
        try {
            String apiKey = gptConfig.getSecretKey();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy-key-for-development")) {
                log.warn("GPT API 키가 설정되지 않음 - 태그 자동 할당 스킵");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", gptConfig.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "당신은 책 태그 전문가입니다. 책의 정보를 분석하여 적절한 태그를 추천합니다."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContentFromGptResponse(response.getBody());
            } else {
                log.error("GPT API 호출 실패: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * GPT API 응답에서 content 추출
     */
    private String extractContentFromGptResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            log.error("GPT 응답 형식 오류");
            return null;
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * GPT 프롬프트 생성
     */
    private String buildTagRecommendationPrompt(Books book, String authorName) {
        // 사용 가능한 모든 태그 조회
        List<Tags> allTags = tagsRepository.findAll();

        StringBuilder moodTags = new StringBuilder();
        StringBuilder styleTags = new StringBuilder();
        StringBuilder immersionTags = new StringBuilder();

        for (Tags tag : allTags) {
            switch (tag.getCategory()) {
                case MOOD -> moodTags.append(tag.getName()).append(", ");
                case STYLE -> styleTags.append(tag.getName()).append(", ");
                case IMMERSION -> immersionTags.append(tag.getName()).append(", ");
            }
        }

        return String.format("""
                다음 책에 어울리는 태그를 추천해주세요.
                
                책 제목: %s
                작가: %s
                설명: %s
                
                다음 태그 목록에서 각 카테고리별로 가장 어울리는 태그를 1개씩 선택해주세요:
                
                분위기(mood): %s
                문체(style): %s
                몰입도(immersion): %s
                
                반드시 다음 JSON 형식으로만 응답해주세요:
                {
                  "mood": "태그명",
                  "style": "태그명",
                  "immersion": "태그명"
                }
                
                주의사항:
                - 태그명은 위에 나열된 것 중에서만 선택
                - JSON 형식을 정확히 지켜주세요
                - 다른 설명 없이 JSON만 응답
                """,
                book.getTitle(),
                authorName,
                book.getDescription() != null ? book.getDescription().substring(0, Math.min(500, book.getDescription().length())) : "설명 없음",
                moodTags.toString(),
                styleTags.toString(),
                immersionTags.toString()
        );
    }

    /**
     * GPT 응답 파싱
     */
    private RecommendedTags parseGptResponse(String gptResponse) {
        try {
            // JSON 추출 (코드 블록으로 감싸져 있을 수 있음)
            String jsonContent = extractJsonFromResponse(gptResponse);

            JsonNode root = objectMapper.readTree(jsonContent);

            String mood = root.path("mood").asText(null);
            String style = root.path("style").asText(null);
            String immersion = root.path("immersion").asText(null);

            if (mood == null || style == null || immersion == null) {
                log.warn("GPT 응답에 필수 필드 누락 - mood: {}, style: {}, immersion: {}", mood, style, immersion);
                return null;
            }

            log.info("GPT 태그 추천 파싱 완료 - mood: {}, style: {}, immersion: {}", mood, style, immersion);
            return new RecommendedTags(mood, style, immersion);

        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패 - response: {}, error: {}", gptResponse, e.getMessage(), e);
            return null;
        }
    }

    /**
     * JSON 추출 (코드 블록 제거)
     */
    private String extractJsonFromResponse(String response) {
        String content = response.trim();

        // 코드 블록으로 감싸져 있는 경우 제거
        if (content.startsWith("```json")) {
            content = content.substring("```json".length());
        } else if (content.startsWith("```")) {
            content = content.substring("```".length());
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.trim();
    }

    /**
     * 태그 할당
     */
    private int assignTags(Books book, RecommendedTags recommendedTags) {
        List<BookTags> newBookTags = new ArrayList<>();

        // mood 태그
        Tags moodTag = tagsRepository.findByName(recommendedTags.mood()).orElse(null);
        if (moodTag != null) {
            newBookTags.add(new BookTags(book, moodTag));
            log.info("태그 할당 - bookId: {}, category: MOOD, tagName: {}", book.getId(), recommendedTags.mood());
        } else {
            log.warn("태그를 찾을 수 없음 - tagName: {}", recommendedTags.mood());
        }

        // style 태그
        Tags styleTag = tagsRepository.findByName(recommendedTags.style()).orElse(null);
        if (styleTag != null) {
            newBookTags.add(new BookTags(book, styleTag));
            log.info("태그 할당 - bookId: {}, category: STYLE, tagName: {}", book.getId(), recommendedTags.style());
        } else {
            log.warn("태그를 찾을 수 없음 - tagName: {}", recommendedTags.style());
        }

        // immersion 태그
        Tags immersionTag = tagsRepository.findByName(recommendedTags.immersion()).orElse(null);
        if (immersionTag != null) {
            newBookTags.add(new BookTags(book, immersionTag));
            log.info("태그 할당 - bookId: {}, category: IMMERSION, tagName: {}", book.getId(), recommendedTags.immersion());
        } else {
            log.warn("태그를 찾을 수 없음 - tagName: {}", recommendedTags.immersion());
        }

        // 저장
        if (!newBookTags.isEmpty()) {
            bookTagsRepository.saveAll(newBookTags);
        }

        return newBookTags.size();
    }

    /**
     * GPT 추천 태그
     */
    private record RecommendedTags(
            String mood,
            String style,
            String immersion
    ) {}
}


package com.example.booklog.domain.ai.service;

import com.example.booklog.domain.ai.entity.GptRecommendationRequest;
import com.example.booklog.domain.ai.entity.GptRecommendationResponse;
import com.example.booklog.global.config.GptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPT 서비스 - OpenAI API 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 온보딩 기반 도서 추천 키워드 생성
     * - GPT에게 작가 타입과 장르를 추론하도록 요청
     * - 응답에서 검색 키워드를 추출
     *
     * @param request GPT 추천 요청
     * @return 검색 키워드 정보
     */
    public GptRecommendationResponse generateRecommendationKeyword(GptRecommendationRequest request) {
        try {
            log.info("GPT 추천 키워드 생성 시작 - field: {}, value: {}",
                    request.getFieldName(), request.getFieldValue());

            // 프롬프트 생성
            String prompt = buildRecommendationPrompt(request);

            // GPT API 호출
            String gptResponse = callGptApi(prompt);

            // 응답 파싱
            GptRecommendationResponse response = parseGptResponse(gptResponse);

            log.info("GPT 추천 키워드 생성 완료 - keyword: {}", response.getSearchKeyword());
            return response;

        } catch (Exception e) {
            log.error("GPT 추천 키워드 생성 실패 - field: {}, error: {}",
                    request.getFieldName(), e.getMessage(), e);

            // 실패 시 기본 검색어 반환 (빈 결과 방지)
            return GptRecommendationResponse.builder()
                    .authorType("추천 작가")
                    .genre(request.getFieldDescription())
                    .searchKeyword(request.getFieldDescription() + " 소설")
                    .build();
        }
    }

    /**
     * 도서 정보 기반 분위기/문체/몰입도 키워드 분석
     * - 서재 기반 및 인기 도서 추천에서 사용
     *
     * @param bookTitle 도서 제목
     * @param author 작가명
     * @param publisher 출판사
     * @return 분위기, 문체, 몰입도 키워드 (각 1개씩)
     */
    public Map<String, String> analyzeBookKeywords(String bookTitle, String author, String publisher) {
        try {
            log.info("GPT 도서 키워드 분석 시작 - title: {}, author: {}", bookTitle, author);

            // 프롬프트 생성
            String prompt = buildBookAnalysisPrompt(bookTitle, author, publisher);

            // GPT API 호출
            String gptResponse = callGptApi(prompt);

            // 응답 파싱
            Map<String, String> keywords = parseBookKeywordsResponse(gptResponse);

            log.info("GPT 도서 키워드 분석 완료 - mood: {}, style: {}, immersion: {}",
                    keywords.get("mood"), keywords.get("style"), keywords.get("immersion"));
            return keywords;

        } catch (Exception e) {
            log.error("GPT 도서 키워드 분석 실패 - title: {}, error: {}", bookTitle, e.getMessage(), e);

            // 실패 시 기본 키워드 반환
            Map<String, String> defaultKeywords = new HashMap<>();
            defaultKeywords.put("mood", "잔잔한");
            defaultKeywords.put("style", "간결한");
            defaultKeywords.put("immersion", "편안한");
            return defaultKeywords;
        }
    }

    /**
     * GPT API 호출
     */
    private String callGptApi(String prompt) {
        try {
            // API 키 유효성 검사
            String apiKey = gptConfig.getSecretKey();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy-key-for-development")) {
                log.warn("GPT API 키가 설정되지 않음. 기본 응답 반환");
                throw new RuntimeException("GPT API 키 미설정");
            }

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", gptConfig.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "당신은 도서 추천 전문가입니다. 사용자의 취향을 분석하여 적합한 작가와 장르를 추천합니다."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 300);

            // API 요청
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContentFromResponse(response.getBody());
            } else {
                throw new RuntimeException("GPT API 호출 실패: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("GPT API 호출 실패", e);
        }
    }

    /**
     * GPT API 응답에서 content 추출
     */
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("GPT 응답 형식 오류");
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }

    /**
     * 추천을 위한 프롬프트 생성
     */
    private String buildRecommendationPrompt(GptRecommendationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자의 독서 취향 정보:\n");
        prompt.append("- ").append(request.getFieldName()).append(": ")
              .append(request.getFieldValue()).append(" (")
              .append(request.getFieldDescription()).append(")\n");

        if (request.getRecentSearchKeywords() != null && !request.getRecentSearchKeywords().isEmpty()) {
            prompt.append("- 최근 검색어: ").append(request.getRecentSearchKeywords()).append("\n");
        }

        prompt.append("\n위 정보를 바탕으로 사용자가 좋아할 만한 도서를 찾기 위한 정보를 제공해주세요.\n");
        prompt.append("다음 형식으로 응답해주세요:\n");
        prompt.append("작가타입: [작가명 또는 특징 1~2단어]\n");
        prompt.append("장르: [장르명 1~2단어]\n");
        prompt.append("검색키워드: [도서 검색용 키워드 2~3단어]\n");
        prompt.append("\n※ 중요: 각 항목은 반드시 짧은 단어로만 응답하세요. 문장이 아닌 핵심 키워드만 작성하세요.");

        return prompt.toString();
    }

    /**
     * 도서 분석용 프롬프트 생성
     */
    private String buildBookAnalysisPrompt(String bookTitle, String author, String publisher) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 도서 정보를 분석하여 분위기, 문체, 몰입도 키워드를 추천해주세요.\n\n");
        prompt.append("도서 정보:\n");
        prompt.append("- 제목: ").append(bookTitle).append("\n");
        prompt.append("- 작가: ").append(author).append("\n");
        if (publisher != null && !publisher.isEmpty()) {
            prompt.append("- 출판사: ").append(publisher).append("\n");
        }

        prompt.append("\n다음 형식으로 응답해주세요:\n");
        prompt.append("분위기: [따뜻한, 잔잔한, 서늘한, 몽환적인, 유쾌한, 어두운 중 1개]\n");
        prompt.append("문체: [간결한, 화려한, 담백한, 섬세한, 직설적, 은유적 중 1개]\n");
        prompt.append("몰입도: [빠른전개, 느린전개, 긴장감있는, 편안한, 현실적, 환상적, 논리적, 감성적 중 1개]\n");
        prompt.append("\n※ 중요: 각 항목은 반드시 위에서 제시한 키워드 중 1개만 선택하세요. 다른 단어는 사용하지 마세요.");

        return prompt.toString();
    }

    /**
     * 도서 키워드 분석 응답 파싱
     */
    private Map<String, String> parseBookKeywordsResponse(String gptContent) {
        Map<String, String> keywords = new HashMap<>();

        try {
            String[] lines = gptContent.split("\n");

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("분위기:")) {
                    keywords.put("mood", trimmed.substring(trimmed.indexOf(":") + 1).trim());
                } else if (trimmed.startsWith("문체:")) {
                    keywords.put("style", trimmed.substring(trimmed.indexOf(":") + 1).trim());
                } else if (trimmed.startsWith("몰입도:")) {
                    keywords.put("immersion", trimmed.substring(trimmed.indexOf(":") + 1).trim());
                }
            }

            // 기본값 설정 (파싱 실패 시)
            keywords.putIfAbsent("mood", "잔잔한");
            keywords.putIfAbsent("style", "간결한");
            keywords.putIfAbsent("immersion", "편안한");

            return keywords;

        } catch (Exception e) {
            log.error("도서 키워드 응답 파싱 실패: {}", e.getMessage(), e);
            keywords.put("mood", "잔잔한");
            keywords.put("style", "간결한");
            keywords.put("immersion", "편안한");
            return keywords;
        }
    }

    /**
     * GPT 응답 파싱
     * 응답 형식:
     * 작가타입: xxx
     * 장르: xxx
     * 검색키워드: xxx
     */
    private GptRecommendationResponse parseGptResponse(String gptContent) {
        try {
            String[] lines = gptContent.split("\n");
            String authorType = "";
            String genre = "";
            String searchKeyword = "";

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("작가타입:") || trimmed.startsWith("작가 타입:")) {
                    authorType = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                } else if (trimmed.startsWith("장르:")) {
                    genre = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                } else if (trimmed.startsWith("검색키워드:") || trimmed.startsWith("검색 키워드:")) {
                    searchKeyword = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                }
            }

            // 검색키워드가 비어있으면 작가타입 + 장르 조합
            if (searchKeyword.isEmpty()) {
                searchKeyword = genre + " " + authorType;
            }

            return GptRecommendationResponse.builder()
                    .authorType(authorType)
                    .genre(genre)
                    .searchKeyword(searchKeyword)
                    .build();

        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }
}


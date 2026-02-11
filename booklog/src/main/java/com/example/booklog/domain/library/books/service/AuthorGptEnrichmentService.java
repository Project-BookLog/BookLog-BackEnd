package com.example.booklog.domain.library.books.service;

import com.example.booklog.global.config.GptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPT를 이용한 작가 정보 보완 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorGptEnrichmentService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * GPT를 사용하여 작가 정보 보완
     *
     * @param authorName 작가명
     * @return 보완된 작가 정보 (프로필 이미지, 소개, 프로필, 수상경력)
     */
    public AuthorEnrichmentResult enrichAuthorInfo(String authorName) {
        try {
            log.info("GPT를 통한 작가 정보 보완 시작 - author: {}", authorName);

            String prompt = buildAuthorInfoPrompt(authorName);
            String gptResponse = callGptApi(prompt);
            AuthorEnrichmentResult result = parseAuthorInfoResponse(gptResponse, authorName);

            log.info("GPT를 통한 작가 정보 보완 완료 - author: {}", authorName);
            return result;

        } catch (Exception e) {
            log.error("GPT 작가 정보 보완 실패 - author: {}, error: {}", authorName, e.getMessage(), e);
            return createEmptyResult();
        }
    }

    /**
     * 작가 정보 프롬프트 생성
     */
    private String buildAuthorInfoPrompt(String authorName) {
        return String.format("""
            작가 '%s'에 대한 상세 정보를 다음 JSON 형식으로 제공해주세요:
            
            {
              "biography": "작가에 대한 한 줄 소개 (예: 대한민국의 대표 소설가, 시인)",
              "nationality": "작가의 국적 (예: 대한민국, 미국, 일본)",
              "profile": {
                "education": ["학력1", "학력2"],
                "debut": "데뷔작 정보 (예: 붉은 닭(1994))",
                "birthDate": "출생 정보 (예: 1970. 11. 27)",
                "occupations": ["직업1", "직업2"]
              },
              "awards": [
                {
                  "year": 2022,
                  "awardName": "수상 내역 (예: 제30회 대산문학상 소설부문)",
                  "workTitle": "수상작 제목"
                }
              ]
            }
            
            - biography는 한 문장으로 간결하게 작성
            - nationality는 국적을 명확히 작성 (예: 대한민국, 미국, 일본, 영국 등)
            - profile은 가능한 정보만 포함 (없으면 빈 배열 또는 null)
            - awards는 최신순으로 정렬 (최대 10개)
            - 정보를 찾을 수 없으면 해당 필드를 null 또는 빈 배열로 설정
            - 반드시 올바른 JSON 형식으로만 응답
            """, authorName);
    }

    /**
     * GPT API 호출
     */
    private String callGptApi(String prompt) {
        try {
            String apiKey = gptConfig.getSecretKey();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy-key-for-development")) {
                log.warn("GPT API 키가 설정되지 않음");
                throw new RuntimeException("GPT API 키 미설정");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", gptConfig.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "당신은 문학 작가 정보 전문가입니다. 정확하고 간결한 정보를 JSON 형식으로 제공합니다."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1500);

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
     * GPT 응답 파싱
     */
    private AuthorEnrichmentResult parseAuthorInfoResponse(String gptResponse, String authorName) {
        try {
            // JSON 추출 (코드 블록으로 감싸져 있을 수 있음)
            String jsonContent = extractJsonFromResponse(gptResponse);

            JsonNode root = objectMapper.readTree(jsonContent);

            String biography = root.path("biography").asText(null);
            String nationality = root.path("nationality").asText(null);

            // Profile 파싱
            JsonNode profileNode = root.path("profile");
            ProfileInfo profile = null;
            if (!profileNode.isMissingNode() && !profileNode.isNull()) {
                List<String> education = jsonArrayToList(profileNode.path("education"));
                String debut = profileNode.path("debut").asText(null);
                String birthDate = profileNode.path("birthDate").asText(null);
                List<String> occupations = jsonArrayToList(profileNode.path("occupations"));

                profile = new ProfileInfo(education, debut, birthDate, occupations);
            }

            // Awards 파싱
            List<AwardInfo> awards = new ArrayList<>();
            JsonNode awardsNode = root.path("awards");
            if (awardsNode.isArray()) {
                for (JsonNode awardNode : awardsNode) {
                    Integer year = awardNode.path("year").isInt() ? awardNode.path("year").asInt() : null;
                    String awardName = awardNode.path("awardName").asText(null);
                    String workTitle = awardNode.path("workTitle").asText(null);

                    if (awardName != null && !awardName.isEmpty()) {
                        awards.add(new AwardInfo(year, awardName, workTitle));
                    }
                }
            }

            return new AuthorEnrichmentResult(biography, nationality, profile, awards);

        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패 - author: {}, response: {}, error: {}",
                    authorName, gptResponse, e.getMessage(), e);
            return createEmptyResult();
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
     * JSON 배열을 리스트로 변환
     */
    private List<String> jsonArrayToList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String value = item.asText(null);
                if (value != null && !value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    /**
     * 빈 결과 생성
     */
    private AuthorEnrichmentResult createEmptyResult() {
        return new AuthorEnrichmentResult(null, null, null, List.of());
    }

    /**
     * 작가 정보 보완 결과
     */
    public record AuthorEnrichmentResult(
            String biography,
            String nationality,
            ProfileInfo profile,
            List<AwardInfo> awards
    ) {}

    /**
     * 프로필 정보
     */
    public record ProfileInfo(
            List<String> education,
            String debut,
            String birthDate,
            List<String> occupations
    ) {}

    /**
     * 수상 정보
     */
    public record AwardInfo(
            Integer year,
            String awardName,
            String workTitle
    ) {}
}


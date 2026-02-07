package com.example.booklog.domain.ai.service;

import com.example.booklog.global.config.GptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GptKeyScreenInsightService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_KEYSCREEN =
            "당신은 독서 앱의 UX 라이터입니다. 사용자의 독서 데이터를 바탕으로 '독서 취향을 분석한 한두 문장'을 씁니다. " +
                    "관찰자 시점, 과장 없이 담백하게, 한국어로 작성하세요.";

    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String generateTasteInsight(YearMonth month, List<String> topMoodTags) {
        String tags = (topMoodTags == null || topMoodTags.isEmpty()) ? "없음" : String.join(", ", topMoodTags);

        String prompt = """
                다음 정보로 마이페이지 키스크린에 들어갈 '독서 취향 분석' 문구를 작성해줘.
                - 대상 월: %s
                - 분위기 태그 TOP: %s

                조건:
                1) 한국어 1~2문장
                2) 140자 이내
                3) 과장/광고 문구 금지
                4) 태그가 있으면 1개 이상 자연스럽게 언급
                5) 따옴표, 이모지 사용하지 말 것
                """.formatted(month, tags);

        try {
            String content = callChatCompletion(SYSTEM_KEYSCREEN, prompt, 0.6, 220);
            return postProcess(content, 140);
        } catch (Exception e) {
            // 기본 문구
            String tag = (topMoodTags != null && !topMoodTags.isEmpty()) ? topMoodTags.get(0) : null;
            return (tag == null)
                    ? month.getMonthValue() + "월 기록을 보면 취향의 흐름이 조금씩 선명해지고 있어요."
                    : month.getMonthValue() + "월에는 " + tag + " 분위기를 중심으로 취향이 모이는 모습이에요.";
        }
    }

    private String callChatCompletion(String systemMessage, String userPrompt, double temperature, int maxTokens) {
        String apiKey = gptConfig.getSecretKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy-key-for-development")) {
            throw new RuntimeException("GPT API 키 미설정");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", gptConfig.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractContent(response.getBody());
        }
        throw new RuntimeException("GPT API 호출 실패: " + response.getStatusCode());
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("GPT 응답 형식 오류");
        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }


    private String postProcess(String text, int maxLen) {
        if (text == null) return null;
        String s = text.trim()
                .replaceAll("^[\"'“”‘’]+", "")
                .replaceAll("[\"'“”‘’]+$", "")
                .replaceAll("\\s*\\R\\s*", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return (s.length() > maxLen) ? s.substring(0, maxLen).trim() : s;
    }
}

package com.example.booklog.domain.ai.service;

import com.example.booklog.global.config.GptConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptUxCopyService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_CALENDAR_UX_WRITER =
            "당신은 독서 앱의 UX 라이터입니다. 사용자의 월간 독서 현황을 따뜻하고 짧게 요약합니다. " +
                    "과장/광고 느낌 없이 담백하게 한국어로 작성하세요.";

    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String generateCalendarMonthlySummary(
            YearMonth month,
            int progressPercent,
            int currentDay,
            int lastDay,
            List<String> topMoodTags
    ) {
        try {
            String prompt = buildPrompt(month, progressPercent, currentDay, lastDay, topMoodTags);
            String content = callChatCompletion(SYSTEM_CALENDAR_UX_WRITER, prompt, 0.6, 220);
            return postProcess(content, 120);
        } catch (Exception e) {
            log.warn("캘린더 요약 생성 실패: {}", e.getMessage());
            return defaultSummary(month, progressPercent, topMoodTags);
        }
    }

    private String buildPrompt(YearMonth month, int progressPercent, int currentDay, int lastDay, List<String> tags) {
        String t = (tags == null || tags.isEmpty()) ? "없음" : String.join(", ", tags);

        return """
                다음 정보로 '독서 캘린더/독서 현황 카드'에 들어갈 회색 요약 문구를 작성해줘.
                - 대상 월: %s
                - 진행 퍼센트: %d%%
                - 기록 진행: %d/%d
                - 분위기 태그 TOP: %s

                조건:
                1) 한국어로 1~2문장
                2) 120자 이내
                3) 과장/광고 문구 금지, 담백하게
                4) 태그가 있으면 자연스럽게 1개 이상 언급
                5) 따옴표, 이모지 사용하지 말 것
                """.formatted(month, progressPercent, currentDay, lastDay, t);
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
                OPENAI_API_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
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
        if (s.length() > maxLen) s = s.substring(0, maxLen).trim();
        return s;
    }

    private String defaultSummary(YearMonth month, int progressPercent, List<String> topMoodTags) {
        String tag = (topMoodTags != null && !topMoodTags.isEmpty()) ? topMoodTags.get(0) : null;
        if (tag == null) return month.getMonthValue() + "월의 기록, " + progressPercent + "%의 흐름이에요.";
        return month.getMonthValue() + "월의 기록, " + progressPercent + "%의 흐름이에요. " + tag + " 분위기의 책들과 함께하고 있어요.";
    }
}

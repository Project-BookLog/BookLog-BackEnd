package com.example.booklog.domain.library.books.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

/**
 * 책 설명(description)이 짤려있을 때 보완하는 서비스
 *
 * 카카오 API의 contents 필드가 불완전한 경우:
 * 1. "..."로 끝나는 경우
 * 2. 너무 짧은 경우 (200자 미만)
 *
 * 해결 방법:
 * - 카카오 책 상세 페이지를 크롤링해서 더 긴 설명 추출
 */
@Slf4j
@Service
public class BookDescriptionEnhancer {

    private static final int MIN_DESCRIPTION_LENGTH = 200;
    private static final int CRAWL_TIMEOUT_MS = 5000;

    /**
     * description이 보완이 필요한지 확인
     *
     * @param description 현재 description
     * @return true: 보완 필요, false: 충분함
     */
    public boolean needsEnhancement(String description) {
        if (description == null || description.isBlank()) {
            return true;
        }

        String trimmed = description.trim();

        // 1. "..."로 끝나면 짤린 것
        if (trimmed.endsWith("...")) {
            return true;
        }

        // 2. 너무 짧으면 불완전
        if (trimmed.length() < MIN_DESCRIPTION_LENGTH) {
            return true;
        }

        // 3. 문장이 중간에 끊겨있는지 확인
        if (isIncompleteSentence(trimmed)) {
            return true;
        }

        return false;
    }

    /**
     * 문장이 중간에 끊겨있는지 확인
     *
     * @param text 확인할 텍스트
     * @return true: 불완전한 문장, false: 완전한 문장
     */
    private boolean isIncompleteSentence(String text) {
        String trimmed = text.trim();

        // 한글/영문 문장 종결 기호로 끝나지 않으면 불완전
        char lastChar = trimmed.charAt(trimmed.length() - 1);

        // 완전한 문장 종결 기호
        if (lastChar == '.' || lastChar == '!' || lastChar == '?' ||
            lastChar == '。' || lastChar == '』' || lastChar == '"' ||
            lastChar == '\'' || lastChar == ')' || lastChar == ']') {
            return false;
        }

        // 한글 종결어미로 끝나는지 확인
        if (trimmed.endsWith("다") || trimmed.endsWith("요") ||
            trimmed.endsWith("까") || trimmed.endsWith("세요") ||
            trimmed.endsWith("습니다") || trimmed.endsWith("입니다") ||
            trimmed.endsWith("였다") || trimmed.endsWith("있다") ||
            trimmed.endsWith("한다") || trimmed.endsWith("된다")) {
            return false;
        }

        // 그 외는 불완전한 문장으로 간주
        return true;
    }

    /**
     * 카카오 책 상세 페이지를 크롤링해서 더 긴 description 추출
     *
     * @param kakaoDetailUrl 카카오 책 상세 페이지 URL
     * @param currentDescription 현재 description (실패 시 fallback)
     * @return 향상된 description
     */
    public String enhanceFromKakaoPage(String kakaoDetailUrl, String currentDescription) {
        if (kakaoDetailUrl == null || kakaoDetailUrl.isBlank()) {
            log.warn("카카오 URL이 없어서 description 보완 불가 - 문장 마무리 시도");
            return completeIncompleteSentence(currentDescription);
        }

        try {
            log.info("카카오 페이지 크롤링 시작: {}", kakaoDetailUrl);

            Document doc = Jsoup.connect(kakaoDetailUrl)
                    .timeout(CRAWL_TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 카카오 책 상세 페이지의 책 소개 영역 추출
            String enhanced = extractDescriptionFromKakaoPage(doc);

            if (enhanced != null && !enhanced.isBlank() && enhanced.length() > currentDescription.length()) {
                log.info("description 보완 성공 (크롤링): {} -> {} 자", currentDescription.length(), enhanced.length());
                return enhanced;
            } else {
                log.warn("크롤링 결과가 기존 description보다 길지 않음 - 문장 마무리 시도");
                return completeIncompleteSentence(currentDescription);
            }

        } catch (Exception e) {
            log.warn("카카오 페이지 크롤링 실패: {}, 문장 마무리 시도", e.getMessage());
            return completeIncompleteSentence(currentDescription);
        }
    }

    /**
     * 불완전한 문장을 자연스럽게 완성
     *
     * @param description 원본 description
     * @return 완성된 description
     */
    private String completeIncompleteSentence(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        String trimmed = description.trim();

        // 이미 완전한 문장이면 그대로 반환
        if (!isIncompleteSentence(trimmed)) {
            return description;
        }

        // 마지막 완전한 문장까지만 자르기
        String completed = truncateToLastCompleteSentence(trimmed);

        if (completed != null && !completed.isBlank()) {
            log.info("description 문장 마무리: {} -> {} 자", trimmed.length(), completed.length());
            return completed;
        }

        // 마지막 완전한 문장이 없으면 "..."로 마무리
        log.info("description에 '...' 추가로 마무리");
        return trimmed + "...";
    }

    /**
     * 마지막 완전한 문장까지만 추출
     *
     * @param text 원본 텍스트
     * @return 마지막 완전한 문장까지의 텍스트
     */
    private String truncateToLastCompleteSentence(String text) {
        // 문장 종결 기호 찾기 (마지막부터 역순 검색)
        int lastPeriod = Math.max(
            Math.max(text.lastIndexOf('.'), text.lastIndexOf('。')),
            Math.max(text.lastIndexOf('!'), text.lastIndexOf('?'))
        );

        // 한글 종결어미 찾기
        int lastKoreanEnding = findLastKoreanSentenceEnding(text);

        // 둘 중 더 뒤에 있는 위치 선택
        int cutPosition = Math.max(lastPeriod, lastKoreanEnding);

        if (cutPosition > 0 && cutPosition < text.length() - 1) {
            // 마지막 완전한 문장까지만 반환
            return text.substring(0, cutPosition + 1).trim();
        }

        return null;
    }

    /**
     * 한글 종결어미의 마지막 위치 찾기
     *
     * @param text 검색할 텍스트
     * @return 마지막 종결어미 위치 (-1이면 없음)
     */
    private int findLastKoreanSentenceEnding(String text) {
        String[] endings = {
            "습니다", "입니다", "였습니다", "있습니다", "했습니다",
            "한다", "된다", "있다", "였다", "없다",
            "요", "네요", "어요", "죠"
        };

        int maxPosition = -1;

        for (String ending : endings) {
            int pos = text.lastIndexOf(ending);
            if (pos > maxPosition) {
                maxPosition = pos + ending.length() - 1;
            }
        }

        return maxPosition;
    }

    /**
     * 카카오 책 상세 페이지에서 description 추출
     *
     * @param doc Jsoup Document
     * @return 추출된 description
     */
    private String extractDescriptionFromKakaoPage(Document doc) {
        try {
            // 카카오 책 페이지 구조에 맞춰 선택자 조정 필요
            // 예시: class="book_introduce" 또는 id="book_contents" 등

            // 시도 1: 책 소개 영역
            Element introElement = doc.selectFirst(".book_introduce, .desc_book, .info_book");
            if (introElement != null) {
                String text = introElement.text().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }

            // 시도 2: meta description 태그
            Element metaDesc = doc.selectFirst("meta[property=og:description], meta[name=description]");
            if (metaDesc != null) {
                String content = metaDesc.attr("content");
                if (!content.isBlank()) {
                    return content;
                }
            }

            // 시도 3: 일반적인 본문 영역
            Element contentElement = doc.selectFirst(".content, .description, .summary");
            if (contentElement != null) {
                String text = contentElement.text().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }

            log.warn("카카오 페이지에서 description을 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.warn("카카오 페이지 파싱 중 오류: {}", e.getMessage());
            return null;
        }
    }
}


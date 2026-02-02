package com.example.booklog.domain.onboarding.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 키워드 유사도 매칭 유틸리티
 * - 코사인 유사도 기반 키워드 매칭
 * - 사전 정의된 키워드 집합 관리
 */
@Slf4j
@Component
public class KeywordSimilarityMatcher {

    /**
     * 사전 정의된 분위기 키워드
     */
    @Getter
    private static final List<String> MOOD_KEYWORDS = Arrays.asList(
            "따뜻한", "잔잔한", "서늘한", "몽환적인", "유쾌한", "어두운"
    );

    /**
     * 사전 정의된 문체 키워드
     */
    @Getter
    private static final List<String> STYLE_KEYWORDS = Arrays.asList(
            "간결한", "화려한", "담백한", "섬세한", "직설적", "은유적"
    );

    /**
     * 사전 정의된 몰입도 키워드
     * (SentenceBreath, ExpressionTexture, ExpressionDirection 통합)
     */
    @Getter
    private static final List<String> IMMERSION_KEYWORDS = Arrays.asList(
            "빠른 전개", "느린 전개", "긴장감 있는", "편안한",
            "현실적", "환상적", "논리적", "감성적"
    );

    /**
     * 한글 자모 분해 기반 간단한 유사도 계산
     * - 실제 프로덕션에서는 Word2Vec, BERT 등 사용 권장
     * - 여기서는 문자열 유사도 기반 간이 구현
     */
    public String findMostSimilarKeyword(String target, List<String> candidates) {
        if (target == null || target.isEmpty() || candidates == null || candidates.isEmpty()) {
            return null;
        }

        double maxSimilarity = 0.0;
        String bestMatch = null;

        for (String candidate : candidates) {
            double similarity = calculateSimilarity(target, candidate);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = candidate;
            }
        }

        // 유사도가 일정 임계값 이상일 때만 반환
        if (maxSimilarity >= 0.3) {
            log.debug("키워드 매칭 성공 - target: {}, match: {}, similarity: {}",
                    target, bestMatch, maxSimilarity);
            return bestMatch;
        }

        log.debug("키워드 매칭 실패 - target: {}, maxSimilarity: {}", target, maxSimilarity);
        return null;
    }

    /**
     * 간단한 문자열 유사도 계산 (Jaccard similarity 기반)
     */
    private double calculateSimilarity(String s1, String s2) {
        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();

        for (char c : s1.toCharArray()) {
            set1.add(c);
        }
        for (char c : s2.toCharArray()) {
            set2.add(c);
        }

        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 분위기 키워드 매칭
     */
    public String matchMoodKeyword(String target) {
        return findMostSimilarKeyword(target, MOOD_KEYWORDS);
    }

    /**
     * 문체 키워드 매칭
     */
    public String matchStyleKeyword(String target) {
        return findMostSimilarKeyword(target, STYLE_KEYWORDS);
    }

    /**
     * 몰입도 키워드 매칭
     */
    public String matchImmersionKeyword(String target) {
        return findMostSimilarKeyword(target, IMMERSION_KEYWORDS);
    }

    /**
     * Enum 값을 한글 설명으로 변환하는 헬퍼 메서드
     */
    public String getEnumDescription(Object enumValue) {
        if (enumValue == null) {
            return null;
        }

        try {
            // Enum의 getDescription() 또는 getLabel() 메서드 호출
            var method = enumValue.getClass().getMethod("getDescription");
            return (String) method.invoke(enumValue);
        } catch (NoSuchMethodException e) {
            try {
                var method = enumValue.getClass().getMethod("getLabel");
                return (String) method.invoke(enumValue);
            } catch (Exception ex) {
                return enumValue.toString();
            }
        } catch (Exception e) {
            log.warn("Enum 설명 조회 실패: {}", e.getMessage());
            return enumValue.toString();
        }
    }

    /**
     * Enum 값을 핵심 단어(label)로 변환하는 헬퍼 메서드
     * PreferredMood -> getDescription() 반환 (예: "따뜻한")
     * SentenceBreath/ExpressionTexture/ExpressionDirection -> getLabel() 반환 (예: "간결한")
     */
    public String getEnumLabel(Object enumValue) {
        if (enumValue == null) {
            return null;
        }

        try {
            // 1. getLabel() 메서드 우선 시도 (문체 관련 Enum)
            var labelMethod = enumValue.getClass().getMethod("getLabel");
            return (String) labelMethod.invoke(enumValue);
        } catch (NoSuchMethodException e) {
            try {
                // 2. getDescription() 메서드 시도 (분위기 관련 Enum)
                var descMethod = enumValue.getClass().getMethod("getDescription");
                return (String) descMethod.invoke(enumValue);
            } catch (Exception ex) {
                log.warn("Enum label 조회 실패, toString 사용: {}", enumValue);
                return enumValue.toString();
            }
        } catch (Exception e) {
            log.warn("Enum label 조회 실패: {}", e.getMessage());
            return enumValue.toString();
        }
    }
}


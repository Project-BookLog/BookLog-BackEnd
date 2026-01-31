package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 문장 호흡 스타일 (문장의 호흡)
 */
public enum SentenceBreath {
    CONCISE("간결한", "불필요한 수식 없이 핵심만 또렷하게 전하는 문장"),
    ELABORATE("화려한", "다채로운 어휘와 수식어로 오감을 자극하는 문장");

    private final String label;
    private final String description;

    SentenceBreath(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}


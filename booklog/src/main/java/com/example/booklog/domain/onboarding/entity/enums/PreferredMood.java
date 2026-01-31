package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 선호 분위기/장르 (어떤 분위기의 책을 좋아하시나요?)
 * UI에서 최대 2개까지 선택 가능
 */
public enum PreferredMood {
    WARM("따뜻한"),
    CALM("잔잔한"),
    COOL("서늘한"),
    DREAMY("몽환적인"),
    CHEERFUL("유쾌한"),
    DARK("어두운");

    private final String description;

    PreferredMood(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}


package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 독서 방식 (어떤 방식으로 책을 읽는 편인가?)
 */
public enum ReaderType {
    BEGINNER_READER("독서 입문자"),
    PROFESSIONAL_READER("프로 다독러");

    private final String description;

    ReaderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}


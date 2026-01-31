package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 표현의 방향 (표현의 방향)
 */
public enum ExpressionDirection {
    DIRECT("직설적", "에둘러 않고 사건 본질을 바로 제공하는 문장"),
    METAPHORICAL("은유적", "비유와 상징으로 의미를 전달하는 문장");

    private final String label;
    private final String description;

    ExpressionDirection(String label, String description) {
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


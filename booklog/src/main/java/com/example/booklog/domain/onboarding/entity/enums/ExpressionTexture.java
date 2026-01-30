package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 표현의 질감 (표현의 질감)
 */
public enum ExpressionTexture {
    PLAIN("담백한", "과한 꾸밈없이 진솔하며 매끄럽게 읽히는 문장"),
    DELICATE("섬세한", "세밀한 묘사로 이야기 속 장면의 밀도를 높인 문장");

    private final String label;
    private final String description;

    ExpressionTexture(String label, String description) {
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


package com.example.booklog.domain.onboarding.entity.enums;

/**
 * 독서 순간 (책을 펼칠 때, 어떤 순간이 좋으신가요?)
 */
public enum ReadingMoment {
    ROUTINE_TRANSITION("기본 전환", "부담 없이 술술 읽히는 글"),
    INTELLECTUAL_EXPLORATION("지적인 탐구", "사유의 깊이를 더해주는 글"),
    IMMERSIVE_FLOW("압도적 몰입", "손식간에 몰입하게 되는 글"),
    LINGERING_AFTERTASTE("집은 여운", "오랫동안 잔상이 남는 글");

    private final String label;
    private final String description;

    ReadingMoment(String label, String description) {
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


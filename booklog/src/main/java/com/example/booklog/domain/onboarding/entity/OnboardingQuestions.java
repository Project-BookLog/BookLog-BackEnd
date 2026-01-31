package com.example.booklog.domain.onboarding.entity;

import com.example.booklog.domain.onboarding.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding_questions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_question_key", columnNames = {"question_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingQuestions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "question_key", length = 50, nullable = false, unique = true)
    private String questionKey;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "question_text", length = 255, nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false) //enum으로 분리
    private QuestionType questionType;

    @Column(name = "max_selections", nullable = false) //max_select임
    private Integer maxSelections = 1;

    @Column(name = "display_order", nullable = false) //sort_order임
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public OnboardingQuestions(String questionKey, String title, String questionText,
                               QuestionType questionType, Integer maxSelections,
                               Integer displayOrder, Boolean isActive) {
        this.questionKey = questionKey;
        this.title = title;
        this.questionText = questionText;
        this.questionType = questionType;
        this.maxSelections = maxSelections != null ? maxSelections : 1;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateActive(Boolean isActive) {
        this.isActive = isActive;
    }
}


package com.example.booklog.domain.onboarding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_onboarding_options_question"))
    private OnboardingQuestions question;

    @Column(name = "option_key", length = 50, nullable = false)
    private String optionKey;

    @Column(name = "label", length = 50, nullable = false)
    private String label;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public OnboardingOptions(OnboardingQuestions question, String optionKey, String label,
                             String description, String imageUrl, Integer displayOrder, Boolean isActive) {
        this.question = question;
        this.optionKey = optionKey;
        this.label = label;
        this.description = description;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateActive(Boolean isActive) {
        this.isActive = isActive;
    }
}


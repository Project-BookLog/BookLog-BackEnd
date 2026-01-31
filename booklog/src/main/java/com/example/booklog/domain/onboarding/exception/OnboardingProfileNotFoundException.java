package com.example.booklog.domain.onboarding.exception;

/**
 * 온보딩 프로필을 찾을 수 없을 때 발생하는 예외
 */
public class OnboardingProfileNotFoundException extends RuntimeException {

    public OnboardingProfileNotFoundException() {
        super("온보딩 프로필이 존재하지 않습니다.");
    }

    public OnboardingProfileNotFoundException(String message) {
        super(message);
    }
}


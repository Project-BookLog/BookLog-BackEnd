package com.example.booklog.domain.onboarding.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("존재하지 않는 사용자입니다.");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}


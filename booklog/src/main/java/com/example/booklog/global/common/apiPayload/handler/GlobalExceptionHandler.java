package com.example.booklog.global.common.apiPayload.handler;

import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthException;
import com.example.booklog.global.common.apiPayload.ApiResponse;
import com.example.booklog.global.common.apiPayload.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * AuthException 처리
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        log.error("AuthException: {}", e.getMessage(), e);
        AuthErrorCode errorCode = e.getErrorCode();

        ApiResponse<Void> response = ApiResponse.onFailure(errorCode);

        // HTTP 상태 코드 매핑
        HttpStatus status = mapAuthErrorToHttpStatus(errorCode);

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Validation 예외 처리 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        log.error("Validation Exception: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.onFailure(
                GeneralErrorCode.VALIDATION_ERROR,
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception e) {
        log.error("Unexpected Exception: {}", e.getMessage(), e);

        ApiResponse<String> response = ApiResponse.onFailure(
                GeneralErrorCode.INTERNAL_SERVER_ERROR,
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * AuthErrorCode를 HTTP Status로 매핑
     */
    private HttpStatus mapAuthErrorToHttpStatus(AuthErrorCode errorCode) {
        return switch (errorCode) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED, TOKEN_EXPIRED, INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case DUPLICATE_EMAIL, INVALID_EMAIL_FORMAT, INVALID -> HttpStatus.BAD_REQUEST;
        };
    }
}

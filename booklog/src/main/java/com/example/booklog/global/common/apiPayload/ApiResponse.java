package com.example.booklog.global.common.apiPayload;

import com.example.booklog.global.auth.exception.AuthErrorCode;
import com.example.booklog.global.auth.exception.AuthSuccessCode;
import com.example.booklog.global.common.apiPayload.code.GeneralErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답")
public class ApiResponse<T> {
    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 코드", example = "AUTH_2000")
    private final String code;

    @Schema(description = "응답 메시지", example = "로그인에 성공했습니다.")
    private final String message;

    @Schema(description = "응답 데이터")
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 성공 응답 (AuthSuccessCode 사용)
    public static <T> ApiResponse<T> onSuccess(AuthSuccessCode successCode, T data) {
        return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), data);
    }

    // 성공 응답 (데이터 없이)
    public static <T> ApiResponse<T> onSuccess(AuthSuccessCode successCode) {
        return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), null);
    }

    // 실패 응답 (AuthErrorCode 사용)
    public static <T> ApiResponse<T> onFailure(AuthErrorCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
    }

    // 실패 응답 (데이터 없이)
    public static <T> ApiResponse<T> onFailure(AuthErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    // 실패 응답 (GeneralErrorCode 사용)
    public static <T> ApiResponse<T> onFailure(GeneralErrorCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
    }

    // 실패 응답 (GeneralErrorCode 사용, 데이터 없이)
    public static <T> ApiResponse<T> onFailure(GeneralErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}

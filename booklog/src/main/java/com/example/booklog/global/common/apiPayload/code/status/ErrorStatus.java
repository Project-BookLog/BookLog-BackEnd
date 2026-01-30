package com.example.booklog.global.common.apiPayload.code.status;

import com.example.booklog.global.common.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // =========================
    // [User / Profile]
    // =========================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저 없음"),
    NICKNAME_EMPTY(HttpStatus.BAD_REQUEST, "U002", "닉네임은 필수입니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "U003", "닉네임은 50자 이하여야 합니다."),

    // =========================
    // [File / Upload]
    // =========================
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "F001", "파일이 필요합니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "F002", "파일 용량이 너무 큽니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "F003", "지원하는 파일 양식이 아닙니다."),

    // =========================
    // [Author / Enrichment / Search]
    // =========================
    AUTHOR_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "AU001", "authorName is required"),
    AUTHOR_NOT_FOUND(HttpStatus.NOT_FOUND, "AU002", "Author not found"),
    SEARCH_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "SRCH001", "검색어는 필수입니다."),
    SEARCH_KEYWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "SRCH002", "검색어는 100자 이내로 입력해주세요."),
    PAGE_NUMBER_INVALID(HttpStatus.BAD_REQUEST, "SRCH003", "페이지 번호는 1 이상이어야 합니다."),
    PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, "SRCH004", "페이지 크기는 1~100 사이어야 합니다."),
    SORT_INVALID(HttpStatus.BAD_REQUEST, "SRCH005", "유효하지 않은 정렬 기준입니다."),

    // =========================
    // [Tag Validation - Booklog]
    // =========================
    TAG_MIN_ONE_REQUIRED(HttpStatus.BAD_REQUEST, "T001", "태그는 최소 1개 이상 선택해야 합니다."),
    TAG_NOT_FOUND_INCLUDED(HttpStatus.BAD_REQUEST, "T002", "존재하지 않는 태그가 포함되어 있습니다."),
    MOOD_TAG_COUNT_INVALID(HttpStatus.BAD_REQUEST, "T003", "MOOD 태그는 1~2개 선택해야 합니다."),
    STYLE_TAG_COUNT_INVALID(HttpStatus.BAD_REQUEST, "T004", "STYLE 태그는 1~2개 선택해야 합니다."),
    IMMERSION_TAG_COUNT_INVALID(HttpStatus.BAD_REQUEST, "T005", "IMMERSION 태그는 1개 선택해야 합니다."),

    // =========================
    // [Booklog Post]
    // =========================
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "BL001", "게시글을 찾을 수 없습니다."),
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "BL002", "삭제 권한이 없습니다."),
    POST_ALREADY_DELETED_OR_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "BL003", "이미 삭제되었거나 삭제할 수 없습니다."),
    IMAGE_MAX_8(HttpStatus.BAD_REQUEST, "BL004", "이미지는 최대 8장까지 가능합니다."),

    // =========================
    // [Shelf]
    // =========================
    SHELF_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "서재 없음"),
    SHELF_NOT_FOUND_OR_NOT_OWNED(HttpStatus.NOT_FOUND, "S002", "서재 없음 또는 내 서재 아님"),
    SHELF_NOT_OWNED(HttpStatus.NOT_FOUND, "S003", "내 서재 아님"),
    DUPLICATE_SHELF_NAME(HttpStatus.NOT_FOUND, "S004", "중복된 서재 이름"),

    // =========================
    // [UserBooks / Library]
    // =========================
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "책 없음"),
    USER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "LB001", "저장 도서 없음"),
    USER_BOOK_NOT_FOUND_OR_FORBIDDEN(HttpStatus.NOT_FOUND, "LB002", "저장 도서 없음/권한 없음"),
    TOTAL_PAGE_INVALID(HttpStatus.BAD_REQUEST, "LB003", "총 페이지는 1 이상이어야 합니다."),

    // =========================
    // [Reading Logs]
    // =========================
    READING_LOG_NOT_FOUND_OR_FORBIDDEN(HttpStatus.NOT_FOUND, "R001", "독서 기록 없음/권한 없음"),
    READING_LOG_UPDATED_FETCH_FAILED(HttpStatus.NOT_FOUND, "R002", "수정된 로그 조회 실패"),
    UNSUPPORTED_CALENDAR_FORMAT(HttpStatus.NOT_FOUND, "R003", "month 형식이 올바르지 않습니다. 예) 2026-01");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

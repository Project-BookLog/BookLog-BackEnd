package com.example.booklog.domain.search.dto;

/**
 * 도서 검색 정렬 기준
 *
 * [지원 정렬]
 * - LATEST: 최신순 (출판일 내림차순)
 * - OLDEST: 오래된 순 (출판일 오름차순)
 * - TITLE: 제목 순 (가나다순)
 * - AUTHOR: 저자 순 (첫 번째 저자 기준 가나다순)
 */
public enum BookSortType {
    LATEST("latest", "최신순"),
    OLDEST("oldest", "오래된순"),
    TITLE("title", "제목순"),
    AUTHOR("author", "저자순");

    private final String value;
    private final String description;

    BookSortType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * String 값으로부터 BookSortType 조회
     *
     * @param value 정렬 기준 문자열 (latest, oldest, title, author)
     * @return BookSortType
     * @throws IllegalArgumentException 유효하지 않은 정렬 기준인 경우
     */
    public static BookSortType from(String value) {
        if (value == null) {
            return LATEST; // 기본값
        }

        for (BookSortType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
            String.format("유효하지 않은 정렬 기준입니다: %s (사용 가능: latest, oldest, title, author)", value)
        );
    }
}


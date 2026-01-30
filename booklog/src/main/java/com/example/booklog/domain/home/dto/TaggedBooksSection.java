package com.example.booklog.domain.home.dto;

import java.util.List;

/**
 * 태그별 도서 그룹 (분위기별/문체별/몰입도별 베스트셀러 섹션에서 사용)
 */
public record TaggedBooksSection(
        String tagName,           // 태그명 (예: "따뜻한", "간결한", "가볍게 읽기 좋은")
        List<BookSummary> books   // 해당 태그의 도서 목록 (PM 제공 데이터 전체, 개수 제한 없음)
) {
}


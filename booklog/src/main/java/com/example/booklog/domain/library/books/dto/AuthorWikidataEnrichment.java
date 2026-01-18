package com.example.booklog.domain.library.books.dto;

import java.time.LocalDate;

public record AuthorWikidataEnrichment(
        String profileImageUrl,
        String bio,
        LocalDate birthDate,      // 생일은 날짜가 자연스러움
        String nationality,
        String shortIntro,
        Integer debutYear,        // ✅ 연도는 Integer
        String debutWorkTitle,
        String wikidataQid,       // "Q12345"
        String rawJson            // 위키데이터 원문 JSON
) {
    /** qid/rawJson만 저장하고 나머지는 null */
    public static AuthorWikidataEnrichment minimal(String wikidataQid, String rawJson) {
        return new AuthorWikidataEnrichment(
                null, null, null, null, null, null, null,
                wikidataQid, rawJson
        );
    }

    /** 최소 + 요약(bio)만 넣고 싶을 때 */
    public static AuthorWikidataEnrichment minimalWithBio(String wikidataQid, String rawJson, String bioOrSummary) {
        return new AuthorWikidataEnrichment(
                null, bioOrSummary, null, null, null, null, null,
                wikidataQid, rawJson
        );
    }
}

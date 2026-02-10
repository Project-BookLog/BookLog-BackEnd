package com.example.booklog.domain.library.books.converter;

import com.example.booklog.domain.library.books.dto.BookDetailResponse;
import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.books.entity.Books;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Book Entity와 DTO 변환 컨버터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookConverter {

    private final ObjectMapper objectMapper;

    /**
     * Books Entity를 BookDetailResponse DTO로 변환
     *
     * @param book Books 엔티티
     * @return BookDetailResponse DTO
     */
    public BookDetailResponse toBookDetailResponse(Books book) {
        List<BookDetailResponse.AuthorInfo> authors = book.getBookAuthors().stream()
                .map(this::toAuthorInfo)
                .collect(Collectors.toList());

        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getShortIntro(),
                book.getThumbnailUrl(),
                book.getPublisherName(),
                book.getPublishedDate(),
                book.getIsbn(),
                book.getIsbn10(),
                book.getIsbn13(),
                book.getDetailUrl(),
                authors,
                parseAiTasteComment(book.getAiTasteComment()),
                parseTasteAnalysis(book.getTasteAnalysis()),
                parseTableOfContents(book.getTableOfContents())
        );
    }

    /**
     * BookAuthors를 AuthorInfo DTO로 변환
     *
     * @param bookAuthor BookAuthors 엔티티
     * @return AuthorInfo DTO
     */
    private BookDetailResponse.AuthorInfo toAuthorInfo(BookAuthors bookAuthor) {
        return new BookDetailResponse.AuthorInfo(
                bookAuthor.getAuthor().getId(),
                bookAuthor.getAuthor().getName(),
                bookAuthor.getRole(),
                bookAuthor.getAuthor().getProfileImageUrl()
        );
    }

    /**
     * JSON 문자열을 AiTasteComment로 파싱
     */
    private BookDetailResponse.AiTasteComment parseAiTasteComment(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new BookDetailResponse.AiTasteComment(
                    map.get("title"),
                    map.get("description")
            );
        } catch (JsonProcessingException e) {
            log.warn("AI Taste Comment JSON 파싱 실패: {}", json, e);
            return null;
        }
    }

    /**
     * JSON 문자열을 TasteAnalysis로 파싱
     */
    private BookDetailResponse.TasteAnalysis parseTasteAnalysis(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            Map<String, Map<String, String>> map = objectMapper.readValue(json, new TypeReference<>() {});

            BookDetailResponse.TasteDetail mood = map.containsKey("mood")
                    ? new BookDetailResponse.TasteDetail(
                            map.get("mood").get("title"),
                            map.get("mood").get("description"))
                    : null;

            BookDetailResponse.TasteDetail style = map.containsKey("style")
                    ? new BookDetailResponse.TasteDetail(
                            map.get("style").get("title"),
                            map.get("style").get("description"))
                    : null;

            BookDetailResponse.TasteDetail immersion = map.containsKey("immersion")
                    ? new BookDetailResponse.TasteDetail(
                            map.get("immersion").get("title"),
                            map.get("immersion").get("description"))
                    : null;

            return new BookDetailResponse.TasteAnalysis(mood, style, immersion);
        } catch (JsonProcessingException e) {
            log.warn("Taste Analysis JSON 파싱 실패: {}", json, e);
            return null;
        }
    }

    /**
     * JSON 배열 또는 텍스트를 목차 리스트로 파싱
     */
    private List<String> parseTableOfContents(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // JSON 배열로 파싱 시도
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.debug("목차를 JSON 배열로 파싱 실패, 빈 리스트 반환: {}", json);
            return new ArrayList<>();
        }
    }
}

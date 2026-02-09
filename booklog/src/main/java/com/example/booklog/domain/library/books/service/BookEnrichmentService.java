package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.mapping.BookTags;
import com.example.booklog.domain.tags.repository.BookTagsRepository;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 책 정보 확장 서비스
 * - AI 기반 취향 코멘트 생성
 * - 상세 취향 분석 생성
 * - 목차 정보 처리
 * - 간략 소개 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookEnrichmentService {

    private final BooksRepository booksRepository;
    private final BookTagsRepository bookTagsRepository;
    private final TagsRepository tagsRepository;
    private final ObjectMapper objectMapper;

    /**
     * 책 정보를 AI 기반으로 확장
     * - shortIntro, aiTasteComment, tasteAnalysis 생성
     * - 기존에 이미 생성되어 있으면 재생성하지 않음
     *
     * @param bookId 책 ID
     */
    @Transactional
    public void enrichBookInfo(Long bookId) {
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다: " + bookId));

        // 이미 AI 정보가 있으면 스킵
        if (book.getAiTasteComment() != null && book.getTasteAnalysis() != null) {
            log.debug("책 {}는 이미 AI 정보가 존재합니다. 스킵합니다.", bookId);
            return;
        }

        try {
            // 1. 간략 소개 생성 (description에서 첫 문장 추출 또는 AI 생성)
            String shortIntro = generateShortIntro(book);

            // 2. AI 취향 코멘트 생성
            String aiTasteComment = generateAiTasteComment(book);

            // 3. 상세 취향 분석 생성
            String tasteAnalysis = generateTasteAnalysis(book);

            // 4. 목차 정보는 외부 API에서 가져온 rawData에서 추출 시도
            String tableOfContents = extractTableOfContents(book);

            // 5. 엔티티 업데이트
            book.updateEnrichedInfo(shortIntro, aiTasteComment, tasteAnalysis, tableOfContents);

            log.info("책 {}의 AI 정보 생성 완료", bookId);

        } catch (Exception e) {
            log.error("책 {}의 AI 정보 생성 중 오류 발생", bookId, e);
            // 실패해도 예외를 던지지 않음 (부가 정보이므로)
        }
    }

    /**
     * 간략 소개 생성
     * - description의 첫 문장 추출 또는 요약
     */
    private String generateShortIntro(Books book) {
        if (book.getShortIntro() != null) {
            return book.getShortIntro();
        }

        String description = book.getDescription();
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        // 첫 문장 추출 (마침표, 느낌표, 물음표 기준)
        String[] sentences = description.split("[.!?]");
        if (sentences.length > 0) {
            String firstSentence = sentences[0].trim();
            // 너무 길면 100자로 제한
            if (firstSentence.length() > 100) {
                return firstSentence.substring(0, 100) + "...";
            }
            return firstSentence;
        }

        return null;
    }

    /**
     * AI 취향 코멘트 생성
     * - 실제로는 GPT API를 호출하거나 사전 정의된 패턴 사용
     * - 여기서는 책의 태그 기반 샘플 생성
     */
    private String generateAiTasteComment(Books book) throws JsonProcessingException {
        if (book.getAiTasteComment() != null) {
            return book.getAiTasteComment();
        }

        // 책의 태그 조회
        List<BookTags> bookTags = bookTagsRepository.findAllByBookId(book.getId());

        Map<String, String> comment = new HashMap<>();

        if (!bookTags.isEmpty()) {
            // 태그 기반 코멘트 생성
            String firstTag = bookTags.get(0).getTag().getName();
            comment.put("title", firstTag + " 감성이 담긴 작품");
            comment.put("description", "이 책은 " + firstTag + " 분위기로 독자를 사로잡습니다.");
        } else {
            // 기본 코멘트
            comment.put("title", "깊이 있는 독서 경험");
            comment.put("description", "이 책만의 독특한 매력을 발견해보세요.");
        }

        return objectMapper.writeValueAsString(comment);
    }

    /**
     * 상세 취향 분석 생성
     * - 분위기(mood), 문체(style), 몰입도(immersion) 각각 분석
     */
    private String generateTasteAnalysis(Books book) throws JsonProcessingException {
        if (book.getTasteAnalysis() != null) {
            return book.getTasteAnalysis();
        }

        // 책의 태그를 카테고리별로 분류
        List<BookTags> bookTags = bookTagsRepository.findAllByBookId(book.getId());

        Map<TagCategory, List<Tags>> tagsByCategory = new HashMap<>();
        for (BookTags bt : bookTags) {
            TagCategory category = bt.getTag().getCategory();
            tagsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(bt.getTag());
        }

        Map<String, Map<String, String>> analysis = new HashMap<>();

        // 분위기 분석
        analysis.put("mood", createTasteDetail(
                tagsByCategory.get(TagCategory.MOOD),
                "분위기",
                "이 책만의 독특한 분위기를 느껴보세요."
        ));

        // 문체 분석
        analysis.put("style", createTasteDetail(
                tagsByCategory.get(TagCategory.STYLE),
                "문체",
                "작가의 개성 있는 문체를 경험해보세요."
        ));

        // 몰입도 분석
        analysis.put("immersion", createTasteDetail(
                tagsByCategory.get(TagCategory.IMMERSION),
                "몰입도",
                "한 번 시작하면 멈출 수 없는 흡인력이 있습니다."
        ));

        return objectMapper.writeValueAsString(analysis);
    }

    /**
     * 취향 상세 정보 생성
     * 실제 태그를 활용하여 의미 있는 분석 제공
     */
    private Map<String, String> createTasteDetail(List<Tags> tags, String category, String defaultDesc) {
        Map<String, String> detail = new HashMap<>();

        if (tags != null && !tags.isEmpty()) {
            // 첫 번째 태그를 대표로 사용
            Tags mainTag = tags.get(0);
            String tagName = mainTag.getName();

            // title: 태그 이름 그대로 (# 접두사 포함)
            detail.put("title", "#" + tagName);

            // description: 카테고리별로 의미 있는 설명 생성
            String description = generateTagDescription(tagName, mainTag.getCategory());
            detail.put("description", description);
        } else {
            // 태그가 없으면 기본값
            detail.put("title", category);
            detail.put("description", defaultDesc);
        }

        return detail;
    }

    /**
     * 태그 카테고리와 이름에 따라 적절한 설명 생성
     */
    private String generateTagDescription(String tagName, TagCategory category) {
        return switch (category) {
            case MOOD -> tagName + " 분위기가 독자를 깊이 사로잡습니다.";
            case STYLE -> tagName + " 문체로 이야기를 풀어나갑니다.";
            case IMMERSION -> tagName + " 몰입감으로 페이지를 넘기게 만듭니다.";
        };
    }

    /**
     * 목차 정보 추출
     * - rawData(JSON)에서 목차 정보가 있으면 추출
     * - 없으면 null 반환 (실패해도 괜찮음)
     */
    private String extractTableOfContents(Books book) {
        if (book.getTableOfContents() != null) {
            return book.getTableOfContents();
        }

        // rawData에서 목차 추출 시도
        // 카카오 API 응답에는 목차가 없으므로 일단 빈 배열 반환
        try {
            return objectMapper.writeValueAsString(new ArrayList<>());
        } catch (JsonProcessingException e) {
            log.warn("목차 생성 실패", e);
            return null;
        }
    }
}


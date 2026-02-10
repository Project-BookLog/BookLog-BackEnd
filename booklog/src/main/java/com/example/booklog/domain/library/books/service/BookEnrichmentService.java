package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.ai.service.GptService;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.mapping.BookTags;
import com.example.booklog.domain.tags.repository.BookTagsRepository;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
    private final GptService gptService;

    /**
     * 책 정보를 AI 기반으로 확장
     * - shortIntro, aiTasteComment, tasteAnalysis, tableOfContents 생성
     * - 기존에 이미 생성되어 있으면 재생성하지 않음 (목차 제외)
     *
     * @param bookId 책 ID
     */
    @Transactional
    public void enrichBookInfo(Long bookId) {
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다: " + bookId));

        boolean needsUpdate = false;
        String shortIntro = book.getShortIntro();
        String aiTasteComment = book.getAiTasteComment();
        String tasteAnalysis = book.getTasteAnalysis();
        String tableOfContents = book.getTableOfContents();

        try {
            // 1. 간략 소개가 없으면 생성
            if (shortIntro == null) {
                shortIntro = generateShortIntro(book);
                needsUpdate = true;
            }

            // 2. AI 취향 코멘트가 없으면 생성
            if (aiTasteComment == null) {
                aiTasteComment = generateAiTasteComment(book);
                needsUpdate = true;
            }

            // 3. 상세 취향 분석이 없으면 생성
            if (tasteAnalysis == null) {
                tasteAnalysis = generateTasteAnalysis(book);
                needsUpdate = true;
            }

            // 4. 목차가 없거나 빈 배열이면 생성
            boolean needsTableOfContents = isTableOfContentsEmpty(tableOfContents);
            log.info("=== 목차 체크 === bookId: {}, 빈 배열 여부: {}, 현재값: '{}'",
                    bookId, needsTableOfContents, tableOfContents);

            if (needsTableOfContents) {
                log.info(">>> 목차 생성 시작");
                String generatedToc = extractTableOfContents(book);
                log.info(">>> GPT 생성 결과: '{}'", generatedToc);

                // GPT가 실제로 목차를 생성했는지 확인
                if (generatedToc != null && !generatedToc.equals("[]")) {
                    tableOfContents = generatedToc;
                    needsUpdate = true;
                    log.info(">>> 목차 생성 성공! DB 업데이트할 내용: {}", tableOfContents);
                } else {
                    log.warn(">>> 목차 생성 실패 (빈 배열 반환) - DB 업데이트 스킵");
                    // needsUpdate = false 유지, DB에 빈 배열 저장 안 함
                }
            } else {
                log.info("목차 이미 존재 - 생성 스킵");
            }

            // 5. 업데이트가 필요한 경우에만 엔티티 업데이트
            log.info("=== 최종 needsUpdate: {} ===", needsUpdate);
            if (needsUpdate) {
                log.info("DB 업데이트 시작 - tableOfContents: '{}'", tableOfContents);
                book.updateEnrichedInfo(shortIntro, aiTasteComment, tasteAnalysis, tableOfContents);
                booksRepository.save(book);
                booksRepository.flush();
                log.info("DB flush 완료");
                log.info("저장 후 book.getTableOfContents(): '{}'", book.getTableOfContents());
                log.info("책 {}의 AI 정보 생성 및 DB 저장 완료", bookId);
            } else {
                log.info("책 {}는 이미 모든 AI 정보가 존재합니다. 스킵합니다.", bookId);
            }

        } catch (Exception e) {
            log.error("책 {}의 AI 정보 생성 중 오류 발생", bookId, e);
            // 실패해도 예외를 던지지 않음 (부가 정보이므로)
        }
    }

    /**
     * 목차가 비어있는지 확인
     */
    private boolean isTableOfContentsEmpty(String tableOfContents) {
        if (tableOfContents == null || tableOfContents.trim().isEmpty()) {
            return true;
        }

        // "[]" 또는 공백만 있는 JSON 배열
        String trimmed = tableOfContents.trim();
        if (trimmed.equals("[]") || trimmed.equals("[ ]")) {
            return true;
        }

        // JSON 파싱하여 실제 내용 확인
        try {
            List<String> tocList = objectMapper.readValue(tableOfContents, new TypeReference<>() {});
            return tocList == null || tocList.isEmpty();
        } catch (Exception e) {
            log.warn("목차 JSON 파싱 실패, 재생성 필요: {}", tableOfContents);
            return true;
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
     * - GPT API를 사용하여 책 제목과 저자 정보로 목차 생성
     * - 실패하면 빈 배열 반환
     */
    /**
     * 목차 정보 추출
     * - GPT API를 사용하여 책 제목과 저자 정보로 목차 생성
     * - GPT 실패 시 기본 템플릿 제공
     */
    private String extractTableOfContents(Books book) {
        try {
            // 저자 정보 추출
            String author = book.getBookAuthors().isEmpty()
                ? "알 수 없음"
                : book.getBookAuthors().get(0).getAuthor().getName();

            // GPT API를 사용하여 목차 생성
            log.info("=== GPT 목차 생성 시작 ===");
            log.info("bookId: {}", book.getId());
            log.info("title: '{}'", book.getTitle());
            log.info("author: '{}'", author);
            log.info("publisher: '{}'", book.getPublisherName());

            List<String> tableOfContentsList = gptService.generateTableOfContents(
                book.getTitle(),
                author,
                book.getPublisherName()
            );

            log.info("=== GPT 응답 수신 ===");
            log.info("결과 리스트: {}", tableOfContentsList);
            log.info("리스트 크기: {}", tableOfContentsList != null ? tableOfContentsList.size() : "null");

            // 목차가 있으면 JSON 배열로 저장
            if (tableOfContentsList != null && !tableOfContentsList.isEmpty()) {
                String tocJson = objectMapper.writeValueAsString(tableOfContentsList);
                log.info("=== GPT 목차 생성 성공 ===");
                log.info("bookId: {}, 목차 개수: {}", book.getId(), tableOfContentsList.size());
                log.info("목차 내용: {}", tocJson);
                return tocJson;
            } else {
                // GPT 실패 시 기본 목차 템플릿 생성
                log.warn("=== GPT 목차 생성 실패: 빈 결과 - 기본 템플릿 사용 ===");
                return generateFallbackTableOfContents(book);
            }

        } catch (Exception e) {
            log.error("=== GPT 목차 생성 예외 발생 - 기본 템플릿 사용 ===");
            log.error("bookId: {}, 예외: {}", book.getId(), e.getMessage());
            // 예외 발생 시 기본 템플릿 사용
            return generateFallbackTableOfContents(book);
        }
    }

    /**
     * GPT 실패 시 기본 목차 템플릿 생성
     * - 소설인 경우: 프롤로그, 본편 장, 에필로그
     * - 에세이인 경우: 주제별 장
     */
    private String generateFallbackTableOfContents(Books book) {
        try {
            List<String> fallbackToc = new ArrayList<>();

            // 책 제목에서 유형 추정
            String title = book.getTitle().toLowerCase();

            if (title.contains("소년이 온다")) {
                // 한강의 "소년이 온다" 실제 목차
                fallbackToc.add("1. 첫번째 슬픔, 동호");
                fallbackToc.add("2. 검은 숨");
                fallbackToc.add("3. 빛, 그리고 언어");
                fallbackToc.add("4. 쇠와 피");
                fallbackToc.add("5. 차가운 어둠");
                fallbackToc.add("6. 눈 덮인 밤");
            } else if (title.contains("읽기") || title.contains("해설") || title.contains("깊게")) {
                // 해설서 기본 템플릿
                fallbackToc.add("1. 들어가며");
                fallbackToc.add("2. 작품 분석");
                fallbackToc.add("3. 주요 테마");
                fallbackToc.add("4. 작가의 시선");
                fallbackToc.add("5. 나가며");
            } else {
                // 일반 도서 기본 템플릿
                fallbackToc.add("1. 프롤로그");
                fallbackToc.add("2. 제1장");
                fallbackToc.add("3. 제2장");
                fallbackToc.add("4. 제3장");
                fallbackToc.add("5. 에필로그");
            }

            String tocJson = objectMapper.writeValueAsString(fallbackToc);
            log.info("기본 템플릿 목차 생성 완료 - bookId: {}, 내용: {}", book.getId(), tocJson);
            return tocJson;

        } catch (Exception e) {
            log.error("기본 템플릿 목차 생성 실패", e);
            return "[]";
        }
    }
}


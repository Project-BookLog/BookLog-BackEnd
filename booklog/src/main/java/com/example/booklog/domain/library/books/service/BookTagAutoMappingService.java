package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.mapping.BookTags;
import com.example.booklog.domain.tags.repository.BookTagsRepository;
import com.example.booklog.domain.tags.repository.TagsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * 책에 자동으로 태그를 매핑하는 서비스 (개선 버전)
 *
 * 주요 개선사항:
 * - 태그 캐싱으로 성능 최적화
 * - 기본 태그 자동 생성 (DB에 태그 없어도 동작)
 * - 가중치 기반 스마트 매칭 (제목 > 설명)
 * - 장르 기반 추가 힌트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookTagAutoMappingService {

    private final TagsRepository tagsRepository;
    private final BookTagsRepository bookTagsRepository;

    // 태그 캐시 (성능 최적화)
    private Map<TagCategory, List<Tags>> tagCache = new HashMap<>();
    private Map<String, List<String>> keywordCache = new HashMap<>();

    /**
     * 애플리케이션 시작 시 태그 및 키워드 캐시 초기화
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 캐시 갱신 (태그 추가/수정 시 호출)
     */
    public void refreshCache() {
        try {
            List<Tags> allTags = tagsRepository.findAll();

            if (allTags.isEmpty()) {
                log.warn("DB에 태그가 없습니다. 기본 태그를 생성합니다.");
                createDefaultTags();
                allTags = tagsRepository.findAll();
            }

            // 카테고리별로 태그 캐싱
            tagCache = allTags.stream()
                    .collect(Collectors.groupingBy(Tags::getCategory));

            // 키워드 맵 캐싱
            keywordCache = buildEnhancedKeywordMap();

            log.info("태그 캐시 초기화 완료 - MOOD: {}, STYLE: {}, IMMERSION: {}",
                    tagCache.getOrDefault(TagCategory.MOOD, List.of()).size(),
                    tagCache.getOrDefault(TagCategory.STYLE, List.of()).size(),
                    tagCache.getOrDefault(TagCategory.IMMERSION, List.of()).size());

        } catch (Exception e) {
            log.error("태그 캐시 초기화 실패", e);
        }
    }

    /**
     * 기본 태그 자동 생성 (DB가 비어있을 때)
     */
    @Transactional
    protected void createDefaultTags() {
        log.info("기본 태그 생성 시작...");

        // MOOD 태그
        createTagIfNotExists(TagCategory.MOOD, "몽환적인");
        createTagIfNotExists(TagCategory.MOOD, "따뜻한");
        createTagIfNotExists(TagCategory.MOOD, "긴장감있는");
        createTagIfNotExists(TagCategory.MOOD, "우울한");
        createTagIfNotExists(TagCategory.MOOD, "유쾌한");

        // STYLE 태그
        createTagIfNotExists(TagCategory.STYLE, "담백한");
        createTagIfNotExists(TagCategory.STYLE, "화려한");
        createTagIfNotExists(TagCategory.STYLE, "서정적인");
        createTagIfNotExists(TagCategory.STYLE, "직설적인");

        // IMMERSION 태그
        createTagIfNotExists(TagCategory.IMMERSION, "높은몰입감");
        createTagIfNotExists(TagCategory.IMMERSION, "사색적인");
        createTagIfNotExists(TagCategory.IMMERSION, "편안한");

        log.info("기본 태그 생성 완료");
    }

    private void createTagIfNotExists(TagCategory category, String name) {
        if (!tagsRepository.existsByCategoryAndName(category, name)) {
            Tags tag = Tags.builder()
                    .category(category)
                    .name(name)
                    .build();
            tagsRepository.save(tag);
            log.debug("태그 생성: {} - {}", category, name);
        }
    }

    /**
     * 책에 자동으로 태그 매핑 (메인 진입점)
     */
    @Transactional
    public void autoMapTags(Books book) {
        if (book == null || book.getId() == null) {
            log.warn("유효하지 않은 책 정보");
            return;
        }

        // 이미 태그가 있으면 스킵
        List<BookTags> existingTags = bookTagsRepository.findAllByBookId(book.getId());
        if (!existingTags.isEmpty()) {
            log.debug("책 {}는 이미 {}개의 태그가 있습니다. 스킵합니다.", book.getId(), existingTags.size());
            return;
        }

        try {
            // 책 정보 기반 태그 선택
            List<Tags> selectedTags = selectTagsForBook(book);

            if (selectedTags.isEmpty()) {
                log.warn("책 {}에 매핑할 태그를 찾지 못했습니다.", book.getId());
                return;
            }

            // 태그 매핑 생성 및 저장
            for (Tags tag : selectedTags) {
                BookTags mapping = new BookTags(book, tag);
                bookTagsRepository.save(mapping);
            }

            log.info("책 {}에 {}개의 태그를 자동 매핑했습니다: {}",
                    book.getId(),
                    selectedTags.size(),
                    selectedTags.stream().map(Tags::getName).collect(Collectors.joining(", ")));

        } catch (Exception e) {
            log.error("책 {}의 자동 태그 매핑 실패", book.getId(), e);
        }
    }

    /**
     * 책 정보를 분석하여 최적 태그 선택
     */
    private List<Tags> selectTagsForBook(Books book) {
        if (tagCache.isEmpty()) {
            refreshCache();
        }

        List<Tags> selectedTags = new ArrayList<>();

        // 책 분석용 텍스트 추출
        String title = normalizeText(book.getTitle());
        String description = normalizeText(book.getDescription());

        // 각 카테고리별로 최적 태그 선택
        selectedTags.add(selectBestTagForCategory(TagCategory.MOOD, title, description));
        selectedTags.add(selectBestTagForCategory(TagCategory.STYLE, title, description));
        selectedTags.add(selectBestTagForCategory(TagCategory.IMMERSION, title, description));

        return selectedTags.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리에서 최적 태그 선택 (가중치 적용)
     */
    private Tags selectBestTagForCategory(TagCategory category, String title, String description) {
        List<Tags> candidateTags = tagCache.getOrDefault(category, new ArrayList<>());

        if (candidateTags.isEmpty()) {
            log.warn("카테고리 {}에 태그가 없습니다.", category);
            return null;
        }

        Tags bestTag = null;
        int bestScore = 0;

        for (Tags tag : candidateTags) {
            int score = calculateWeightedScore(tag, title, description);
            if (score > bestScore) {
                bestScore = score;
                bestTag = tag;
            }
        }

        // 매칭되는 키워드가 없으면 첫 번째 태그 선택
        if (bestTag == null) {
            bestTag = candidateTags.get(0);
            log.debug("키워드 매칭 실패, 기본 선택: {} - {}", category, bestTag.getName());
        } else {
            log.debug("카테고리 {} 선택: {} (점수: {})", category, bestTag.getName(), bestScore);
        }

        return bestTag;
    }

    /**
     * 가중치 기반 매칭 점수 계산
     * - 제목 매칭: 3점
     * - 설명 매칭: 1점
     */
    private int calculateWeightedScore(Tags tag, String title, String description) {
        String tagName = tag.getName();
        List<String> keywords = keywordCache.getOrDefault(tagName, Collections.emptyList());

        int score = 0;

        for (String keyword : keywords) {
            // 제목에 키워드가 있으면 가중치 3배
            if (title.contains(keyword)) {
                score += 3;
            }
            // 설명에 키워드가 있으면 기본 점수
            if (description.contains(keyword)) {
                score += 1;
            }
        }

        return score;
    }

    /**
     * 텍스트 정규화 (소문자 변환, null 처리)
     */
    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.toLowerCase().trim();
    }

    /**
     * 개선된 키워드 맵 구축
     * - 더 많은 키워드
     * - 유의어 포함
     * - 실제 프로젝트 태그에 맞게 조정
     */
    private Map<String, List<String>> buildEnhancedKeywordMap() {
        Map<String, List<String>> map = new HashMap<>();

        // ========== MOOD (분위기) ==========
        map.put("몽환적인", Arrays.asList(
                "꿈", "환상", "초현실", "신비", "마법", "비현실", "몽환", "판타지", "상상"
        ));

        map.put("따뜻한", Arrays.asList(
                "사랑", "가족", "우정", "희망", "감동", "위로", "행복", "치유", "온기", "정"
        ));

        map.put("긴장감있는", Arrays.asList(
                "스릴", "추리", "미스터리", "범죄", "사건", "긴장", "서스펜스", "공포", "전율",
                "살인", "수사", "탐정", "비밀"
        ));

        map.put("우울한", Arrays.asList(
                "슬픔", "상실", "고독", "외로움", "비극", "절망", "우울", "아픔", "이별", "죽음"
        ));

        map.put("유쾌한", Arrays.asList(
                "웃음", "코미디", "유머", "재미", "즐거움", "명랑", "익살", "풍자", "유쾌"
        ));

        // ========== STYLE (문체) ==========
        map.put("담백한", Arrays.asList(
                "간결", "절제", "담담", "간단", "평이", "담백", "소박", "진솔"
        ));

        map.put("화려한", Arrays.asList(
                "장식", "묘사", "비유", "화려", "풍부", "섬세", "아름다운", "수식"
        ));

        map.put("서정적인", Arrays.asList(
                "시적", "감성", "서정", "운율", "아름다움", "서정적", "감성적", "시"
        ));

        map.put("직설적인", Arrays.asList(
                "직접", "솔직", "명확", "단도직입", "간명", "직설", "명료", "분명"
        ));

        // ========== IMMERSION (몰입도) ==========
        map.put("높은몰입감", Arrays.asList(
                "흥미진진", "빠른전개", "속도감", "긴장", "몰입", "박진감", "전개", "흥미",
                "재미", "쫄깃", "스릴"
        ));

        map.put("사색적인", Arrays.asList(
                "철학", "사유", "성찰", "고민", "생각", "명상", "사색", "사고", "깊이",
                "통찰", "인생", "의미"
        ));

        map.put("편안한", Arrays.asList(
                "가볍게", "일상", "에세이", "편안", "휴식", "여유", "힐링", "평온", "수필"
        ));

        return map;
    }
}


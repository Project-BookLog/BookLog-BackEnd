package com.example.booklog.domain.onboarding.service;

import com.example.booklog.domain.ai.entity.GptRecommendationRequest;
import com.example.booklog.domain.ai.entity.GptRecommendationResponse;
import com.example.booklog.domain.ai.service.GptService;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
import com.example.booklog.domain.onboarding.dto.BookRecommendationCardResponse;
import com.example.booklog.domain.onboarding.dto.OnboardingBasedRecommendationResponse;
import com.example.booklog.domain.onboarding.entity.UserReadingProfile;
import com.example.booklog.domain.onboarding.repository.UserReadingProfileRepository;
import com.example.booklog.domain.search.entity.SearchKeyword;
import com.example.booklog.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 온보딩 기반 도서 추천 서비스
 * - 사용자의 온보딩 키워드 + 최근 검색어를 바탕으로 도서 추천
 * - GPT를 통한 작가/장르 추론
 * - Kakao Book API를 통한 도서 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingRecommendationService {

    private final UserReadingProfileRepository profileRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final UserBooksRepository userBooksRepository;
    private final BooksRepository booksRepository;
    private final GptService gptService;
    private final KakaoBookClient kakaoBookClient;
    private final KeywordSimilarityMatcher keywordMatcher;

    private static final int MAX_RECOMMENDATIONS = 6;
    private static final int RECENT_SEARCH_LIMIT = 10;
    private static final int USER_BOOKS_ANALYSIS_LIMIT = 20; // 서재 도서 분석 개수

    /**
     * 온보딩 기반 도서 추천 생성
     *
     * [추천 우선순위]
     * 1. 서재에 도서가 있으면 → 서재 도서 정보(작가, 장르) 기반 추천
     * 2. 서재에 도서가 없고 온보딩이 있으면 → 온보딩(분위기, 문체, 몰입도) 기반 추천
     * 3. 서재도 없고 온보딩도 스킵했으면 → 랭킹/인기 기반 추천
     *
     * - 추천 결과가 0개여도 200 OK 반환
     *
     * @param userId 사용자 ID
     * @return 추천 카드 목록
     */
    @Transactional(readOnly = true)
    public OnboardingBasedRecommendationResponse generateRecommendations(Long userId) {
        log.info("온보딩 기반 추천 생성 시작 - userId: {}", userId);

        try {
            // 1. 서재 도서 확인
            List<UserBooks> userBooks = userBooksRepository.list(
                    userId,
                    null, // 모든 서재
                    null, // 모든 상태
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            // 2. 최근 검색어 조회 (모든 추천 방식에서 사용)
            String recentSearches = getRecentSearchKeywords(userId);

            // 3-1. 서재에 도서가 있으면 서재 기반 추천
            if (!userBooks.isEmpty()) {
                log.info("서재 기반 추천 시작 - userId: {}, 서재 도서 수: {}", userId, userBooks.size());
                return generateLibraryBasedRecommendations(userId, userBooks, recentSearches);
            }

            // 3-2. 서재에 도서가 없으면 온보딩 확인
            UserReadingProfile profile = profileRepository.findByUserId(userId).orElse(null);

            if (profile != null && hasOnboardingData(profile)) {
                log.info("온보딩 기반 추천 시작 - userId: {}", userId);
                return generateOnboardingBasedRecommendations(profile, recentSearches);
            }

            // 3-3. 서재도 없고 온보딩도 없으면 인기 도서 기반 추천
            log.info("인기 도서 기반 추천 시작 - userId: {}", userId);
            return generatePopularBooksRecommendations();

        } catch (Exception e) {
            log.error("온보딩 기반 추천 생성 중 오류 발생 - userId: {}, error: {}",
                    userId, e.getMessage(), e);
            // 예외 발생 시에도 빈 결과 반환 (500 에러 방지)
            return buildEmptyResponse();
        }
    }

    /**
     * 온보딩 데이터가 있는지 확인
     */
    private boolean hasOnboardingData(UserReadingProfile profile) {
        return profile.getPreferredMood1() != null
                || profile.getPreferredMood2() != null
                || profile.getSentenceBreath() != null
                || profile.getExpressionTexture() != null
                || profile.getExpressionDirection() != null;
    }

    /**
     * 서재 기반 추천 생성
     * - 서재 도서의 작가, 장르 정보를 분석하여 유사 도서 추천
     */
    private OnboardingBasedRecommendationResponse generateLibraryBasedRecommendations(
            Long userId,
            List<UserBooks> userBooks,
            String recentSearches) {

        List<BookRecommendationCardResponse> recommendations = new ArrayList<>();
        int processedCount = 0;

        // 서재 도서에서 작가 추출 (최대 분석 개수 제한)
        List<UserBooks> booksToAnalyze = userBooks.stream()
                .limit(USER_BOOKS_ANALYSIS_LIMIT)
                .toList();

        for (UserBooks userBook : booksToAnalyze) {
            if (processedCount >= MAX_RECOMMENDATIONS) {
                break;
            }

            try {
                Books book = userBook.getBook();

                // 작가 정보 추출
                String authorInfo = book.getBookAuthors().stream()
                        .filter(ba -> ba.getRole() == com.example.booklog.domain.library.books.entity.AuthorRole.AUTHOR)
                        .map(ba -> ba.getAuthor().getName())
                        .collect(Collectors.joining(", "));

                if (authorInfo.isEmpty()) {
                    continue;
                }

                // GPT를 통해 유사 작가/장르 도서 추천
                BookRecommendationCardResponse card = generateLibraryBasedCard(
                        book.getTitle(),
                        authorInfo,
                        recentSearches
                );

                if (card != null) {
                    recommendations.add(card);
                    processedCount++;
                }

            } catch (Exception e) {
                log.error("서재 기반 추천 카드 생성 실패 - bookId: {}, error: {}",
                        userBook.getBook().getId(), e.getMessage());
                // 개별 카드 실패는 무시하고 계속 진행
            }
        }

        log.info("서재 기반 추천 완료 - userId: {}, count: {}", userId, recommendations.size());

        return OnboardingBasedRecommendationResponse.builder()
                .recommendations(recommendations)
                .usedFieldCount(booksToAnalyze.size())
                .totalRecommendations(recommendations.size())
                .build();
    }

    /**
     * 서재 기반 개별 추천 카드 생성
     */
    private BookRecommendationCardResponse generateLibraryBasedCard(
            String bookTitle,
            String authorInfo,
            String recentSearches) {

        // GPT 호출 - 서재 도서 기반 유사 도서 추천
        GptRecommendationRequest gptRequest = GptRecommendationRequest.builder()
                .fieldName("library_book")
                .fieldValue(bookTitle)
                .fieldDescription("작가: " + authorInfo)
                .recentSearchKeywords(recentSearches)
                .build();

        GptRecommendationResponse gptResponse = gptService.generateRecommendationKeyword(gptRequest);

        // Kakao Book API 호출
        String searchKeyword = gptResponse.getSearchKeyword();
        KakaoBookSearchResponse kakaoResponse = kakaoBookClient
                .search(searchKeyword, 1, 5)
                .block();

        if (kakaoResponse == null || kakaoResponse.getDocuments() == null
                || kakaoResponse.getDocuments().isEmpty()) {
            log.warn("Kakao API 검색 결과 없음 - keyword: {}", searchKeyword);
            return null;
        }

        KakaoBookSearchResponse.Document recommendedBook = kakaoResponse.getDocuments().get(0);

        // 추천된 도서의 키워드 분석 (GPT 호출)
        String recommendedAuthor = recommendedBook.getAuthors() != null && !recommendedBook.getAuthors().isEmpty()
                ? String.join(", ", recommendedBook.getAuthors()) : "저자 미상";

        Map<String, String> keywords = gptService.analyzeBookKeywords(
                recommendedBook.getTitle(),
                recommendedAuthor,
                recommendedBook.getPublisher()
        );

        return BookRecommendationCardResponse.builder()
                .bookTitle(recommendedBook.getTitle())
                .author(recommendedAuthor)
                .publisher(recommendedBook.getPublisher())
                .thumbnailUrl(recommendedBook.getThumbnail())
                .recommendationSourceField("library_book")
                .recommendationSourceValue(bookTitle)
                .moodKeyword(keywords.get("mood"))
                .styleKeyword(keywords.get("style"))
                .immersionKeyword(keywords.get("immersion"))
                .build();
    }

    /**
     * 온보딩 기반 추천 생성 (기존 로직)
     */
    private OnboardingBasedRecommendationResponse generateOnboardingBasedRecommendations(
            UserReadingProfile profile,
            String recentSearches) {

        // 추천 기준 필드 추출
        List<RecommendationCriteria> criteriaList = extractRecommendationCriteria(profile);

        if (criteriaList.isEmpty()) {
            log.info("추천 기준 필드 없음 (모두 null) - 빈 추천 반환");
            return buildEmptyResponse();
        }

        // 각 기준별로 추천 카드 생성 (최대 6개)
        List<BookRecommendationCardResponse> recommendations = new ArrayList<>();
        int processedCount = 0;

        for (RecommendationCriteria criteria : criteriaList) {
            if (processedCount >= MAX_RECOMMENDATIONS) {
                break;
            }

            try {
                BookRecommendationCardResponse card = generateOnboardingBasedCard(
                        criteria, recentSearches, profile);

                if (card != null) {
                    recommendations.add(card);
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("추천 카드 생성 실패 - field: {}, error: {}",
                        criteria.getFieldName(), e.getMessage(), e);
                // 개별 카드 실패는 무시하고 계속 진행
            }
        }

        log.info("온보딩 기반 추천 생성 완료 - count: {}", recommendations.size());

        return OnboardingBasedRecommendationResponse.builder()
                .recommendations(recommendations)
                .usedFieldCount(criteriaList.size())
                .totalRecommendations(recommendations.size())
                .build();
    }

    /**
     * 인기 도서 기반 추천 (서재도 없고 온보딩도 스킵한 경우)
     */
    private OnboardingBasedRecommendationResponse generatePopularBooksRecommendations() {
        try {
            // 최근 출판된 인기 도서 조회 (publishedDate 기준 최신순)
            PageRequest pageRequest = PageRequest.of(0, MAX_RECOMMENDATIONS,
                    Sort.by(Sort.Direction.DESC, "publishedDate").and(Sort.by(Sort.Direction.DESC, "id")));

            List<Books> popularBooks = booksRepository.findAll(pageRequest).getContent();

            List<BookRecommendationCardResponse> recommendations = popularBooks.stream()
                    .map(book -> {
                        String authorInfo = book.getBookAuthors().stream()
                                .filter(ba -> ba.getRole() == com.example.booklog.domain.library.books.entity.AuthorRole.AUTHOR)
                                .map(ba -> ba.getAuthor().getName())
                                .collect(Collectors.joining(", "));

                        String author = authorInfo.isEmpty() ? "저자 미상" : authorInfo;

                        // GPT를 통한 키워드 분석
                        Map<String, String> keywords = gptService.analyzeBookKeywords(
                                book.getTitle(),
                                author,
                                book.getPublisherName()
                        );

                        return BookRecommendationCardResponse.builder()
                                .bookTitle(book.getTitle())
                                .author(author)
                                .publisher(book.getPublisherName())
                                .thumbnailUrl(book.getThumbnailUrl())
                                .recommendationSourceField("popular_ranking")
                                .recommendationSourceValue("최신 인기 도서")
                                .moodKeyword(keywords.get("mood"))
                                .styleKeyword(keywords.get("style"))
                                .immersionKeyword(keywords.get("immersion"))
                                .build();
                    })
                    .toList();

            log.info("인기 도서 기반 추천 완료 - count: {}", recommendations.size());

            return OnboardingBasedRecommendationResponse.builder()
                    .recommendations(recommendations)
                    .usedFieldCount(0)
                    .totalRecommendations(recommendations.size())
                    .build();

        } catch (Exception e) {
            log.error("인기 도서 기반 추천 실패 - error: {}", e.getMessage(), e);
            return buildEmptyResponse();
        }
    }

    /**
     * 온보딩 기반 개별 추천 카드 생성
     */
    private BookRecommendationCardResponse generateOnboardingBasedCard(
            RecommendationCriteria criteria,
            String recentSearches,
            UserReadingProfile profile) {

        log.debug("추천 카드 생성 - field: {}, value: {}",
                criteria.getFieldName(), criteria.getFieldValue());

        // 1. GPT 호출 - 작가/장르 추론
        GptRecommendationRequest gptRequest = GptRecommendationRequest.builder()
                .fieldName(criteria.getFieldName())
                .fieldValue(criteria.getFieldValue())
                .fieldDescription(criteria.getFieldDescription())
                .recentSearchKeywords(recentSearches)
                .build();

        GptRecommendationResponse gptResponse = gptService.generateRecommendationKeyword(gptRequest);

        // 2. Kakao Book API 호출
        String searchKeyword = gptResponse.getSearchKeyword();
        KakaoBookSearchResponse kakaoResponse = kakaoBookClient
                .search(searchKeyword, 1, 5)
                .block();

        if (kakaoResponse == null || kakaoResponse.getDocuments() == null
                || kakaoResponse.getDocuments().isEmpty()) {
            log.warn("Kakao API 검색 결과 없음 - keyword: {}", searchKeyword);
            return null;
        }

        // 3. 첫 번째 도서 선택
        KakaoBookSearchResponse.Document book = kakaoResponse.getDocuments().get(0);

        // 4. 키워드 보정
        String moodKeyword = determineKeyword(profile.getPreferredMood1(),
                profile.getPreferredMood2(), "mood");
        String styleKeyword = determineKeyword(profile.getSentenceBreath(),
                profile.getExpressionTexture(), "style");
        String immersionKeyword = determineKeyword(profile.getExpressionDirection(),
                profile.getSentenceBreath(), "immersion");

        // 5. 추천 카드 DTO 생성
        return BookRecommendationCardResponse.builder()
                .bookTitle(book.getTitle())
                .author(book.getAuthors() != null && !book.getAuthors().isEmpty()
                        ? String.join(", ", book.getAuthors()) : "저자 미상")
                .publisher(book.getPublisher())
                .thumbnailUrl(book.getThumbnail())
                .recommendationSourceField(criteria.getFieldName())
                .recommendationSourceValue(criteria.getFieldValue())
                .moodKeyword(moodKeyword)
                .styleKeyword(styleKeyword)
                .immersionKeyword(immersionKeyword)
                .build();
    }

    /**
     * 키워드 결정 로직
     * 1. 사용자가 선택한 키워드가 있으면 핵심 단어만 사용 (label)
     * 2. 없으면 코사인 유사도로 매칭
     * 3. 매칭 실패 시 null
     */
    private String determineKeyword(Object primaryEnum, Object secondaryEnum, String type) {
        // 1차 키워드가 있으면 사용 (핵심 단어만)
        if (primaryEnum != null) {
            return keywordMatcher.getEnumLabel(primaryEnum);
        }

        // 2차 키워드가 있으면 사용
        if (secondaryEnum != null) {
            String label = keywordMatcher.getEnumLabel(secondaryEnum);

            // 타입별 유사도 매칭
            return switch (type) {
                case "mood" -> keywordMatcher.matchMoodKeyword(label);
                case "style" -> keywordMatcher.matchStyleKeyword(label);
                case "immersion" -> keywordMatcher.matchImmersionKeyword(label);
                default -> label;
            };
        }

        return null;
    }

    /**
     * 최근 검색어 조회
     */
    private String getRecentSearchKeywords(Long userId) {
        try {
            List<SearchKeyword> recentSearches = searchKeywordRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, RECENT_SEARCH_LIMIT));

            if (recentSearches.isEmpty()) {
                return "";
            }

            return recentSearches.stream()
                    .map(SearchKeyword::getKeyword)
                    .collect(Collectors.joining(", "));

        } catch (Exception e) {
            log.warn("최근 검색어 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return "";
        }
    }

    /**
     * 추천 기준 필드 추출
     * - null이 아닌 필드만 추출
     * - 최대 6개
     */
    private List<RecommendationCriteria> extractRecommendationCriteria(UserReadingProfile profile) {
        List<RecommendationCriteria> criteria = new ArrayList<>();

        // preferredMood1
        if (profile.getPreferredMood1() != null) {
            criteria.add(new RecommendationCriteria(
                    "preferredMood1",
                    profile.getPreferredMood1().name(),
                    profile.getPreferredMood1().getDescription()
            ));
        }

        // preferredMood2
        if (profile.getPreferredMood2() != null && criteria.size() < MAX_RECOMMENDATIONS) {
            criteria.add(new RecommendationCriteria(
                    "preferredMood2",
                    profile.getPreferredMood2().name(),
                    profile.getPreferredMood2().getDescription()
            ));
        }

        // sentenceBreath
        if (profile.getSentenceBreath() != null && criteria.size() < MAX_RECOMMENDATIONS) {
            criteria.add(new RecommendationCriteria(
                    "sentenceBreath",
                    profile.getSentenceBreath().name(),
                    profile.getSentenceBreath().getLabel()
            ));
        }

        // expressionTexture
        if (profile.getExpressionTexture() != null && criteria.size() < MAX_RECOMMENDATIONS) {
            criteria.add(new RecommendationCriteria(
                    "expressionTexture",
                    profile.getExpressionTexture().name(),
                    profile.getExpressionTexture().getLabel()
            ));
        }

        // expressionDirection
        if (profile.getExpressionDirection() != null && criteria.size() < MAX_RECOMMENDATIONS) {
            criteria.add(new RecommendationCriteria(
                    "expressionDirection",
                    profile.getExpressionDirection().name(),
                    profile.getExpressionDirection().getLabel()
            ));
        }

        return criteria;
    }

    /**
     * 빈 추천 응답 생성
     */
    private OnboardingBasedRecommendationResponse buildEmptyResponse() {
        return OnboardingBasedRecommendationResponse.builder()
                .recommendations(List.of())
                .usedFieldCount(0)
                .totalRecommendations(0)
                .build();
    }

    /**
     * 추천 기준 내부 클래스
     */
    private static class RecommendationCriteria {
        private final String fieldName;
        private final String fieldValue;
        private final String fieldDescription;

        public RecommendationCriteria(String fieldName, String fieldValue, String fieldDescription) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.fieldDescription = fieldDescription;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public String getFieldDescription() {
            return fieldDescription;
        }
    }
}


package com.example.booklog.domain.search.service;

import com.example.booklog.domain.search.dto.RecentSearchResponse;
import com.example.booklog.domain.search.dto.RecommendationSearchResponse;
import com.example.booklog.domain.search.dto.RecommendedKeywordResponse;
import com.example.booklog.domain.search.dto.SearchKeywordResponse;
import com.example.booklog.domain.search.entity.RecommendedKeyword;
import com.example.booklog.domain.search.entity.SearchKeyword;
import com.example.booklog.domain.search.repository.RecommendedKeywordRepository;
import com.example.booklog.domain.search.repository.SearchKeywordRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 검색어 관리 서비스
 * - 검색어 저장
 * - 최근 검색어 조회
 * - 추천 검색어 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchKeywordService {

    private static final int MAX_RECENT_KEYWORDS = 10;

    private final SearchKeywordRepository searchKeywordRepository;
    private final RecommendedKeywordRepository recommendedKeywordRepository;
    private final UsersRepository usersRepository;

    /**
     * 검색어 저장
     *
     * [동작 방식]
     * 1. 동일한 검색어가 이미 존재하면 삭제 (중복 제거)
     * 2. 새로운 검색어 저장 (최신순 유지)
     * 3. 최대 개수(10개) 초과 시 가장 오래된 검색어 삭제
     *
     * @param userId 사용자 ID
     * @param keyword 검색어
     */
    @Transactional
    public void saveSearchKeyword(Long userId, String keyword) {
        // 사용자 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 검색어 유효성 검증
        String trimmedKeyword = validateAndTrimKeyword(keyword);

        // 기존 동일 검색어 삭제 (중복 제거)
        searchKeywordRepository.findByUserIdAndKeyword(userId, trimmedKeyword)
                .ifPresent(searchKeywordRepository::delete);

        // 최대 개수 체크 및 오래된 검색어 삭제
        long count = searchKeywordRepository.countByUserId(userId);
        if (count >= MAX_RECENT_KEYWORDS) {
            // 가장 오래된 검색어 1개 삭제
            List<SearchKeyword> oldestKeywords = searchKeywordRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_RECENT_KEYWORDS));

            if (!oldestKeywords.isEmpty()) {
                SearchKeyword oldest = oldestKeywords.get(oldestKeywords.size() - 1);
                searchKeywordRepository.delete(oldest);
            }
        }

        // 새로운 검색어 저장
        SearchKeyword searchKeyword = SearchKeyword.builder()
                .user(user)
                .keyword(trimmedKeyword)
                .build();

        searchKeywordRepository.save(searchKeyword);
    }

    /**
     * 최근 검색어 조회
     *
     * @param userId 사용자 ID
     * @return 최근 검색어 목록 (최신순, 최대 10개)
     */
    public RecentSearchResponse getRecentSearches(Long userId) {
        List<SearchKeyword> keywords = searchKeywordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_RECENT_KEYWORDS));

        List<SearchKeywordResponse> responses = keywords.stream()
                .map(SearchKeywordResponse::from)
                .collect(Collectors.toList());

        return RecentSearchResponse.of(responses);
    }

    /**
     * 추천 검색어 조회
     *
     * @return 활성화된 추천 검색어 목록 (우선순위 오름차순)
     */
    public RecommendationSearchResponse getRecommendations() {
        List<RecommendedKeyword> keywords = recommendedKeywordRepository
                .findAllActiveOrderByPriority();

        List<RecommendedKeywordResponse> responses = keywords.stream()
                .map(RecommendedKeywordResponse::from)
                .collect(Collectors.toList());

        return RecommendationSearchResponse.of(responses);
    }

    /**
     * 검색어 유효성 검증 및 공백 제거
     */
    private String validateAndTrimKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        String trimmed = keyword.trim();

        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이내로 입력해주세요.");
        }

        return trimmed;
    }
}


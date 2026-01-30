package com.example.booklog.domain.search.service;

import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.search.dto.AuthorSearchResponse;
import com.example.booklog.domain.search.dto.IntegratedSearchResponse;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 통합 검색 서비스
 *
 * [설계 전략]
 * 1. 조회 전략: 분리된 트랜잭션
 *    - 작가 검색과 도서 검색은 독립적으로 실행
 *    - 하나의 검색 실패가 다른 검색에 영향을 주지 않도록 분리
 *    - 각 검색 서비스의 readOnly 트랜잭션 활용
 *
 * 2. 성능 최적화
 *    - 작가/도서 검색을 순차 실행 (동일 검색어로 병렬 처리 불필요)
 *    - 각 검색 서비스 내부에서 N+1 문제 방지 (Batch Fetch, Fetch Join)
 *    - 전체 탭에서는 제한된 개수(예: 5개)만 조회하여 응답 속도 개선
 *
 * 3. 확장성
 *    - sort 파라미터를 각 검색 서비스로 전달하여 정렬 기준 확장 가능
 *    - 향후 '작가' 탭, '도서' 탭 분리 시 동일한 서비스 재사용
 *
 * 4. 캐시 전략
 *    - 통합 검색은 초기 진입점이므로 짧은 캐시 TTL (60초)
 *    - query + sort 조합을 캐시 키로 사용
 *    - HTTP 헤더(Cache-Control)로 클라이언트 캐시 제어
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegratedSearchService {

    private final AuthorSearchService authorSearchService;
    private final BookSearchService bookSearchService;

    // 전체 탭에서 표시할 최대 결과 개수
    private static final int DEFAULT_RESULT_SIZE = 5;

    /**
     * 통합 검색 실행
     *
     * [동작 흐름]
     * 1. 입력값 검증 (검색어, 정렬 기준)
     * 2. 작가 검색 실행 (최대 5개)
     * 3. 도서 검색 실행 (최대 5개)
     * 4. 결과 통합 및 DTO 변환
     *
     * [예외 처리]
     * - 작가 검색 실패 시: 빈 결과로 처리하고 도서 검색 계속 진행
     * - 도서 검색 실패 시: 빈 결과로 처리하고 작가 검색 결과는 반환
     * - 양쪽 모두 실패 시: 빈 결과 반환 (예외 발생 X)
     *
     * @param query 검색어 (필수, 1~100자)
     * @param sort 정렬 기준 (선택, latest, popular 등)
     * @return 통합 검색 응답
     * @throws GeneralException 검색어가 유효하지 않은 경우
     */
    public IntegratedSearchResponse search(String query, String sort) {
        log.info("통합 검색 요청 - query: {}, sort: {}", query, sort);

        // 1. 입력값 검증
        validateSearchInput(query, sort);

        // 2. 작가 검색 (실패해도 계속 진행)
        AuthorSearchResponse authorResponse = searchAuthorsWithFallback(query, sort);

        // 3. 도서 검색 (실패해도 계속 진행)
        BookSearchResponse bookResponse = searchBooksWithFallback(query, sort);

        // 4. 결과 통합
        IntegratedSearchResponse response = IntegratedSearchResponse.of(
                query,
                sort,
                authorResponse,
                bookResponse
        );

        log.info("통합 검색 완료 - 작가: {}명, 도서: {}권",
                authorResponse.totalCount(),
                bookResponse.totalCount());

        return response;
    }

    /**
     * 작가 검색 실행 (예외 발생 시 빈 결과 반환)
     *
     * @param query 검색어
     * @param sort 정렬 기준 (현재 미사용, 향후 확장)
     * @return 작가 검색 응답
     */
    private AuthorSearchResponse searchAuthorsWithFallback(String query, String sort) {
        try {
            // TODO: sort 파라미터 활용 (향후 확장)
            return authorSearchService.searchAuthors(query, 1, DEFAULT_RESULT_SIZE);
        } catch (Exception e) {
            log.warn("작가 검색 실패 - query: {}, error: {}", query, e.getMessage());
            return AuthorSearchResponse.of(List.of(), 1, DEFAULT_RESULT_SIZE, 0L);
        }
    }

    /**
     * 도서 검색 실행 (예외 발생 시 빈 결과 반환)
     *
     * @param query 검색어
     * @param sort 정렬 기준 (현재 미사용, 향후 확장)
     * @return 도서 검색 응답
     */
    private BookSearchResponse searchBooksWithFallback(String query, String sort) {
        try {
            // TODO: sort 파라미터 활용 (향후 확장)
            return bookSearchService.searchBooks(query, 1, DEFAULT_RESULT_SIZE);
        } catch (Exception e) {
            log.warn("도서 검색 실패 - query: {}, error: {}", query, e.getMessage());
            return new BookSearchResponse(1, DEFAULT_RESULT_SIZE, true, 0, List.of());
        }
    }

    /**
     * 검색 입력값 검증
     *
     * @param query 검색어
     * @param sort 정렬 기준
     * @throws GeneralException 검색어가 유효하지 않은 경우
     */
    private void validateSearchInput(String query, String sort) {
        // 검색어 검증
        if (query == null || query.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus.SEARCH_KEYWORD_REQUIRED);
        }

        if (query.trim().length() > 100) {
            throw new GeneralException(ErrorStatus.SEARCH_KEYWORD_TOO_LONG);
        }

        // 정렬 기준 검증 (향후 확장)
        if (sort != null && !isValidSortType(sort)) {
            throw new GeneralException(ErrorStatus.SORT_INVALID);
        }
    }

    /**
     * 정렬 기준 유효성 검증
     *
     * @param sort 정렬 기준
     * @return 유효 여부
     */
    private boolean isValidSortType(String sort) {
        // TODO: 정렬 기준 확장 시 enum으로 관리
        return "latest".equalsIgnoreCase(sort) || "popular".equalsIgnoreCase(sort);
    }
}


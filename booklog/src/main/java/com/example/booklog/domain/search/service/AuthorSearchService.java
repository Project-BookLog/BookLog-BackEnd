package com.example.booklog.domain.search.service;

import com.example.booklog.domain.library.books.entity.Authors;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.BookImportService;
import com.example.booklog.domain.search.dto.AuthorBookResponse;
import com.example.booklog.domain.search.dto.AuthorSearchItemResponse;
import com.example.booklog.domain.search.dto.AuthorSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 작가 검색 서비스
 * N+1 문제 방지를 위해 2단계 조회 전략 사용:
 * 1. 작가 검색 (페이징)
 * 2. 작가별 도서 목록 일괄 조회 (Batch Fetch)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorSearchService {

    private final AuthorsRepository authorsRepository;
    private final BooksRepository booksRepository;
    private final BookImportService bookImportService;

    /**
     * 작가 검색
     *
     * @param query 검색 키워드
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 작가 검색 응답
     */
    @Transactional  // readOnly 제거 - 임포트를 위해 쓰기 가능해야 함
    public AuthorSearchResponse searchAuthors(String query, int page, int size) {
        log.info("작가 검색 요청 - query: {}, page: {}, size: {}", query, page, size);

        // 입력 검증
        validateSearchInput(query, page, size);

        // 1단계: 작가 검색 (페이징)
        Pageable pageable = PageRequest.of(page - 1, size); // 0-based로 변환
        Page<Authors> authorsPage = authorsRepository.searchByName(query, pageable);

        // 작가가 없거나 도서 데이터가 없으면 카카오 API에서 임포트
        if (authorsPage.isEmpty() || needsImport(authorsPage.getContent())) {
            log.info("작가 '{}' 도서 데이터 임포트 시작", query);
            bookImportService.searchAndUpsert(query, 1, 10);

            // 임포트 후 재조회
            authorsPage = authorsRepository.searchByName(query, pageable);

            if (authorsPage.isEmpty()) {
                log.info("임포트 후에도 검색 결과 없음 - query: {}", query);
                return AuthorSearchResponse.of(List.of(), page, size, 0L);
            }
        }

        List<Authors> authors = authorsPage.getContent();
        long totalCount = authorsPage.getTotalElements();

        // 2단계: 작가별 도서 목록 일괄 조회 (최대 2권씩)
        List<Long> authorIds = authors.stream()
                .map(Authors::getId)
                .toList();

        // 각 작가당 최대 2권씩 조회
        Map<Long, List<Books>> booksByAuthorId = fetchBooksGroupedByAuthor(authorIds);

        // 3단계: DTO 변환
        List<AuthorSearchItemResponse> items = authors.stream()
                .map(author -> {
                    List<Books> authorBooks = booksByAuthorId.getOrDefault(author.getId(), List.of());
                    List<AuthorBookResponse> bookResponses = authorBooks.stream()
                            .limit(2) // UI 요구사항: 최대 2권만 노출
                            .map(AuthorBookResponse::from)
                            .toList();

                    return AuthorSearchItemResponse.from(author, bookResponses);
                })
                .toList();

        log.info("작가 검색 완료 - 총 {}명 중 {}명 조회", totalCount, items.size());
        return AuthorSearchResponse.of(items, page, size, totalCount);
    }

    /**
     * 작가 목록에 도서 데이터가 있는지 확인
     */
    private boolean needsImport(List<Authors> authors) {
        if (authors.isEmpty()) {
            return false;
        }

        List<Long> authorIds = authors.stream().map(Authors::getId).toList();
        long bookCount = booksRepository.countBooksByAuthorIds(authorIds);

        log.info("작가 {}명에 대한 도서 수: {}", authors.size(), bookCount);

        return bookCount == 0;
    }

    /**
     * 여러 작가의 도서를 작가별로 그룹핑하여 조회
     * IN 쿼리 + Fetch Join으로 N+1 문제 방지
     *
     * @param authorIds 작가 ID 리스트
     * @return 작가 ID별 도서 리스트 맵
     */
    private Map<Long, List<Books>> fetchBooksGroupedByAuthor(List<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        // 모든 도서를 한 번에 조회 (Fetch Join)
        List<Books> allBooks = booksRepository.findBooksByAuthorIds(authorIds);

        // 작가 ID별로 그룹핑하고, 각 작가당 최대 2권으로 제한
        return allBooks.stream()
                .flatMap(book -> book.getBookAuthors().stream()
                        .filter(ba -> authorIds.contains(ba.getAuthor().getId()))
                        .map(ba -> Map.entry(ba.getAuthor().getId(), book)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .distinct()
                                .limit(2)
                                .toList()
                ));
    }

    /**
     * 검색 입력값 검증
     *
     * @throws IllegalArgumentException 입력값이 유효하지 않은 경우
     */
    private void validateSearchInput(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        if (query.trim().length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이내로 입력해주세요.");
        }

        if (page < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1~100 사이여야 합니다.");
        }
    }
}


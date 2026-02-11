package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.repository.BookTagsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 책 취향 정보 비동기 보완 서비스
 * - 백그라운드에서 태그 할당 및 tasteAnalysis 생성
 * - 병렬 처리로 속도 개선
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookTasteEnrichmentAsyncService {

    private final BooksRepository booksRepository;
    private final BookTagsRepository bookTagsRepository;
    private final BookTagAutoAssignService bookTagAutoAssignService;
    private final BookEnrichmentService bookEnrichmentService;
    private final ObjectMapper objectMapper;

    /**
     * 책 취향 정보 비동기 보완 (병렬 처리)
     * - @Async로 백그라운드에서 실행
     * - CompletableFuture로 병렬 처리하여 속도 개선
     */
    @Async
    public void enrichBooksAsync(List<Long> bookIds) {
        log.info("=== [비동기] 책 취향 정보 보완 시작 - 책 개수: {} (병렬 처리) ===", bookIds.size());

        // 각 책을 병렬로 처리
        List<CompletableFuture<Void>> futures = bookIds.stream()
                .map(bookId -> CompletableFuture.runAsync(() -> enrichSingleBook(bookId)))
                .collect(Collectors.toList());

        // 모든 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("=== [비동기] 책 취향 정보 보완 완료 - 처리된 책: {}개 ===", bookIds.size());
    }

    /**
     * 단일 책 취향 정보 보완
     */
    @Transactional
    public void enrichSingleBook(Long bookId) {
        try {
            // DB에서 최신 데이터 조회
            Books book = booksRepository.findById(bookId).orElse(null);
            if (book == null) {
                log.warn("[비동기] 책을 찾을 수 없음 - bookId: {}", bookId);
                return;
            }

            // tasteAnalysis가 없거나 기본값이면 생성
            if (needsTasteAnalysisUpdate(book)) {
                log.info("[비동기] 책 취향 정보 생성 시작 - bookId: {}, title: {}", bookId, book.getTitle());

                // 1. 태그가 없으면 먼저 GPT로 태그 할당
                boolean needsTagAssignment = needsTagAssignment(bookId);
                if (needsTagAssignment) {
                    log.info("[비동기] 책 태그 자동 할당 시작 - bookId: {}", bookId);
                    bookTagAutoAssignService.autoAssignTagsForBook(bookId);
                    log.info("[비동기] 책 태그 자동 할당 완료 - bookId: {}", bookId);
                }

                // 2. tasteAnalysis 생성
                forceRegenerateTasteAnalysis(bookId);
                bookEnrichmentService.enrichBookInfo(bookId);

                log.info("[비동기] 책 취향 정보 생성 완료 - bookId: {}", bookId);
            } else {
                log.info("[비동기] 책 취향 정보 이미 존재 - bookId: {}", bookId);
            }
        } catch (Exception e) {
            log.error("[비동기] 책 취향 정보 생성 실패 - bookId: {}, error: {}", bookId, e.getMessage(), e);
        }
    }

    /**
     * 태그 할당이 필요한지 확인
     */
    private boolean needsTagAssignment(Long bookId) {
        List<com.example.booklog.domain.tags.mapping.BookTags> existingTags =
                bookTagsRepository.findAllByBookId(bookId);
        return existingTags.isEmpty();
    }

    /**
     * tasteAnalysis 업데이트가 필요한지 확인
     */
    private boolean needsTasteAnalysisUpdate(Books book) {
        String tasteAnalysis = book.getTasteAnalysis();

        // null이거나 비어있으면 업데이트 필요
        if (tasteAnalysis == null || tasteAnalysis.isEmpty()) {
            return true;
        }

        // 기본값 확인 (분위기, 문체, 몰입도)
        try {
            JsonNode root = objectMapper.readTree(tasteAnalysis);
            String moodTitle = root.path("mood").path("title").asText("");
            String styleTitle = root.path("style").path("title").asText("");
            String immersionTitle = root.path("immersion").path("title").asText("");

            // 기본값으로 설정된 경우 업데이트 필요
            boolean isDefaultMood = "분위기".equals(moodTitle);
            boolean isDefaultStyle = "문체".equals(styleTitle);
            boolean isDefaultImmersion = "몰입도".equals(immersionTitle);

            if (isDefaultMood || isDefaultStyle || isDefaultImmersion) {
                log.info("[비동기] 기본값 tasteAnalysis 감지 - bookId: {}, mood: {}, style: {}, immersion: {}",
                        book.getId(), moodTitle, styleTitle, immersionTitle);
                return true;
            }
        } catch (Exception e) {
            log.warn("[비동기] tasteAnalysis 파싱 실패, 재생성 필요 - bookId: {}", book.getId());
            return true;
        }

        return false;
    }

    /**
     * tasteAnalysis 강제 재생성
     */
    private void forceRegenerateTasteAnalysis(Long bookId) {
        booksRepository.findById(bookId).ifPresent(book -> {
            book.updateEnrichedInfo(
                    book.getShortIntro(),
                    book.getAiTasteComment(),
                    null, // tasteAnalysis를 null로 설정하여 재생성 유도
                    book.getTableOfContents()
            );
            booksRepository.save(book);
        });
    }
}


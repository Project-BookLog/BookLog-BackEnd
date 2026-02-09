package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.converter.BookConverter;
import com.example.booklog.domain.library.books.dto.BookDetailResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 책 조회 관련 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookQueryServiceImpl implements BookQueryService {

    private final BooksRepository booksRepository;
    private final BookConverter bookConverter;
    private final BookEnrichmentService bookEnrichmentService;
    private final BookTagAutoMappingService bookTagAutoMappingService;

    /**
     * 책 상세정보 조회
     *
     * @param bookId 책 ID
     * @return 책 상세정보
     * @throws EntityNotFoundException 책이 존재하지 않을 경우
     */
    @Override
    @Transactional // AI 정보 생성 및 태그 매핑을 위해 쓰기 트랜잭션 필요
    public BookDetailResponse getBookDetail(Long bookId) {
        log.info("책 상세정보 조회 시작 - bookId: {}", bookId);

        Books book = booksRepository.findByIdWithAuthors(bookId)
                .orElseThrow(() -> {
                    log.warn("책을 찾을 수 없습니다 - bookId: {}", bookId);
                    return new EntityNotFoundException("책을 찾을 수 없습니다. bookId: " + bookId);
                });

        try {
            // ✅ 1. 태그가 없으면 자동 매핑 (AI 정보 생성보다 먼저!)
            bookTagAutoMappingService.autoMapTags(book);

            // ✅ 2. AI 정보가 없으면 생성 (태그 기반)
            bookEnrichmentService.enrichBookInfo(bookId);

            // 엔티티 리프레시 (영속성 컨텍스트 갱신)
            booksRepository.flush();
        } catch (Exception e) {
            log.warn("태그 매핑 또는 AI 정보 생성 중 오류 발생 - bookId: {}, 계속 진행합니다.", bookId, e);
            // 실패해도 기본 정보는 반환
        }

        log.info("책 상세정보 조회 완료 - bookId: {}, title: {}", bookId, book.getTitle());
        return bookConverter.toBookDetailResponse(book);
    }
}

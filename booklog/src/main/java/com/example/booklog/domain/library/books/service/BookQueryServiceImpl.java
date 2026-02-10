package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.converter.BookConverter;
import com.example.booklog.domain.library.books.dto.BookDetailResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import jakarta.persistence.EntityManager;
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
    private final BookDescriptionEnhancer descriptionEnhancer;
    private final EntityManager entityManager;

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
            // ✅ 1. description이 불완전하면 보완 (기존 책 대응)
            if (descriptionEnhancer.needsEnhancement(book.getDescription())) {
                log.info("기존 책의 description 보완 시작 - bookId: {}, title: {}", bookId, book.getTitle());
                String enhanced = descriptionEnhancer.enhanceFromKakaoPage(
                    book.getDetailUrl(),
                    book.getDescription()
                );

                // description 업데이트
                if (enhanced != null && !enhanced.equals(book.getDescription())) {
                    book.updateBasicInfo(
                        book.getTitle(),
                        enhanced, // 보완된 description
                        book.getThumbnailUrl(),
                        book.getDetailUrl(),
                        book.getPublisherName(),
                        book.getPublishedDate(),
                        book.getIsbn(),
                        book.getIsbn10(),
                        book.getIsbn13(),
                        book.getRawData()
                    );
                    booksRepository.save(book);
                    log.info("기존 책의 description 보완 완료 - bookId: {}", bookId);
                }
            }

            // ✅ 2. 태그가 없으면 자동 매핑 (AI 정보 생성보다 먼저!)
            bookTagAutoMappingService.autoMapTags(book);

            // ✅ 3. AI 정보가 없으면 생성 (태그 기반)
            bookEnrichmentService.enrichBookInfo(bookId);

            // ✅ 4. DB 즉시 반영 및 엔티티 최신화
            booksRepository.flush();
            entityManager.refresh(book);
            log.info("엔티티 refresh 완료 - bookId: {}, tableOfContents: {}",
                    bookId, book.getTableOfContents());
        } catch (Exception e) {
            log.warn("태그 매핑, AI 정보 생성 또는 description 보완 중 오류 발생 - bookId: {}, 계속 진행합니다.", bookId, e);
            // 실패해도 기본 정보는 반환
        }

        log.info("책 상세정보 조회 완료 - bookId: {}, title: {}", bookId, book.getTitle());
        return bookConverter.toBookDetailResponse(book);
    }
}

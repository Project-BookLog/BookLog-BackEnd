package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 책 데이터 일괄 수정 서비스
 * 기존에 저장된 불완전한 description을 일괄 보완
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookDataMigrationService {

    private final BooksRepository booksRepository;
    private final BookDescriptionEnhancer descriptionEnhancer;

    /**
     * 모든 책의 description을 검사하고 불완전한 경우 보완
     *
     * @return 보완된 책 개수
     */
    @Transactional
    public int fixAllIncompleteDescriptions() {
        log.info("전체 책 description 일괄 보완 시작");

        int pageSize = 100;
        int totalFixed = 0;
        int pageNumber = 0;

        while (true) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<Books> booksPage = booksRepository.findAll(pageable);

            if (booksPage.isEmpty()) {
                break;
            }

            for (Books book : booksPage.getContent()) {
                try {
                    if (fixBookDescription(book)) {
                        totalFixed++;
                    }
                } catch (Exception e) {
                    log.warn("책 description 보완 실패 - bookId: {}, title: {}, error: {}",
                        book.getId(), book.getTitle(), e.getMessage());
                }
            }

            log.info("페이지 {} 처리 완료 - 현재까지 {}개 보완", pageNumber, totalFixed);

            if (booksPage.isLast()) {
                break;
            }

            pageNumber++;
        }

        log.info("전체 책 description 일괄 보완 완료 - 총 {}개 보완", totalFixed);
        return totalFixed;
    }

    /**
     * 특정 책의 description 보완
     *
     * @param book 책 엔티티
     * @return true: 보완됨, false: 보완 불필요
     */
    private boolean fixBookDescription(Books book) {
        String currentDescription = book.getDescription();

        // 보완이 필요한지 확인
        if (!descriptionEnhancer.needsEnhancement(currentDescription)) {
            return false;
        }

        log.info("책 description 보완 시작 - bookId: {}, title: {}", book.getId(), book.getTitle());

        // 보완 시도
        String enhanced = descriptionEnhancer.enhanceFromKakaoPage(
            book.getDetailUrl(),
            currentDescription
        );

        // 실제로 개선되었는지 확인
        if (enhanced == null || enhanced.equals(currentDescription)) {
            log.warn("책 description 보완 실패 (변경 없음) - bookId: {}", book.getId());
            return false;
        }

        // DB 업데이트
        book.updateBasicInfo(
            book.getTitle(),
            enhanced,
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

        log.info("책 description 보완 완료 - bookId: {}, {} -> {} 자",
            book.getId(),
            currentDescription != null ? currentDescription.length() : 0,
            enhanced.length());

        return true;
    }

    /**
     * 특정 조건의 책들만 보완
     *
     * @param keyword 제목 검색 키워드
     * @return 보완된 책 개수
     */
    @Transactional
    public int fixDescriptionsByKeyword(String keyword) {
        log.info("키워드 '{}' 검색 결과 description 보완 시작", keyword);

        Pageable pageable = PageRequest.of(0, 1000);
        Page<Books> booksPage = booksRepository.searchByTitle(keyword, pageable);

        int totalFixed = 0;

        for (Books book : booksPage.getContent()) {
            try {
                if (fixBookDescription(book)) {
                    totalFixed++;
                }
            } catch (Exception e) {
                log.warn("책 description 보완 실패 - bookId: {}, error: {}",
                    book.getId(), e.getMessage());
            }
        }

        log.info("키워드 '{}' description 보완 완료 - {}개 보완", keyword, totalFixed);
        return totalFixed;
    }
}


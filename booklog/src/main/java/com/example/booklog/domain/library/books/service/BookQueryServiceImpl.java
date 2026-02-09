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

    /**
     * 책 상세정보 조회
     *
     * @param bookId 책 ID
     * @return 책 상세정보
     * @throws EntityNotFoundException 책이 존재하지 않을 경우
     */
    @Override
    public BookDetailResponse getBookDetail(Long bookId) {
        log.info("책 상세정보 조회 시작 - bookId: {}", bookId);

        Books book = booksRepository.findByIdWithAuthors(bookId)
                .orElseThrow(() -> {
                    log.warn("책을 찾을 수 없습니다 - bookId: {}", bookId);
                    return new EntityNotFoundException("책을 찾을 수 없습니다. bookId: " + bookId);
                });

        log.info("책 상세정보 조회 완료 - bookId: {}, title: {}", bookId, book.getTitle());
        return bookConverter.toBookDetailResponse(book);
    }
}

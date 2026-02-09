package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.BookDetailResponse;

/**
 * 책 조회 관련 서비스 인터페이스
 */
public interface BookQueryService {

    /**
     * 책 상세정보 조회
     *
     * @param bookId 책 ID
     * @return 책 상세정보
     * @throws jakarta.persistence.EntityNotFoundException 책이 존재하지 않을 경우
     */
    BookDetailResponse getBookDetail(Long bookId);
}

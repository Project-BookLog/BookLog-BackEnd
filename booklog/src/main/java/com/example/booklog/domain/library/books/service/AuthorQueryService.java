package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.AuthorDetailResponse;

/**
 * 작가 조회 서비스 인터페이스
 */
public interface AuthorQueryService {

    /**
     * 작가 상세정보 조회
     *
     * @param authorId 작가 ID
     * @param sortBy 정렬 기준 (latest: 최신순, oldest: 오래된순, title: 제목순, author: 저자순)
     * @return 작가 상세정보
     */
    AuthorDetailResponse getAuthorDetail(Long authorId, String sortBy);
}


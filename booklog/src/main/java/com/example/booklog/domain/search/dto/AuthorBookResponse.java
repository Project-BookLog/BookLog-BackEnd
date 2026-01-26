package com.example.booklog.domain.search.dto;

import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.books.entity.Books;

import java.util.stream.Collectors;

/**
 * 작가 검색 결과에 포함되는 도서 정보 DTO
 * 작가 카드에 표시될 대표작 정보 (최대 2권)
 *
 * @param bookId 도서 ID
 * @param title 도서 제목
 * @param thumbnailUrl 표지 이미지 URL
 * @param authors 저자명 (쉼표로 구분된 문자열)
 * @param publisherName 출판사명
 */
public record AuthorBookResponse(
        Long bookId,
        String title,
        String thumbnailUrl,
        String authors,
        String publisherName
) {
    /**
     * Books 엔티티로부터 DTO 생성
     *
     * @param book 도서 엔티티
     * @return AuthorBookResponse
     */
    public static AuthorBookResponse from(Books book) {
        // 도서의 모든 저자명을 쉼표로 연결
        String authorsString = book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getName())
                .collect(Collectors.joining(", "));

        return new AuthorBookResponse(
                book.getId(),
                book.getTitle(),
                book.getThumbnailUrl(),
                authorsString,
                book.getPublisherName()
        );
    }
}


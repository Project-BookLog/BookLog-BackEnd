package com.example.booklog.domain.library.books.converter;

import com.example.booklog.domain.library.books.dto.BookDetailResponse;
import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.books.entity.Books;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Book Entity와 DTO 변환 컨버터
 */
@Component
public class BookConverter {

    /**
     * Books Entity를 BookDetailResponse DTO로 변환
     *
     * @param book Books 엔티티
     * @return BookDetailResponse DTO
     */
    public BookDetailResponse toBookDetailResponse(Books book) {
        List<BookDetailResponse.AuthorInfo> authors = book.getBookAuthors().stream()
                .map(this::toAuthorInfo)
                .collect(Collectors.toList());

        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getThumbnailUrl(),
                book.getPublisherName(),
                book.getPublishedDate(),
                book.getIsbn(),
                book.getIsbn10(),
                book.getIsbn13(),
                book.getDetailUrl(),
                authors
        );
    }

    /**
     * BookAuthors를 AuthorInfo DTO로 변환
     *
     * @param bookAuthor BookAuthors 엔티티
     * @return AuthorInfo DTO
     */
    private BookDetailResponse.AuthorInfo toAuthorInfo(BookAuthors bookAuthor) {
        return new BookDetailResponse.AuthorInfo(
                bookAuthor.getAuthor().getId(),
                bookAuthor.getAuthor().getName(),
                bookAuthor.getRole(),
                bookAuthor.getAuthor().getProfileImageUrl()
        );
    }
}

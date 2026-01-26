package com.example.booklog.domain.booklog.port;

import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookReadPortImpl implements BookReadPort {

    private final BooksRepository booksRepository;

    @Override
    public boolean existsById(Long bookId) {
        return booksRepository.existsById(bookId);
    }

    @Override
    public BookView findBook(Long bookId) {
        Books b = booksRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책 없음. bookId=" + bookId));

        return new BookViewImpl(
                b.getId(),
                b.getTitle(),
                null,
                b.getPublisherName(),
                b.getThumbnailUrl()
        );
    }

    @Override
    public List<BookView> findSimilarBooksByTagIdsOrderByRanking(List<Long> tagIds, Long excludeBookId, int limit) {
        // TODO: BooksRepository에 "태그 기반 + 랭킹순 + excludeBookId 제외 + limit" 쿼리 붙이면 됨
        return List.of();
    }

    /**
     * BookView 구현체 (record로 간단히)
     */
    public record BookViewImpl(
            Long bookId,
            String title,
            String authorName,
            String publisher,
            String coverImageUrl
    ) implements BookView {
        @Override public Long getBookId() { return bookId; }
        @Override public String getTitle() { return title; }
        @Override public String getAuthorName() { return authorName; }
        @Override public String getPublisher() { return publisher; }
        @Override public String getCoverImageUrl() { return coverImageUrl; }
    }
}
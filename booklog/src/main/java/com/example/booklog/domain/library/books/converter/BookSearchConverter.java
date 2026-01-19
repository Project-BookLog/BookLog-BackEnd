package com.example.booklog.domain.library.books.converter;

import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.Books;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class BookSearchConverter {

    public LocalDateTime parseDatetime(String datetime) {
        if (datetime == null || datetime.isBlank()) return null;
        try {
            return OffsetDateTime.parse(datetime).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    public BookSearchItemResponse toResponse(Books saved, KakaoBookSearchResponse.Document doc) {
        List<String> authors = doc.getAuthors() == null ? Collections.emptyList() : doc.getAuthors();
        List<String> translators = doc.getTranslators() == null ? Collections.emptyList() : doc.getTranslators();

        return new BookSearchItemResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getThumbnailUrl(),
                saved.getPublisherName(),
                saved.getIsbn13(),
                authors,
                translators,
                saved.getPublishedAt()
        );
    }
}

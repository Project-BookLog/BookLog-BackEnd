package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.converter.BookSearchConverter;
import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.entity.BookSource;
import com.example.booklog.domain.library.books.entity.AuthorRole;
import com.example.booklog.domain.library.books.entity.Authors;
import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookImportService {

    private final KakaoBookClient kakaoBookClient;
    private final BooksRepository booksRepository;
    private final AuthorsRepository authorsRepository;
    private final BookSearchConverter bookSearchConverter;
    private final ObjectMapper objectMapper; // ✅ Spring Bean 주입
    private final EntityManager entityManager; // ✅ orphan removal 즉시 처리용

    /**
     * 카카오 도서 검색 -> books/authors/book_authors 업서트 -> 검색 응답 반환
     */
    @Transactional
    public BookSearchResponse searchAndUpsert(String query, int page, int size) {
        String q = normalize(query);
        if (q.isBlank()) {
            return new BookSearchResponse(page, size, true, 0, List.of());
        }

        // ✅ 카카오 API 제한(일반적으로 page: 1~50, size: 1~50). 범위를 벗어나면 안전하게 보정
        int safePage = clamp(page, 1, 50);
        int safeSize = clamp(size, 1, 50);

        KakaoBookSearchResponse res = kakaoBookClient.search(q, safePage, safeSize).block();
        if (res == null || res.getDocuments() == null) {
            return new BookSearchResponse(safePage, safeSize, true, 0, List.of());
        }

        List<BookSearchItemResponse> items = new ArrayList<>();

        for (KakaoBookSearchResponse.Document doc : res.getDocuments()) {
            if (doc == null) continue;

            String rawJson = toJsonQuietly(doc);

            IsbnParts isbnParts = IsbnParts.from(doc.getIsbn());
            LocalDateTime publishedAt = bookSearchConverter.parseDatetime(doc.getDatetime());

            // ✅ Upsert 기준:
            // 1) isbn13 있으면 isbn13
            // 2) 없으면 kakaoUrl
            Books book = findOrCreateBook(isbnParts.isbn13, doc.getUrl());

            book.updateBasicInfo(
                    safe(doc.getTitle()),
                    doc.getContents(),
                    doc.getThumbnail(),
                    doc.getUrl(),
                    doc.getPublisher(),
                    publishedAt,
                    doc.getIsbn(),
                    isbnParts.isbn10,
                    isbnParts.isbn13,
                    doc.getPrice(),
                    doc.getSalePrice(),
                    doc.getStatus(),
                    rawJson
            );

            // ✅ 저자/역자 매핑 교체
            List<BookAuthors> mappings = new ArrayList<>();

            // authors (빈값은 스킵: 전체 실패 방지)
            List<String> authorNames = doc.getAuthors() == null ? List.of() : doc.getAuthors();
            int order = 1;
            for (String name : authorNames) {
                Authors author = findOrCreateAuthorOrNull(name);
                if (author == null) continue;
                mappings.add(BookAuthors.builder()
                        .book(book)
                        .author(author)
                        .role(AuthorRole.AUTHOR)
                        .displayOrder(order++)
                        .build());
            }

            // translators (역자도 별도 sort_order)
            List<String> translators = doc.getTranslators() == null ? List.of() : doc.getTranslators();
            int tOrder = 1;
            for (String name : translators) {
                Authors author = findOrCreateAuthorOrNull(name);
                if (author == null) continue;
                mappings.add(BookAuthors.builder()
                        .book(book)
                        .author(author)
                        .role(AuthorRole.TRANSLATOR)
                        .displayOrder(tOrder++)
                        .build());
            }

            // ✅ 기존 매핑 삭제를 먼저 DB에 반영
            if (book.getId() != null) {
                book.getBookAuthors().clear();
                booksRepository.save(book);
                entityManager.flush();
            }

            // ✅ 새로운 매핑 추가
            book.replaceBookAuthors(mappings);

            Books saved = booksRepository.save(book);

            items.add(bookSearchConverter.toResponse(saved, doc));
        }

        int totalCount = (res.getMeta() == null) ? items.size() : res.getMeta().getTotalCount();
        boolean isEnd = (res.getMeta() != null) && res.getMeta().isEnd();

        return new BookSearchResponse(safePage, safeSize, isEnd, totalCount, items);
    }

    private Books findOrCreateBook(String isbn13, String kakaoUrl) {
        String i13 = normalize(isbn13);
        if (!i13.isBlank()) {
            return booksRepository.findByIsbn13(i13)
                    .orElseGet(() -> Books.builder().source(BookSource.KAKAO).build());
        }

        String url = normalize(kakaoUrl);
        if (!url.isBlank()) {
            return booksRepository.findByDetailUrl(url)
                    .orElseGet(() -> Books.builder().source(BookSource.KAKAO).build());
        }

        // ⚠️ 식별자가 둘 다 없으면 중복 삽입 가능성이 큼(운영 정책 필요)
        return Books.builder().source(BookSource.KAKAO).build();
    }

    /**
     * 빈 author가 들어오면 전체 import가 터지는 걸 막기 위해 "스킵" 처리
     */
    private Authors findOrCreateAuthorOrNull(String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) return null;

        return authorsRepository.findByName(normalized)
                .orElseGet(() -> authorsRepository.save(
                        Authors.builder().name(normalized).build()
                ));
    }

    private String safe(String s) {
        String n = normalize(s);
        return n.isBlank() ? "" : n;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private String toJsonQuietly(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        return Math.min(v, max);
    }

    private static class IsbnParts {
        final String isbn10;
        final String isbn13;

        private IsbnParts(String isbn10, String isbn13) {
            this.isbn10 = isbn10;
            this.isbn13 = isbn13;
        }

        static IsbnParts from(String raw) {
            if (raw == null || raw.isBlank()) return new IsbnParts(null, null);
            String[] parts = raw.trim().split("\\s+");
            String p1 = parts.length > 0 ? parts[0].trim() : null;
            String p2 = parts.length > 1 ? parts[1].trim() : null;

            String isbn10 = (p1 != null && p1.length() == 10) ? p1 : (p2 != null && p2.length() == 10 ? p2 : null);
            String isbn13 = (p1 != null && p1.length() == 13) ? p1 : (p2 != null && p2.length() == 13 ? p2 : null);

            return new IsbnParts(isbn10, isbn13);
        }
    }
}

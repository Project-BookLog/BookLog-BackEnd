package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.converter.BookSearchConverter;
import com.example.booklog.domain.library.books.dto.BookSearchItemResponse;
import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.*;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.books.service.client.KakaoBookClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookImportService {

    private final KakaoBookClient kakaoBookClient;
    private final BooksRepository booksRepository;
    private final AuthorsRepository authorsRepository;
    private final BookSearchConverter bookSearchConverter;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    /**
     * 카카오 도서 검색 -> books/authors/book_authors 업서트 -> 검색 응답 반환
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookSearchResponse searchAndUpsert(String query, int page, int size) {
        String q = normalize(query);
        if (q.isBlank()) {
            return new BookSearchResponse(page, size, true, 0, List.of());
        }

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
            LocalDate publishedAt = bookSearchConverter.parseDate(doc.getDatetime());

            // 1) Upsert 기준으로 book 찾기/생성
            Books book = findOrCreateBook(isbnParts.isbn13, doc.getUrl());

            // 2) book 정보 갱신
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
                    rawJson
            );

            // ✅ 3) 새 책이면 먼저 저장해서 book_id 확보 (가장 중요)
            if (book.getId() == null) {
                book = booksRepository.save(book);
                // flush는 필수는 아니지만, 이후 관계 insert 안정성을 높여줌
                entityManager.flush();
            }

            // ✅ 4) 기존 매핑 제거 (orphanRemoval=true면 delete 나감)
            // 기존 코드의 if(book.getId()!=null) 분기 필요 없음 (이미 id 있음)
            book.getBookAuthors().clear();
            entityManager.flush(); // 기존 row 삭제를 즉시 반영하고 싶으면

            // ✅ 5) 새로운 매핑 생성 (book은 반드시 id가 있음)
            List<BookAuthors> mappings = new ArrayList<>();

            // authors
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

            // translators
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

            // ✅ 6) 교체 + 저장
            book.replaceBookAuthors(mappings);

            Books saved = booksRepository.save(book);

            // 응답 구성
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

        return Books.builder().source(BookSource.KAKAO).build();
    }

    /**
     * 빈 author가 들어오면 전체 import가 터지는 걸 막기 위해 "스킵"
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

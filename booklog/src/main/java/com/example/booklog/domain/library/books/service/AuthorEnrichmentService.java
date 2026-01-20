package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.AuthorWikidataEnrichment;
import com.example.booklog.domain.library.books.dto.WikidataSearchResponse;
import com.example.booklog.domain.library.books.entity.Authors;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.service.client.WikidataClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorEnrichmentService {

    private final AuthorsRepository authorsRepository;
    private final WikidataClient wikidataClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void enrichAuthorByName(String authorName) {
        String normalized = authorName == null ? "" : authorName.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("authorName is blank");

        Authors author = authorsRepository.findByName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + normalized));

        if (author.hasWikidataId()) return;

        // 1) QID Search
        WikidataSearchResponse search = wikidataClient.searchEntity(author.getName()).block();
        Optional<String> qidOpt = pickBestQid(search);

        if (qidOpt.isEmpty()) return;
        String qid = qidOpt.get(); // e.g. "Q12345"

        // 2) Entity detail
        Object entityRes = wikidataClient.getEntity(qid).block();
        String rawJson = toJsonQuietly(entityRes);

        // 3) 최소 보강(처음엔 raw 저장 + summary만)
        String summary = null; // TODO: wikipedia 요약 붙이기

        AuthorWikidataEnrichment enrichment =
                AuthorWikidataEnrichment.minimalWithBio(qid, rawJson, summary);

        author.applyWikidataEnrichment(enrichment);

        authorsRepository.save(author);
    }

    private Optional<String> pickBestQid(WikidataSearchResponse res) {
        if (res == null || res.getSearch() == null || res.getSearch().isEmpty()) return Optional.empty();

        return res.getSearch().stream()
                .filter(item -> item.getId() != null && !item.getId().isBlank())
                .sorted((a, b) -> score(b) - score(a))
                .map(WikidataSearchResponse.SearchItem::getId)
                .findFirst();
    }

    private int score(WikidataSearchResponse.SearchItem item) {
        String d = item.getDescription() == null ? "" : item.getDescription();
        int s = 0;
        if (d.contains("작가")) s += 5;
        if (d.contains("소설가")) s += 5;
        if (d.contains("시인")) s += 5;
        if (d.contains("writer")) s += 3;
        if (d.contains("novelist")) s += 3;
        return s;
    }

    private String toJsonQuietly(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

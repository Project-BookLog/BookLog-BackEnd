package com.example.booklog.domain.library.books.service;

import com.example.booklog.domain.library.books.dto.AuthorDetailResponse;
import com.example.booklog.domain.library.books.dto.KakaoBookSearchResponse;
import com.example.booklog.domain.library.books.entity.AuthorAwards;
import com.example.booklog.domain.library.books.entity.Authors;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.AuthorRewardRepository;
import com.example.booklog.domain.library.books.repository.AuthorsRepository;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작가 조회 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorQueryServiceImpl implements AuthorQueryService {

    private final AuthorsRepository authorsRepository;
    private final BooksRepository booksRepository;
    private final AuthorRewardRepository authorRewardRepository;
    private final AuthorKakaoImportService authorKakaoImportService;
    private final AuthorGptEnrichmentService authorGptEnrichmentService;
    private final AuthorEnrichmentService authorEnrichmentService;
    private final BookImportService bookImportService;
    private final BookEnrichmentService bookEnrichmentService;
    private final BookTagAutoAssignService bookTagAutoAssignService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    /**
     * 작가 상세정보 조회
     *
     * @param authorId 작가 ID
     * @param sortBy 정렬 기준 (latest: 최신순, oldest: 오래된순, title: 제목순, author: 저자순)
     * @return 작가 상세정보
     */
    @Override
    @Transactional(readOnly = false) // 쓰기 작업이 있을 수 있음 (GPT 보완, 태그 할당 등)
    public AuthorDetailResponse getAuthorDetail(Long authorId, String sortBy) {
        log.info("작가 상세정보 조회 시작 - authorId: {}, sortBy: {}", authorId, sortBy);

        // 1. 작가 조회
        Authors author = authorsRepository.findById(authorId)
                .orElseThrow(() -> {
                    log.warn("작가를 찾을 수 없습니다 - authorId: {}", authorId);
                    return new EntityNotFoundException("작가를 찾을 수 없습니다. authorId: " + authorId);
                });

        log.info("작가 조회 완료 - profileJson: {}, profileImageUrl: {}",
                author.getProfileJson() != null ? "있음" : "null",
                author.getProfileImageUrl() != null ? "있음" : "null");

        // 2. DB에 데이터가 완전하지 않으면 보완
        boolean needsEnrichment = needsEnrichment(author);

        if (needsEnrichment) {
            log.info("작가 정보 보완 필요 - authorId: {}, name: {}", authorId, author.getName());
            enrichAuthorData(author);

            // 엔티티 최신화
            entityManager.flush();
            entityManager.clear(); // 캐시 클리어
            author = authorsRepository.findById(authorId).orElseThrow();

            log.info("엔티티 최신화 완료 - profileJson: {}",
                    author.getProfileJson() != null ? "있음" : "null");
        } else {
            log.info("작가 정보 보완 불필요 - authorId: {}", authorId);
        }

        // 3. 작가의 도서 목록 조회 (정렬 적용)
        List<Books> books = booksRepository.findBooksByAuthorId(authorId);
        books = applySorting(books, sortBy);

        // 4. 수상경력 조회
        List<AuthorAwards> awards = authorRewardRepository.findAllByAuthor_Id(authorId);

        // 5. 책 취향 정보 보완 (tasteAnalysis가 없는 책들)
        enrichBooksWithTasteInfo(books);

        // 6. 책 목록 다시 조회 (보완된 데이터 반영)
        books = booksRepository.findBooksByAuthorId(authorId);
        books = applySorting(books, sortBy);

        // 7. DTO 변환
        AuthorDetailResponse response = convertToResponse(author, books, awards);

        log.info("작가 상세정보 조회 완료 - authorId: {}, name: {}, 도서 수: {}",
                authorId, author.getName(), books.size());

        return response;
    }

    /**
     * 책 취향 정보 보완 (tasteAnalysis가 없는 책들)
     */
    private void enrichBooksWithTasteInfo(List<Books> books) {
        for (Books book : books) {
            if (book.getTasteAnalysis() == null || book.getTasteAnalysis().isEmpty()) {
                try {
                    log.info("책 취향 정보 생성 시작 - bookId: {}, title: {}", book.getId(), book.getTitle());
                    bookEnrichmentService.enrichBookInfo(book.getId());
                    log.info("책 취향 정보 생성 완료 - bookId: {}", book.getId());
                } catch (Exception e) {
                    log.warn("책 취향 정보 생성 실패 (계속 진행) - bookId: {}, error: {}",
                            book.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 작가 정보 보완 필요 여부 확인
     */
    private boolean needsEnrichment(Authors author) {
        // biography, profileImageUrl, profileJson이 없거나, 도서가 없거나, 수상경력이 없으면 보완 필요
        boolean hasNoBiography = author.getBiography() == null || author.getBiography().isEmpty();
        boolean hasNoProfileImage = author.getProfileImageUrl() == null || author.getProfileImageUrl().isEmpty();
        boolean hasNoProfileJson = author.getProfileJson() == null || author.getProfileJson().isEmpty();

        List<Books> books = booksRepository.findBooksByAuthorId(author.getId());
        boolean hasNoBooks = books.isEmpty();

        List<AuthorAwards> awards = authorRewardRepository.findAllByAuthor_Id(author.getId());
        boolean hasNoAwards = awards.isEmpty();

        log.info("작가 정보 상태 체크 - authorId: {}, hasNoBiography: {}, hasNoProfileImage: {}, hasNoProfileJson: {}, hasNoBooks: {}, hasNoAwards: {}",
                author.getId(), hasNoBiography, hasNoProfileImage, hasNoProfileJson, hasNoBooks, hasNoAwards);

        return hasNoBiography || hasNoProfileImage || hasNoProfileJson || hasNoBooks || hasNoAwards;
    }

    /**
     * 작가 정보 보완
     * 1. 카카오 API로 도서 정보 임포트
     * 2. 위키데이터로 프로필 이미지 보완
     * 3. 카카오 API가 비어있거나 부족하면 GPT로 보완
     */
    private void enrichAuthorData(Authors author) {
        try {
            // 1. 위키데이터로 프로필 이미지 보완 (wikidataId가 없거나 프로필 이미지가 없으면)
            boolean needsWikidataEnrichment = !author.hasWikidataId() ||
                    author.getProfileImageUrl() == null ||
                    author.getProfileImageUrl().isEmpty();

            if (needsWikidataEnrichment) {
                log.info("위키데이터를 통한 작가 정보 보완 시작 - author: {}", author.getName());
                try {
                    authorEnrichmentService.enrichAuthorByName(author.getName());
                } catch (Exception e) {
                    log.warn("위키데이터 보완 실패 - author: {}, error: {}", author.getName(), e.getMessage());
                }
            }

            // 2. 카카오 API로 도서 정보 임포트
            log.info("카카오 API를 통한 작가 도서 임포트 시작 - author: {}", author.getName());
            bookImportService.searchAndUpsert(author.getName(), 1, 10);

            // 3. 카카오 API 결과 확인
            KakaoBookSearchResponse kakaoResponse = authorKakaoImportService.searchAuthorBooks(author.getName());

            // 4. 작가 정보가 부족하면 GPT로 보완
            boolean needsGptEnrichment =
                    (author.getBiography() == null || author.getBiography().isEmpty()) ||
                    (author.getProfileJson() == null || author.getProfileJson().isEmpty());

            log.info("GPT 보완 필요 여부 체크 - biography: {}, profileJson: {}, needsGptEnrichment: {}",
                    author.getBiography() != null ? "있음" : "없음",
                    author.getProfileJson() != null ? "있음" : "없음",
                    needsGptEnrichment);

            if (needsGptEnrichment || authorKakaoImportService.isEmpty(kakaoResponse)) {
                log.info("GPT를 통한 작가 정보 보완 시작 - author: {}", author.getName());
                enrichWithGpt(author);
            } else {
                log.info("GPT 보완 불필요 - author: {}", author.getName());
            }

        } catch (Exception e) {
            log.error("작가 정보 보완 실패 - authorId: {}, error: {}", author.getId(), e.getMessage(), e);
            // 실패해도 기본 정보는 반환
        }
    }


    /**
     * GPT로 작가 정보 보완
     */
    private void enrichWithGpt(Authors author) {
        try {
            log.info("=== GPT 작가 정보 보완 시작 - author: {} ===", author.getName());

            AuthorGptEnrichmentService.AuthorEnrichmentResult gptResult =
                    authorGptEnrichmentService.enrichAuthorInfo(author.getName());

            log.info("GPT 응답 받음 - biography: {}, profile: {}, awards: {}",
                    gptResult.biography() != null ? "있음" : "없음",
                    gptResult.profile() != null ? "있음" : "없음",
                    gptResult.awards() != null ? gptResult.awards().size() + "개" : "없음");

            // biography 업데이트
            if (gptResult.biography() != null && !gptResult.biography().isEmpty()) {
                if (author.getBiography() == null || author.getBiography().isEmpty()) {
                    author.updateProfile(author.getProfileImageUrl(), gptResult.biography());
                    log.info("biography 업데이트 완료");
                }
            }

            // 프로필 정보 업데이트 (JSON으로 저장)
            if (gptResult.profile() != null) {
                try {
                    String profileJson = objectMapper.writeValueAsString(gptResult.profile());
                    author.updateProfileJson(profileJson);
                    log.info("프로필 정보 JSON 저장 완료 - profileJson: {}", profileJson);
                } catch (Exception e) {
                    log.error("프로필 정보 JSON 변환 실패", e);
                }
            }

            // 수상경력 업데이트
            if (gptResult.awards() != null && !gptResult.awards().isEmpty()) {
                List<AuthorAwards> existingAwards = authorRewardRepository.findAllByAuthor_Id(author.getId());

                if (existingAwards.isEmpty()) {
                    List<AuthorAwards> newAwards = gptResult.awards().stream()
                            .map(awardInfo -> AuthorAwards.builder()
                                    .author(author)
                                    .year(awardInfo.year())
                                    .awardName(awardInfo.awardName())
                                    .workTitle(awardInfo.workTitle())
                                    .build())
                            .collect(Collectors.toList());

                    authorRewardRepository.saveAll(newAwards);
                    log.info("수상경력 저장 완료 - 개수: {}", newAwards.size());
                }
            }

            authorsRepository.save(author);
            log.info("=== GPT 작가 정보 보완 완료 - author: {} ===", author.getName());

        } catch (Exception e) {
            log.error("GPT 작가 정보 보완 실패 - author: {}, error: {}", author.getName(), e.getMessage(), e);
        }
    }

    /**
     * 도서 정렬 적용
     */
    private List<Books> applySorting(List<Books> books, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "latest"; // 기본값: 최신순
        }

        return switch (sortBy.toLowerCase()) {
            case "oldest" -> books.stream()
                    .sorted(Comparator.comparing(Books::getPublishedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Books::getId))
                    .collect(Collectors.toList());

            case "title" -> books.stream()
                    .sorted(Comparator.comparing(Books::getTitle, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            case "author" -> books.stream()
                    .sorted(Comparator.comparing(book -> {
                        if (book.getBookAuthors().isEmpty()) return "";
                        return book.getBookAuthors().get(0).getAuthor().getName();
                    }, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            default -> // "latest" 또는 기타
                    books.stream()
                            .sorted(Comparator.comparing(Books::getPublishedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(Comparator.comparing(Books::getId).reversed()))
                            .collect(Collectors.toList());
        };
    }

    /**
     * DTO 변환
     */
    private AuthorDetailResponse convertToResponse(Authors author, List<Books> books, List<AuthorAwards> awards) {
        log.info("=== DTO 변환 시작 - authorId: {}, profileImageUrl: {}, biography: {}, profileJson: {} ===",
                author.getId(),
                author.getProfileImageUrl() != null ? "있음" : "null",
                author.getBiography() != null ? "있음" : "null",
                author.getProfileJson() != null ? "있음" : "null");

        // 도서 요약 목록
        List<AuthorDetailResponse.AuthorBookSummary> bookSummaries = books.stream()
                .map(book -> {
                    String authorName = book.getBookAuthors().isEmpty() ? "" :
                            book.getBookAuthors().get(0).getAuthor().getName();

                    AuthorDetailResponse.BookTasteInfo tasteInfo = extractTasteInfo(book);

                    return new AuthorDetailResponse.AuthorBookSummary(
                            book.getId(),
                            book.getTitle(),
                            authorName,
                            book.getPublisherName(),
                            book.getThumbnailUrl(),
                            tasteInfo
                    );
                })
                .collect(Collectors.toList());

        // 프로필 정보 (DB의 profileJson에서 파싱)
        AuthorDetailResponse.AuthorProfile profile = parseProfileJson(author.getProfileJson());

        // 수상경력 (연도별 정렬)
        List<AuthorDetailResponse.AuthorAward> awardList = awards.stream()
                .sorted(Comparator.comparing(AuthorAwards::getYear, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(award -> new AuthorDetailResponse.AuthorAward(
                        award.getYear(),
                        award.getAwardName(),
                        award.getWorkTitle()
                ))
                .collect(Collectors.toList());

        log.info("DTO 변환 완료 - profile education: {}, debut: {}, birthDate: {}, occupations: {}",
                profile.education().size(), profile.debut(), profile.birthDate(), profile.occupations().size());

        return new AuthorDetailResponse(
                author.getId(),
                author.getName(),
                author.getProfileImageUrl(),
                author.getBiography(),
                bookSummaries,
                profile,
                awardList
        );
    }

    /**
     * 프로필 JSON 파싱
     */
    private AuthorDetailResponse.AuthorProfile parseProfileJson(String profileJson) {
        try {
            if (profileJson == null || profileJson.isEmpty()) {
                log.warn("profileJson이 null 또는 비어있음");
                return new AuthorDetailResponse.AuthorProfile(List.of(), null, null, List.of());
            }

            log.info("프로필 JSON 파싱 시작 - profileJson: {}", profileJson);

            JsonNode root = objectMapper.readTree(profileJson);

            List<String> education = parseJsonArray(root.path("education"));
            String debut = root.path("debut").asText(null);
            String birthDate = root.path("birthDate").asText(null);
            List<String> occupations = parseJsonArray(root.path("occupations"));

            log.info("프로필 JSON 파싱 완료 - education: {}, debut: {}, birthDate: {}, occupations: {}",
                    education.size(), debut, birthDate, occupations.size());

            return new AuthorDetailResponse.AuthorProfile(education, debut, birthDate, occupations);

        } catch (Exception e) {
            log.warn("프로필 JSON 파싱 실패 - profileJson: {}, error: {}", profileJson, e.getMessage());
            return new AuthorDetailResponse.AuthorProfile(List.of(), null, null, List.of());
        }
    }

    /**
     * JSON 배열 파싱
     */
    private List<String> parseJsonArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String value = item.asText(null);
                if (value != null && !value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    /**
     * 책의 취향 정보 추출 (tasteAnalysis JSON 파싱)
     * 주의: enrichBooksWithTasteInfo()에서 이미 보완되었으므로 여기서는 파싱만 수행
     */
    private AuthorDetailResponse.BookTasteInfo extractTasteInfo(Books book) {
        try {
            if (book.getTasteAnalysis() == null || book.getTasteAnalysis().isEmpty()) {
                log.debug("책 취향 정보 없음 - bookId: {}", book.getId());
                return new AuthorDetailResponse.BookTasteInfo(null, null, null);
            }

            return parseTasteAnalysis(book.getTasteAnalysis(), book.getId());

        } catch (Exception e) {
            log.warn("책 취향 정보 파싱 실패 - bookId: {}, error: {}", book.getId(), e.getMessage());
            return new AuthorDetailResponse.BookTasteInfo(null, null, null);
        }
    }

    /**
     * tasteAnalysis JSON 파싱
     */
    private AuthorDetailResponse.BookTasteInfo parseTasteAnalysis(String tasteAnalysisJson, Long bookId) {
        try {
            JsonNode root = objectMapper.readTree(tasteAnalysisJson);

            String mood = root.path("mood").path("title").asText(null);
            String style = root.path("style").path("title").asText(null);
            String immersion = root.path("immersion").path("title").asText(null);

            log.debug("책 취향 정보 파싱 완료 - bookId: {}, mood: {}, style: {}, immersion: {}",
                    bookId, mood, style, immersion);

            return new AuthorDetailResponse.BookTasteInfo(mood, style, immersion);

        } catch (Exception e) {
            log.warn("tasteAnalysis JSON 파싱 실패 - bookId: {}, json: {}", bookId, tasteAnalysisJson);
            return new AuthorDetailResponse.BookTasteInfo(null, null, null);
        }
    }
}

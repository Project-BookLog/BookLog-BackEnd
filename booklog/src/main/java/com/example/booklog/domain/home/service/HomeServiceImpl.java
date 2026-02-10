package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.*;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.entity.TagCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final BookMetadataService bookMetadataService;
    private final BooksRepository booksRepository; // ✅ 추가

    /* =========================
     * Seed DTO (bookId + title)
     * ========================= */
    private record Seed(Long bookId, String title) {}

    /* =========================
     * PM 하드코딩 데이터셋 (DB bookId 기반)
     * ========================= */

    private static final List<Seed> REALTIME_TOP3 = List.of(
            new Seed(41L, "트렌드 코리아 2026"),
            new Seed(488L, "비가 오면 열리는 상점"),
            new Seed(480L, "이중 하나는 거짓말")
    );

    /**
     * TagCategory -> (tagName -> seeds(9개))
     * - seed 순서가 곧 랭킹(1~9)
     */
    private static final Map<TagCategory, Map<String, List<Seed>>> TAG_RANKINGS = Map.of(
            TagCategory.MOOD, Map.of(
                    "따뜻한", List.of(
                            new Seed(null, "불편한 편의점"),
                            new Seed(null, "메리골드 마음 세탁소"),
                            new Seed(null, "어서 오세요, 휴남동 서점입니다"),
                            new Seed(null, "나의 서툰 위로가 너에게 닿기를"),
                            new Seed(null, "세상의 마지막 우체국"),
                            new Seed(null, "밝은 밤"),
                            new Seed(null, "보노보노처럼 살다니 다행이야"),
                            new Seed(null, "곰돌이 푸, 행복한 일은 매일 있어"),
                            // ✅ 교정
                            new Seed(null, "쇼펜하우어 아포리즘: 당신의 인생이 왜 힘들지 않아야 한다고 생각하십니까")
                    ),
                    "잔잔한", List.of(
                            new Seed(null, "모순"),
                            new Seed(null, "마흔에 읽는 쇼펜하우어"),
                            new Seed(null, "기분이 태도가 되지 않게"),
                            new Seed(null, "보통의 존재"),
                            new Seed(null, "언어의 온도"),
                            // ✅ 교정
                            new Seed(null, "모든 삶은 흔적을 남긴다"),
                            new Seed(null, "당신도 느리게 나이 들 수 있습니다"),
                            new Seed(null, "혼자 있는 시간의 힘"),
                            new Seed(null, "무례한 사람에게 웃으며 대처하는 법")
                    ),
                    "유쾌한", List.of(
                            new Seed(null, "1cm 다이빙"),
                            new Seed(null, "하마터면 열심히 살 뻔했다"),
                            new Seed(null, "보건교사 안은영"),
                            new Seed(null, "일의 기쁨과 슬픔"),
                            new Seed(null, "지구에서 한아뿐"),
                            new Seed(null, "죽고 싶지만 떡볶이는 먹고 싶어"),
                            new Seed(null, "세이노의 가르침"),
                            new Seed(null, "돈의 속성"),
                            new Seed(null, "역행자")
                    ),
                    "어두운", List.of(
                            new Seed(null, "채식주의자"),
                            new Seed(null, "소년이 온다"),
                            new Seed(null, "인간 실격"),
                            new Seed(null, "7년의 밤"),
                            new Seed(null, "28"),
                            new Seed(null, "눈먼 자들의 도시"),
                            new Seed(null, "구의 증명"),
                            new Seed(null, "지극히 사적인 초능력"),
                            new Seed(null, "소문의 벽")
                    ),
                    "서늘한", List.of(
                            new Seed(null, "이중 하나는 거짓말"),
                            new Seed(null, "종의 기원"),
                            new Seed(null, "완전한 행복"),
                            new Seed(null, "당신이 누군가를 죽였다"),
                            new Seed(null, "방주"),
                            new Seed(null, "하우스메이드"),
                            new Seed(null, "그리고 아무도 없었다"),
                            new Seed(null, "진이, 지니"),
                            new Seed(null, "타인의 해석")
                    ),
                    "몽환적인", List.of(
                            new Seed(null, "비가 오면 열리는 상점"),
                            new Seed(null, "달러구트 꿈 백화점"),
                            new Seed(null, "미드나잇 라이브러리"),
                            new Seed(null, "연금술사"),
                            // ✅ 교정(띄어쓰기)
                            new Seed(null, "어린 왕자"),
                            new Seed(null, "작별인사"),
                            new Seed(null, "거울 속 외딴 성"),
                            new Seed(null, "물고기는 존재하지 않는다"),
                            new Seed(null, "정오에서 가장 먼 시간")
                    )
            ),

            TagCategory.STYLE, Map.of(
                    "간결한", List.of(
                            new Seed(null, "트렌드 코리아 2026"),
                            new Seed(null, "시대예보: 핵개인의 시대"),
                            new Seed(null, "마흔에 읽는 쇼펜하우어"),
                            new Seed(null, "돈의 속성"),
                            new Seed(null, "초격차"),
                            new Seed(null, "킵고잉"),
                            new Seed(null, "타이탄의 도구들"),
                            new Seed(null, "원씽"),
                            new Seed(null, "아주 작은 습관의 힘")
                    ),
                    "화려한", List.of(
                            new Seed(null, "달러구트 꿈 백화점"),
                            new Seed(null, "물고기는 존재하지 않는다"),
                            new Seed(null, "위대한 개츠비"),
                            new Seed(null, "연금술사"),
                            new Seed(null, "향수"),
                            new Seed(null, "파친코 1"),
                            new Seed(null, "미드나잇 라이브러리"),
                            new Seed(null, "모모"),
                            new Seed(null, "오만과 편견")
                    ),
                    "담백한", List.of(
                            new Seed(null, "모순"),
                            new Seed(null, "어서 오세요, 휴남동 서점입니다"),
                            new Seed(null, "보통의 존재"),
                            new Seed(null, "언어의 온도"),
                            new Seed(null, "불편한 편의점"),
                            new Seed(null, "1cm 다이빙"),
                            new Seed(null, "하마터면 열심히 살 뻔했다"),
                            new Seed(null, "퇴사는 여행"),
                            new Seed(null, "태도의 말들")
                    ),
                    "섬세한", List.of(
                            new Seed(null, "비가 오면 열리는 상점"),
                            new Seed(null, "이중 하나는 거짓말"),
                            new Seed(null, "메리골드 마음 세탁소"),
                            new Seed(null, "소년이 온다"),
                            new Seed(null, "밝은 밤"),
                            new Seed(null, "작별인사"),
                            new Seed(null, "데미안"),
                            new Seed(null, "각각의 계절"),
                            new Seed(null, "정오에서 가장 먼 시간")
                    ),
                    "직설적", List.of(
                            new Seed(null, "세이노의 가르침"),
                            new Seed(null, "역행자"),
                            new Seed(null, "돈의 속성"),
                            new Seed(null, "부의 추월차선"),
                            new Seed(null, "타이탄의 도구들"),
                            new Seed(null, "킵고잉"),
                            new Seed(null, "넛지"),
                            new Seed(null, "스틱!"),
                            new Seed(null, "그릿")
                    ),
                    "은유적", List.of(
                            new Seed(null, "채식주의자"),
                            new Seed(null, "소년이 온다"),
                            // ✅ 교정(띄어쓰기)
                            new Seed(null, "어린 왕자"),
                            new Seed(null, "연금술사"),
                            new Seed(null, "데미안"),
                            new Seed(null, "작별인사"),
                            new Seed(null, "구의 증명"),
                            new Seed(null, "이토록 평범한 미래"),
                            new Seed(null, "파친코 1")
                    )
            ),

            TagCategory.IMMERSION, Map.of(
                    "기분 전환", List.of(
                            new Seed(null, "트렌드 코리아 2026"),
                            new Seed(null, "돈의 속성"),
                            new Seed(null, "나의 서툰 위로가 너에게 닿기를"),
                            new Seed(null, "1cm 다이빙"),
                            new Seed(null, "기분이 태도가 되지 않게"),
                            // ✅ 교정
                            new Seed(null, "모든 삶은 흔적을 남긴다"),
                            new Seed(null, "킵고잉"),
                            new Seed(null, "타이탄의 도구들"),
                            new Seed(null, "부의 추월차선")
                    ),
                    "지적인 탐구", List.of(
                            new Seed(null, "모순"),
                            new Seed(null, "시대예보: 핵개인의 시대"),
                            new Seed(null, "마흔에 읽는 쇼펜하우어"),
                            new Seed(null, "채식주의자"),
                            new Seed(null, "소년이 온다"),
                            new Seed(null, "초격차"),
                            new Seed(null, "사피엔스"),
                            new Seed(null, "정의란 무엇인가"),
                            new Seed(null, "총 균 쇠")
                    ),
                    "압도적 몰입", List.of(
                            new Seed(null, "비가 오면 열리는 상점"),
                            new Seed(null, "메리골드 마음 세탁소"),
                            new Seed(null, "불편한 편의점"),
                            new Seed(null, "달러구트 꿈 백화점"),
                            new Seed(null, "파친코 1"),
                            new Seed(null, "향수"),
                            new Seed(null, "위대한 개츠비"),
                            new Seed(null, "미드나잇 라이브러리"),
                            new Seed(null, "모모")
                    ),
                    "짙은 여운", List.of(
                            new Seed(null, "이중 하나는 거짓말"),
                            new Seed(null, "작별인사"),
                            new Seed(null, "물고기는 존재하지 않는다"),
                            new Seed(null, "데미안"),
                            // ✅ 교정(띄어쓰기)
                            new Seed(null, "어린 왕자"),
                            new Seed(null, "연금술사"),
                            new Seed(null, "소년이 온다"),
                            new Seed(null, "나는 왜 작은 실수에도 이렇게 힘들까"),
                            new Seed(null, "세상의 마지막 우체국")
                    )
            )
    );

    /* =========================
     * Main
     * ========================= */

    @Override
    public HomeResponse getHomeData() {

        // ✅ 0) null bookId seed들을 제목 기반으로 DB에서 찾아서 매핑(괄호 제거 포함)
        Map<String, Long> normTitleToId = buildNormTitleToBookIdMapForNullSeeds();

        // 1) 홈에서 필요한 모든 seed 수집 (bookId가 있는 것만)
        List<BookMetadataService.BookInfo> infos = collectAllBookInfos(normTitleToId);

        // 2) 메타 붙이기 (bookId 기반)
        List<BookSummary> allBooks = bookMetadataService.getBookSummaries(infos);

        // 3) bookId -> summary
        Map<Long, BookSummary> bookMap = allBooks.stream()
                .filter(b -> b.bookId() != null)
                .collect(Collectors.toMap(BookSummary::bookId, b -> b, (a, b) -> a));

        return new HomeResponse(
                buildRealTimeRanking(bookMap),
                buildBestsellersByCategory(bookMap, TagCategory.MOOD, normTitleToId),
                buildBestsellersByCategory(bookMap, TagCategory.STYLE, normTitleToId),
                buildBestsellersByCategory(bookMap, TagCategory.IMMERSION, normTitleToId)
        );
    }

    /* =========================
     * Normalize / DB Resolve
     * ========================= */

    private String normalizeTitle(String t) {
        if (t == null) return null;
        // ( ... ) 제거 + 공백 제거
        return t.replaceAll("\\([^\\)]*\\)", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    /**
     * Seed의 bookId가 null인 애들만 모아서
     * 정규화 title IN 으로 DB에서 찾아 대표 bookId 매핑
     */
    private Map<String, Long> buildNormTitleToBookIdMapForNullSeeds() {
        Set<String> need = new HashSet<>();

        TAG_RANKINGS.values().forEach(tagMap ->
                tagMap.values().forEach(list ->
                        list.forEach(s -> {
                            if (s.bookId() == null && s.title() != null && !s.title().isBlank()) {
                                need.add(normalizeTitle(s.title()));
                            }
                        })
                )
        );

        if (need.isEmpty()) return Map.of();

        List<Books> found = booksRepository.findAllByNormalizedTitleIn(new ArrayList<>(need));

        // 정규화제목 충돌 시: publishedDate 최신 > id 큰 값 선택
        return found.stream()
                .collect(Collectors.groupingBy(
                        b -> normalizeTitle(b.getTitle()),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator
                                        .comparing(Books::getPublishedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(Books::getId)
                                ),
                                opt -> opt.map(Books::getId).orElse(null)
                        )
                ));
    }

    private Long resolveBookId(Seed s, Map<String, Long> normTitleToId) {
        if (s.bookId() != null) return s.bookId();
        if (s.title() == null) return null;
        return normTitleToId.get(normalizeTitle(s.title()));
    }

    /* =========================
     * BookInfo
     * ========================= */

    private List<BookMetadataService.BookInfo> collectAllBookInfos(Map<String, Long> normTitleToId) {
        LinkedHashMap<Long, BookMetadataService.BookInfo> dedup = new LinkedHashMap<>();

        // realtime top3 (rank 포함)
        for (int i = 0; i < REALTIME_TOP3.size(); i++) {
            Seed s = REALTIME_TOP3.get(i);
            if (s.bookId() == null) continue;
            dedup.put(s.bookId(), new BookMetadataService.BookInfo(s.bookId(), s.title(), i + 1));
        }

        // tag rankings (rank 없이 메타 조회용) - ✅ null이면 DB에서 찾아서 넣기
        TAG_RANKINGS.values().forEach(tagMap ->
                tagMap.values().forEach(list ->
                        list.forEach(s -> {
                            Long id = resolveBookId(s, normTitleToId);
                            if (id == null) return;
                            dedup.putIfAbsent(id, new BookMetadataService.BookInfo(id, s.title(), null));
                        })
                )
        );

        return new ArrayList<>(dedup.values());
    }

    /* =========================
     * Sections
     * ========================= */

    private RealTimeRankingSection buildRealTimeRanking(Map<Long, BookSummary> bookMap) {
        List<BookSummary> rankings = new ArrayList<>();

        for (int i = 0; i < REALTIME_TOP3.size(); i++) {
            Seed s = REALTIME_TOP3.get(i);
            int rank = i + 1;

            BookSummary b = bookMap.get(s.bookId());
            if (b != null) {
                rankings.add(new BookSummary(
                        b.bookId(), b.title(), b.author(), b.publisher(), b.coverImageUrl(), rank
                ));
            } else {
                rankings.add(createFallback(s.bookId(), s.title(), rank));
            }
        }

        return new RealTimeRankingSection("2030 인기 도서 TOP 3", rankings);
    }

    private List<TaggedBooksSection> buildBestsellersByCategory(
            Map<Long, BookSummary> bookMap,
            TagCategory category,
            Map<String, Long> normTitleToId
    ) {
        Map<String, List<Seed>> tagMap = TAG_RANKINGS.getOrDefault(category, Map.of());

        return tagMap.entrySet().stream()
                .map(e -> createTagSection(bookMap, normTitleToId, e.getKey(), e.getValue()))
                .toList();
    }

    private TaggedBooksSection createTagSection(
            Map<Long, BookSummary> bookMap,
            Map<String, Long> normTitleToId,
            String tagName,
            List<Seed> seeds
    ) {
        List<BookSummary> books = new ArrayList<>();

        for (int i = 0; i < seeds.size(); i++) {
            Seed s = seeds.get(i);
            int ranking = i + 1;

            Long id = resolveBookId(s, normTitleToId);

            if (id == null) {
                books.add(createFallback(0L, s.title(), ranking));
                continue;
            }

            BookSummary b = bookMap.get(id);
            if (b != null) {
                books.add(new BookSummary(
                        b.bookId(), b.title(), b.author(), b.publisher(), b.coverImageUrl(), ranking
                ));
            } else {
                books.add(createFallback(id, s.title(), ranking));
            }
        }

        while (books.size() < 9) {
            books.add(createFallback(0L, "미정", books.size() + 1));
        }
        if (books.size() > 9) {
            books = books.subList(0, 9);
        }

        return new TaggedBooksSection(tagName, books);
    }

    private BookSummary createFallback(Long bookId, String title, Integer ranking) {
        return new BookSummary(bookId, title, null, null, null, ranking);
    }
}

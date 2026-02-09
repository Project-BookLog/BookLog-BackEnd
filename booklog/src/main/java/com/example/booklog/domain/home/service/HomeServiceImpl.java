package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.*;
import com.example.booklog.domain.tags.entity.TagCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈 화면 데이터 제공 서비스 구현체 (PM 하드코딩 랭킹표 기반)
 *
 * 요구사항 반영:
 * 1) 베스트셀러(태그 섹션) BookSummary.ranking 필수 (null 금지)  -> 1~9 부여
 * 2) 베스트셀러는 태그당 9권 (ranking 1~9)
 * 3) 몰입도 태그명: "기분 전환", "지적인 탐구", "압도적 몰입", "짙은 여운"
 * 4) TagCategory enum 활용 (MOOD/STYLE/IMMERSION)
 *
 * 현재 제약:
 * - DB 태그-책 매핑이 없어 전부 하드코딩 목록으로 구성
 * - DB 메타데이터(저자/출판사/표지)는 있으면 조회, 없으면 null fallback
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final BookMetadataService bookMetadataService;

    /* =========================
     * PM 하드코딩 데이터셋
     * ========================= */

    /** 실시간 랭킹 TOP3 (PM 제공) */
    private static final List<String> REALTIME_TOP3 = List.of(
            "트렌드 코리아 2026",
            "비가 오면 열리는 상점",
            "이중 하나는 거짓말"
    );

    /**
     * 태그별 랭킹표 (각 태그당 9권)
     * - key: TagCategory
     * - value: (tagName -> 9권 제목 리스트)
     */
    private static final Map<TagCategory, Map<String, List<String>>> TAG_RANKINGS = Map.of(
            TagCategory.MOOD, Map.of(
                    "따뜻한", List.of(
                            "불편한 편의점",
                            "메리골드 마음 세탁소",
                            "어서 오세요, 휴남동 서점입니다",
                            "나의 서투른 위로가 너에게 닿기를",
                            "세상의 마지막 우체국",
                            "밝은 밤",
                            "보노보노처럼 살다니 다행이야",
                            "곰돌이 푸, 행복한 일은 매일 있어",
                            "당신의 인생이 왜 힘들지 않아야 한다고 생각하십니까"
                    ),
                    "잔잔한", List.of(
                            "모순",
                            "마흔에 읽는 쇼펜하우어",
                            "기분이 태도가 되지 않게",
                            "보통의 존재",
                            "언어의 온도",
                            "모든 삶은 기록을 남긴다",
                            "당신도 느리게 나이 들 수 있습니다",
                            "혼자 있는 시간의 힘",
                            "무례한 사람에게 웃으며 대처하는 법"
                    ),
                    "유쾌한", List.of(
                            "1cm 다이빙",
                            "하마터면 열심히 살 뻔했다",
                            "보건교사 안은영",
                            "일의 기쁨과 슬픔",
                            "지구에서 한아뿐",
                            "죽고 싶지만 떡볶이는 먹고 싶어",
                            "세이노의 가르침",
                            "돈의 속성",
                            "역행자"
                    ),
                    "어두운", List.of(
                            "채식주의자",
                            "소년이 온다",
                            "인간 실격",
                            "7년의 밤",
                            "28",
                            "눈먼 자들의 도시",
                            "구의 증명",
                            "지극히 사적인 초능력",
                            "소문의 벽"
                    ),
                    "서늘한", List.of(
                            "이중 하나는 거짓말",
                            "종의 기원",
                            "완전한 행복",
                            "당신이 누군가를 죽였다",
                            "방주",
                            "하우스메이드",
                            "그리고 아무도 없었다",
                            "진이, 지니",
                            "타인의 해석"
                    ),
                    "몽환적인", List.of(
                            "비가 오면 열리는 상점",
                            "달러구트 꿈 백화점",
                            "미드나잇 라이브러리",
                            "연금술사",
                            "어린왕자",
                            "작별인사",
                            "거울 속의 외딴 성",
                            "물고기는 존재하지 않는다",
                            "정오에서 가장 먼 시간"
                    )
            ),

            TagCategory.STYLE, Map.of(
                    "간결한", List.of(
                            "트렌드 코리아 2026",
                            "시대예보: 핵개인의 시대",
                            "마흔에 읽는 쇼펜하우어",
                            "돈의 속성",
                            "초격차",
                            "킵고잉",
                            "타이탄의 도구들",
                            "원씽",
                            "아토믹 해빗"
                    ),
                    "화려한", List.of(
                            "달러구트 꿈 백화점",
                            "물고기는 존재하지 않는다",
                            "위대한 개츠비",
                            "연금술사",
                            "향수",
                            "파친코",
                            "미드나잇 라이브러리",
                            "모모",
                            "오만과 편견"
                    ),
                    "담백한", List.of(
                            "모순",
                            "어서 오세요, 휴남동 서점입니다",
                            "보통의 존재",
                            "언어의 온도",
                            "불편한 편의점",
                            "1cm 다이빙",
                            "하마터면 열심히 살 뻔했다",
                            "퇴사는 여행",
                            "태도의 말들"
                    ),
                    "섬세한", List.of(
                            "비가 오면 열리는 상점",
                            "이중 하나는 거짓말",
                            "메리골드 마음 세탁소",
                            "소년이 온다",
                            "밝은 밤",
                            "작별인사",
                            "데미안",
                            "각각의 계절",
                            "정오에서 가장 먼 시간"
                    ),
                    "직설적", List.of(
                            "세이노의 가르침",
                            "역행자",
                            "돈의 속성",
                            "부의 추월차선",
                            "타이탄의 도구들",
                            "킵고잉",
                            "넛지",
                            "스틱!",
                            "그릿"
                    ),
                    "은유적", List.of(
                            "채식주의자",
                            "소년이 온다",
                            "어린왕자",
                            "연금술사",
                            "데미안",
                            "작별인사",
                            "구의 증명",
                            "이토록 평범한 미래",
                            "파친코"
                    )
            ),

            TagCategory.IMMERSION, Map.of(
                    "기분 전환", List.of(
                            "트렌드 코리아 2026",
                            "돈의 속성",
                            "나의 서투른 위로가 너에게 닿기를",
                            "1cm 다이빙",
                            "기분이 태도가 되지 않게",
                            "모든 삶은 기록을 남긴다",
                            "킵고잉",
                            "타이탄의 도구들",
                            "부의 추월차선"
                    ),
                    "지적인 탐구", List.of(
                            "모순",
                            "시대예보: 핵개인의 시대",
                            "마흔에 읽는 쇼펜하우어",
                            "채식주의자",
                            "소년이 온다",
                            "초격차",
                            "사피엔스",
                            "정의란 무엇인가",
                            "총 균 쇠"
                    ),
                    "압도적 몰입", List.of(
                            "비가 오면 열리는 상점",
                            "메리골드 마음 세탁소",
                            "불편한 편의점",
                            "달러구트 꿈 백화점",
                            "파친코",
                            "향수",
                            "위대한 개츠비",
                            "미드나잇 라이브러리",
                            "모모"
                    ),
                    "짙은 여운", List.of(
                            "이중 하나는 거짓말",
                            "작별인사",
                            "물고기는 존재하지 않는다",
                            "데미안",
                            "어린왕자",
                            "연금술사",
                            "소년이 온다",
                            "서늘한 여름밤",
                            "세상의 마지막 우체국"
                    )
            )
    );

    /**
     * 제목 -> 임시 bookId 매핑
     * - 실시간 + 태그랭킹에 등장하는 모든 책 제목을 자동 수집하여 1..N 부여
     */
    private static final Map<String, Long> BOOK_ID_MAPPING = initializeBookIdMapping();

    private static Map<String, Long> initializeBookIdMapping() {
        Set<String> titles = new LinkedHashSet<>();

        titles.addAll(REALTIME_TOP3);
        TAG_RANKINGS.values().forEach(tagMap ->
                tagMap.values().forEach(titles::addAll)
        );

        Map<String, Long> mapping = new LinkedHashMap<>();
        long id = 1L;
        for (String t : titles) {
            mapping.put(t, id++);
        }
        return Collections.unmodifiableMap(mapping);
    }

    /* =========================
     * Main
     * ========================= */

    @Override
    public HomeResponse getHomeData() {
        // 1) 홈에서 필요한 모든 책 제목 수집 -> 메타데이터 일괄 조회
        List<BookMetadataService.BookInfo> allInfos = collectAllBookInfos();
        List<BookSummary> allBooks = bookMetadataService.getBookSummaries(allInfos);

        // 2) title -> BookSummary 맵
        Map<String, BookSummary> bookMap = allBooks.stream()
                .collect(Collectors.toMap(BookSummary::title, b -> b, (a, b) -> a));

        return new HomeResponse(
                buildRealTimeRanking(bookMap),
                buildBestsellersByCategory(bookMap, TagCategory.MOOD),
                buildBestsellersByCategory(bookMap, TagCategory.STYLE),
                buildBestsellersByCategory(bookMap, TagCategory.IMMERSION)
        );
    }

    /* =========================
     * Collect all book infos
     * ========================= */

    private List<BookMetadataService.BookInfo> collectAllBookInfos() {
        List<BookMetadataService.BookInfo> result = new ArrayList<>();

        // 실시간 TOP3는 ranking 포함
        for (int i = 0; i < REALTIME_TOP3.size(); i++) {
            addBookInfo(result, REALTIME_TOP3.get(i), i + 1);
        }

        // 태그 랭킹에 등장하는 모든 책은 ranking 없이(메타 조회용)
        TAG_RANKINGS.values().forEach(tagMap ->
                tagMap.values().forEach(list ->
                        list.forEach(t -> addBookInfo(result, t, null))
                )
        );

        // 중복 제거: bookId 기준 유니크
        return result.stream()
                .collect(Collectors.toMap(
                        BookMetadataService.BookInfo::bookId,
                        b -> b,
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private void addBookInfo(List<BookMetadataService.BookInfo> list, String title, Integer ranking) {
        Long bookId = BOOK_ID_MAPPING.get(title);
        if (bookId != null) {
            list.add(new BookMetadataService.BookInfo(bookId, title, ranking));
        }
    }

    /* =========================
     * Sections
     * ========================= */

    private RealTimeRankingSection buildRealTimeRanking(Map<String, BookSummary> bookMap) {
        List<BookSummary> rankings = new ArrayList<>();

        for (int i = 0; i < REALTIME_TOP3.size(); i++) {
            String title = REALTIME_TOP3.get(i);
            int rank = i + 1;

            BookSummary b = bookMap.getOrDefault(
                    title,
                    createFallback(BOOK_ID_MAPPING.getOrDefault(title, 0L), title, rank)
            );

            rankings.add(new BookSummary(
                    b.bookId(),
                    b.title(),
                    b.author(),
                    b.publisher(),
                    b.coverImageUrl(),
                    rank
            ));
        }

        return new RealTimeRankingSection("2030 인기 도서 TOP 3", rankings);
    }

    private List<TaggedBooksSection> buildBestsellersByCategory(Map<String, BookSummary> bookMap, TagCategory category) {
        Map<String, List<String>> tagMap = TAG_RANKINGS.get(category);

        return tagMap.entrySet().stream()
                .map(e -> createTagSection(bookMap, e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * ✅ 태그 섹션: ranking 필수 (1~9)
     * ✅ 태그당 9권
     */
    private TaggedBooksSection createTagSection(
            Map<String, BookSummary> bookMap,
            String tagName,
            List<String> bookTitles
    ) {
        List<BookSummary> books = new ArrayList<>();

        for (int i = 0; i < bookTitles.size(); i++) {
            String title = bookTitles.get(i);
            int ranking = i + 1;

            BookSummary original = bookMap.get(title);
            if (original != null) {
                books.add(new BookSummary(
                        original.bookId(),
                        original.title(),
                        original.author(),
                        original.publisher(),
                        original.coverImageUrl(),
                        ranking
                ));
            } else {
                Long bookId = BOOK_ID_MAPPING.getOrDefault(title, 0L);
                books.add(createFallback(bookId, title, ranking));
            }
        }

        // 안전장치: 혹시 9개 미만/초과이면 정규화
        while (books.size() < 9) {
            int ranking = books.size() + 1;
            books.add(createFallback(0L, "미정", ranking));
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

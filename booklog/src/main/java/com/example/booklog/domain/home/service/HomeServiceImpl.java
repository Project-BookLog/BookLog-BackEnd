package com.example.booklog.domain.home.service;

import com.example.booklog.domain.home.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈 화면 데이터 제공 서비스 구현체
 *
 * PM 제공 데이터 기반으로 구성하되,
 * 실제 도서 메타데이터는 BookMetadataService를 통해 DB/카카오 API에서 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    private final BookMetadataService bookMetadataService;

    // PM 제공 데이터 기반 도서명 → bookId 매핑
    // 실제 운영 시에는 DB에서 title로 조회하여 bookId를 가져와야 함
    private static final Map<String, Long> BOOK_ID_MAPPING = initializeBookIdMapping();

    /**
     * 도서명 → bookId 매핑 초기화
     *
     * 실제 시스템에서는:
     * 1. DB Book 테이블에 title을 unique 제약조건으로 관리하거나
     * 2. 별도 매핑 테이블을 운영하거나
     * 3. 시드 데이터 적재 시 고정 ID를 할당하는 전략 필요
     */
    private static Map<String, Long> initializeBookIdMapping() {
        Map<String, Long> mapping = new HashMap<>();
        long id = 1L;

        // PM 제공 데이터의 모든 도서명을 순서대로 ID 할당
        String[] bookTitles = {
            "트렌드 코리아 2026",
            "비가 오면 열리는 상점",
            "이중 하나는 거짓말",
            "모순",
            "메리골드 마음 세탁소",
            "시대예보: 핵개인의 시대",
            "마흔에 읽는 쇼펜하우어",
            "불편한 편의점",
            "돈의 속성 (300쇄 리미티드)",
            "채식주의자",
            "나의 서투른 위로가 너에게 닿기를",
            "달러구트 꿈 백화점",
            "모든 삶은 기록을 남긴다",
            "데미안",
            "기분이 태도가 되지 않게",
            "작별인사",
            "당신도 느리게 재생할 수 있습니다",
            "1cm 다이빙",
            "초격차",
            "물고기는 존재하지 않는다"
        };

        for (String title : bookTitles) {
            mapping.put(title, id++);
        }

        return Collections.unmodifiableMap(mapping);
    }

    @Override
    @Cacheable(value = "homeBooks", key = "'home:all'")
    public HomeResponse getHomeData() {
        log.info("홈 화면 데이터 조회 시작");

        // 1. 모든 도서 정보 수집
        List<BookMetadataService.BookInfo> allBookInfos = collectAllBookInfos();

        // 2. 일괄 조회 (DB 쿼리 최적화)
        List<BookSummary> allBooks = bookMetadataService.getBookSummaries(allBookInfos);

        // 3. title을 key로 하는 Map 생성
        Map<String, BookSummary> bookMap = allBooks.stream()
                .collect(Collectors.toMap(BookSummary::title, b -> b, (a, b) -> a));

        return new HomeResponse(
                buildRealTimeRanking(bookMap),
                buildMoodBestsellers(bookMap),
                buildWritingStyleBestsellers(bookMap),
                buildImmersionBestsellers(bookMap)
        );
    }

    /**
     * 모든 섹션의 도서 정보 수집
     */
    private List<BookMetadataService.BookInfo> collectAllBookInfos() {
        List<BookMetadataService.BookInfo> result = new ArrayList<>();

        // 실시간 랭킹 (1-20위)
        addBookInfo(result, "트렌드 코리아 2026", 1);
        addBookInfo(result, "비가 오면 열리는 상점", 2);
        addBookInfo(result, "이중 하나는 거짓말", 3);
        addBookInfo(result, "모순", 4);
        addBookInfo(result, "메리골드 마음 세탁소", 5);
        addBookInfo(result, "시대예보: 핵개인의 시대", 6);
        addBookInfo(result, "마흔에 읽는 쇼펜하우어", 7);
        addBookInfo(result, "불편한 편의점", 8);
        addBookInfo(result, "돈의 속성 (300쇄 리미티드)", 9);
        addBookInfo(result, "채식주의자", 10);
        addBookInfo(result, "나의 서투른 위로가 너에게 닿기를", 11);
        addBookInfo(result, "달러구트 꿈 백화점", 12);
        addBookInfo(result, "모든 삶은 기록을 남긴다", 13);
        addBookInfo(result, "데미안", 14);
        addBookInfo(result, "기분이 태도가 되지 않게", 15);
        addBookInfo(result, "작별인사", 16);
        addBookInfo(result, "당신도 느리게 재생할 수 있습니다", 17);
        addBookInfo(result, "1cm 다이빙", 18);
        addBookInfo(result, "초격차", 19);
        addBookInfo(result, "물고기는 존재하지 않는다", 20);

        return result;
    }

    private void addBookInfo(List<BookMetadataService.BookInfo> list, String title, Integer ranking) {
        Long bookId = BOOK_ID_MAPPING.get(title);
        if (bookId != null) {
            list.add(new BookMetadataService.BookInfo(bookId, title, ranking));
        }
    }

    /**
     * 실시간 랭킹 섹션 구성
     * PM 데이터: 2030 인기 도서 TOP 20
     */
    private RealTimeRankingSection buildRealTimeRanking(Map<String, BookSummary> bookMap) {
        List<BookSummary> rankings = List.of(
            bookMap.getOrDefault("트렌드 코리아 2026", createFallback(1L, "트렌드 코리아 2026", 1)),
            bookMap.getOrDefault("비가 오면 열리는 상점", createFallback(2L, "비가 오면 열리는 상점", 2)),
            bookMap.getOrDefault("이중 하나는 거짓말", createFallback(3L, "이중 하나는 거짓말", 3)),
            bookMap.getOrDefault("모순", createFallback(4L, "모순", 4)),
            bookMap.getOrDefault("메리골드 마음 세탁소", createFallback(5L, "메리골드 마음 세탁소", 5)),
            bookMap.getOrDefault("시대예보: 핵개인의 시대", createFallback(6L, "시대예보: 핵개인의 시대", 6)),
            bookMap.getOrDefault("마흔에 읽는 쇼펜하우어", createFallback(7L, "마흔에 읽는 쇼펜하우어", 7)),
            bookMap.getOrDefault("불편한 편의점", createFallback(8L, "불편한 편의점", 8)),
            bookMap.getOrDefault("돈의 속성 (300쇄 리미티드)", createFallback(9L, "돈의 속성 (300쇄 리미티드)", 9)),
            bookMap.getOrDefault("채식주의자", createFallback(10L, "채식주의자", 10)),
            bookMap.getOrDefault("나의 서투른 위로가 너에게 닿기를", createFallback(11L, "나의 서투른 위로가 너에게 닿기를", 11)),
            bookMap.getOrDefault("달러구트 꿈 백화점", createFallback(12L, "달러구트 꿈 백화점", 12)),
            bookMap.getOrDefault("모든 삶은 기록을 남긴다", createFallback(13L, "모든 삶은 기록을 남긴다", 13)),
            bookMap.getOrDefault("데미안", createFallback(14L, "데미안", 14)),
            bookMap.getOrDefault("기분이 태도가 되지 않게", createFallback(15L, "기분이 태도가 되지 않게", 15)),
            bookMap.getOrDefault("작별인사", createFallback(16L, "작별인사", 16)),
            bookMap.getOrDefault("당신도 느리게 재생할 수 있습니다", createFallback(17L, "당신도 느리게 재생할 수 있습니다", 17)),
            bookMap.getOrDefault("1cm 다이빙", createFallback(18L, "1cm 다이빙", 18)),
            bookMap.getOrDefault("초격차", createFallback(19L, "초격차", 19)),
            bookMap.getOrDefault("물고기는 존재하지 않는다", createFallback(20L, "물고기는 존재하지 않는다", 20))
        );

        return new RealTimeRankingSection(
                "2030 인기 도서 TOP 20",
                rankings
        );
    }

    /**
     * 분위기별 베스트셀러 섹션 구성
     * PM 데이터: 분위기 세부 태그별 전체 도서 (TOP 3가 아님)
     */
    private List<TaggedBooksSection> buildMoodBestsellers(Map<String, BookSummary> bookMap) {
        return List.of(
            createTagSection(bookMap, "따뜻한",
                "비가 오면 열리는 상점", "메리골드 마음 세탁소", "불편한 편의점"),
            createTagSection(bookMap, "잔잔한",
                "이중 하나는 거짓말", "모순", "마흔에 읽는 쇼펜하우어"),
            createTagSection(bookMap, "유쾌한",
                "트렌드 코리아 2026", "시대예보: 핵개인의 시대", "불편한 편의점"),
            createTagSection(bookMap, "어두운",
                "마흔에 읽는 쇼펜하우어", "채식주의자", "데미안"),
            createTagSection(bookMap, "서늘한",
                "트렌드 코리아 2026", "이중 하나는 거짓말", "모순"),
            createTagSection(bookMap, "몽환적인",
                "비가 오면 열리는 상점", "메리골드 마음 세탁소", "달러구트 꿈 백화점")
        );
    }

    /**
     * 문체별 베스트셀러 섹션 구성
     * PM 데이터: 문체 세부 태그별 전체 도서 (TOP 3가 아님)
     */
    private List<TaggedBooksSection> buildWritingStyleBestsellers(Map<String, BookSummary> bookMap) {
        return List.of(
            createTagSection(bookMap, "간결한",
                "트렌드 코리아 2026", "시대예보: 핵개인의 시대", "마흔에 읽는 쇼펜하우어"),
            createTagSection(bookMap, "화려한",
                "달러구트 꿈 백화점", "물고기는 존재하지 않는다"), // 3위 없음
            createTagSection(bookMap, "담백한",
                "모순", "메리골드 마음 세탁소", "불편한 편의점"),
            createTagSection(bookMap, "섬세한",
                "비가 오면 열리는 상점", "이중 하나는 거짓말", "메리골드 마음 세탁소"),
            createTagSection(bookMap, "직설적",
                "트렌드 코리아 2026", "시대예보: 핵개인의 시대", "마흔에 읽는 쇼펜하우어"),
            createTagSection(bookMap, "은유적",
                "비가 오면 열리는 상점", "이중 하나는 거짓말", "모순")
        );
    }

    /**
     * 몰입도별 베스트셀러 섹션 구성
     * PM 데이터: 몰입도 세부 태그별 전체 도서 (TOP 3가 아님)
     */
    private List<TaggedBooksSection> buildImmersionBestsellers(Map<String, BookSummary> bookMap) {
        return List.of(
            createTagSection(bookMap, "가볍게 읽기 좋은",
                "트렌드 코리아 2026", "돈의 속성 (300쇄 리미티드)", "나의 서투른 위로가 너에게 닿기를"),
            createTagSection(bookMap, "생각이 필요한",
                "모순", "시대예보: 핵개인의 시대", "마흔에 읽는 쇼펜하우어"),
            createTagSection(bookMap, "쉽게 빠져드는",
                "비가 오면 열리는 상점", "메리골드 마음 세탁소", "불편한 편의점"),
            createTagSection(bookMap, "여운이 남는",
                "이중 하나는 거짓말", "작별인사", "물고기는 존재하지 않는다")
        );
    }

    /**
     * 태그 섹션 생성 헬퍼 메서드
     * bookMap에서 조회하여 태그 섹션 생성
     */
    private TaggedBooksSection createTagSection(Map<String, BookSummary> bookMap,
                                                 String tagName, String... bookTitles) {
        List<BookSummary> books = new ArrayList<>();
        for (String title : bookTitles) {
            // ranking을 null로 설정한 새로운 BookSummary 생성
            BookSummary original = bookMap.get(title);
            if (original != null) {
                books.add(new BookSummary(
                        original.bookId(),
                        original.title(),
                        original.author(),
                        original.publisher(),
                        original.coverImageUrl(),
                        null // 태그별 섹션에서는 ranking null
                ));
            } else {
                Long bookId = BOOK_ID_MAPPING.getOrDefault(title, 0L);
                books.add(createFallback(bookId, title, null));
            }
        }

        return new TaggedBooksSection(tagName, books);
    }

    /**
     * Fallback BookSummary 생성
     */
    private BookSummary createFallback(Long bookId, String title, Integer ranking) {
        return new BookSummary(bookId, title, null, null, null, ranking);
    }
}


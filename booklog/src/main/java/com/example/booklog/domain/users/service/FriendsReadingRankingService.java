package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.repository.FriendsReadingRankingQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendsReadingRankingService {

    private final FriendsReadingRankingQueryRepository rankingQueryRepository;

    /** ✅ Top3만 */
    public FriendReadingRankingTop3Response getTop3(Long meId, String month) {
        DateRange range = range(month);

        List<FriendReadingRankingItem> top3 = rankingQueryRepository
                .findAfterRank(meId, range.start, range.end, 0, 3)
                .stream()
                .map(this::toItem)
                .toList();

        return new FriendReadingRankingTop3Response(month, top3);
    }

    /** ✅ 전체보기 무한스크롤 (Top3 포함 + items) */
    public FriendReadingRankingInfiniteResponse getInfinite(Long meId, String month, Integer cursor, Integer size) {
        DateRange range = range(month);

        int finalSize = (size != null ? size : 20);
        int afterRank = (cursor != null ? cursor : 0);

        // 첫 호출이면 Top3 포함 + items는 4등부터
        List<FriendReadingRankingItem> top3 = Collections.emptyList();
        int itemsAfterRank = afterRank;

        if (afterRank == 0) {
            top3 = rankingQueryRepository.findAfterRank(meId, range.start, range.end, 0, 3)
                    .stream()
                    .map(this::toItem)
                    .toList();
            itemsAfterRank = 3; // 4등부터 내려주기
        }

        // hasNext 판단용 size+1
        List<FriendReadingRankingRow> rows = rankingQueryRepository
                .findAfterRank(meId, range.start, range.end, itemsAfterRank, finalSize + 1);

        boolean hasNext = rows.size() > finalSize;
        List<FriendReadingRankingRow> sliced = hasNext ? rows.subList(0, finalSize) : rows;

        List<FriendReadingRankingItem> items = sliced.stream()
                .map(this::toItem)
                .toList();

        Integer nextCursor = (hasNext && !items.isEmpty())
                ? items.get(items.size() - 1).rank()
                : null;

        return new FriendReadingRankingInfiniteResponse(
                month,
                top3,
                items,
                nextCursor,
                hasNext
        );
    }

    // ===== helper =====

    private FriendReadingRankingItem toItem(FriendReadingRankingRow r) {
        return new FriendReadingRankingItem(
                r.getRank(),
                r.getUserId(),
                r.getNickname(),
                r.getProfileImageUrl(),
                nvl(r.getCompletedCount()),
                nvl(r.getReadingDays())
        );
    }

    private long nvl(Long v) { return v == null ? 0L : v; }

    private DateRange range(String month) {
        YearMonth ym = YearMonth.parse(month); // "YYYY-MM"
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}

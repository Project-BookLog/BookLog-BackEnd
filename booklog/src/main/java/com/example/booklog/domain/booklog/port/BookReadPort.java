package com.example.booklog.domain.booklog.port;

import com.example.booklog.domain.booklog.view.BookView;

import java.util.List;

public interface BookReadPort {
    boolean existsById(Long bookId);

    BookView findBook(Long bookId);

    // 추천 기능(태그 기반 랭킹순) - 구현은 우선 TODO 처리 가능
    List<BookView> findSimilarBooksByTagIdsOrderByRanking(
            List<Long> tagIds,
            Long excludeBookId,
            int limit
    );
}
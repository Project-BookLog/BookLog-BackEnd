package com.example.booklog.domain.booklog.view;

import java.time.LocalDateTime;

public interface SimilarBookAggView {
    Long getBookId();
    Long getOverlapTags();      // 겹치는 태그 수
    Long getTotalViews();       // 해당 book 관련 post 조회수 합
    Long getTotalBookmarks();   // 해당 book 관련 post 북마크 합 (없으면 0으로 대체 가능)
    LocalDateTime getLatestPostAt();
}
package com.example.booklog.domain.booklog.port;

import com.example.booklog.domain.booklog.view.AuthorView;

public interface UserReadPort {
    boolean existsById(Long userId);

    AuthorView findAuthorSummary(Long userId);

    // 상세가 필요 없으면 나중에 지워도 됨 (지금은 Facade 시그니처 맞추기용)
    AuthorView findAuthorDetail(Long userId, Long viewerId);
}
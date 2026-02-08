package com.example.booklog.domain.users.service;

import com.example.booklog.domain.library.shelves.entity.ReadingStatus;
import com.example.booklog.domain.users.dto.MeProfileWithStatsResponse;
import com.example.booklog.domain.users.repository.MeProfileWithStatsQueryRepository;
import com.example.booklog.domain.users.repository.projection.MeProfileWithStatsProjection;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeProfileWithStatsService {

    private final MeProfileWithStatsQueryRepository queryRepository;

    public MeProfileWithStatsResponse getMyProfileWithStats(Long userId, String emailFromLoginAccount) {

        MeProfileWithStatsProjection p = queryRepository
                .findMeProfileWithStats(userId, ReadingStatus.COMPLETED.name())
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean shelfPublic = toBoolDefaultTrue(p.getIsShelfPublic());
        boolean booklogPublic = toBoolDefaultTrue(p.getIsBooklogPublic());

        return new MeProfileWithStatsResponse(
                p.getUserId(),
                p.getNickname(),
                p.getAvatarUrl(),
                shelfPublic,
                booklogPublic,

                emailFromLoginAccount,

                nvl(p.getFollowerCount()),
                nvl(p.getFollowingCount()),
                nvl(p.getCompletedBookCount()),
                nvl(p.getMyBooklogCount()),
                nvl(p.getBookmarkCount())
        );
    }

    private boolean toBoolDefaultTrue(Integer v) {
        // settings 없으면 기본 true여야 하니까 null -> true
        return v == null || v == 1;
    }

    private long nvl(Number v) {
        // COUNT(*)가 BigInteger/BigDecimal로 와도 안전
        return v == null ? 0L : v.longValue();
    }
}

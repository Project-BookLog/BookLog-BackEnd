package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.UserProfileResponse;
import com.example.booklog.domain.users.repository.UserProfileQueryRepository;
import com.example.booklog.domain.users.repository.projection.UserProfileSummaryProjection;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileQueryRepository userProfileQueryRepository;

    public UserProfileResponse getProfile(Long meId, Long targetUserId) {
        UserProfileSummaryProjection p = userProfileQueryRepository
                .findUserProfileSummary(meId, targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        return new UserProfileResponse(
                p.getUserId(),
                p.getNickname(),
                p.getEmail(),
                p.getAvatarUrl(),

                safeLong(p.getFollowerCount()),
                safeLong(p.getFollowingCount()),

                safeLong(p.getSavedBookCount()),
                safeLong(p.getCompletedBookCount()),
                safeLong(p.getBooklogCount()),
                safeLong(p.getBookmarkCount()),

                // ✅ 0/1(Long) -> boolean
                toBool(p.getIsFollowing()),
                toBool(p.getIsShelfPublic()),
                toBool(p.getIsBooklogPublic())
        );
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private boolean toBool(Long v) {
        return v != null && v == 1L;
    }
}

package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.*;
import com.example.booklog.domain.users.entity.UserFollows;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UserFollowsRepository;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFollowService {

    private final UsersRepository usersRepository;
    private final UserFollowsRepository userFollowsRepository;

    /** 팔로우(멱등) + 맞팔 여부 */
    public FollowActionResponse follow(Long meId, Long targetUserId) {
        validateNotSelf(meId, targetUserId);

        Users me = usersRepository.findById(meId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Users target = usersRepository.findById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 멱등 처리: 이미 있으면 그대로 반환
        UserFollows relation = userFollowsRepository
                .findByFollowerIdAndFolloweeId(meId, targetUserId)
                .orElseGet(() -> userFollowsRepository.save(
                        UserFollows.builder()
                                .follower(me)
                                .followee(target)
                                .build()
                ));

        boolean isMutual = userFollowsRepository.existsByFollowerIdAndFolloweeId(targetUserId, meId);

        return new FollowActionResponse(
                targetUserId,
                true,
                isMutual,
                relation.getFollowedAt()
        );
    }

    /** 언팔로우(멱등) */
    public FollowActionResponse unfollow(Long meId, Long targetUserId) {
        validateNotSelf(meId, targetUserId);

        // 정책: 대상 유저가 없으면 404
        if (!usersRepository.existsById(targetUserId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        userFollowsRepository.deleteByFollowerIdAndFolloweeId(meId, targetUserId);

        return new FollowActionResponse(
                targetUserId,
                false,
                false,
                null
        );
    }

    /** 맞팔 친구 목록(무한스크롤) */
    @Transactional(readOnly = true)
    public MutualFriendsResponse getMutualFriends(Long meId, Long cursor, Integer size) {
        int finalSize = (size != null ? size : 20);

        // hasNext 판단을 위해 size+1로 조회
        /*
        무한 스크롤의 핵심
        cursor(커서) 로 “어디부터 이어서 가져올지”를 정하고
        size(LIMIT) 로 “몇 개 가져올지”를 정하는 것
        */
        List<MutualFriendRow> rows = usersRepository.findMutualFriendsCursor(
                meId,
                cursor,
                PageRequest.of(0, finalSize + 1)
        );

        boolean hasNext = rows.size() > finalSize;
        List<MutualFriendRow> sliced = hasNext ? rows.subList(0, finalSize) : rows;

        List<MutualFriendItem> items = sliced.stream()
                .map(r -> new MutualFriendItem(r.getUserId(), r.getNickname(), r.getProfileImageUrl()))
                .toList();

        Long nextCursor = (hasNext && !items.isEmpty())
                ? items.get(items.size() - 1).userId()
                : null;

        return new MutualFriendsResponse(items, nextCursor, hasNext);
    }

    private void validateNotSelf(Long meId, Long targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new GeneralException(ErrorStatus.SELF_FOLLOW_NOT_ALLOWED);
        }
    }
}

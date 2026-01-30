package com.example.booklog.domain.booklog.port;

import com.example.booklog.domain.booklog.port.UserReadPort;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserReadPortImpl implements UserReadPort {

    private final UsersRepository usersRepository;

    @Override
    public boolean existsById(Long userId) {
        return usersRepository.existsById(userId);
    }

    @Override
    public AuthorView findAuthorSummary(Long userId) {
        Users u = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // summary에서는 email/followedByMe는 null 허용
        return new AuthorViewImpl(
                u.getId(),
                u.getNickname(),
                null,
                u.getProfileImageUrl(),
                null
        );
    }

    @Override
    public AuthorView findAuthorDetail(Long userId, Long viewerId) {
        Users u = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 팔로우 도메인이 아직 없으면 우선 null 또는 false로 둠
        Boolean followedByMe = null; // 또는 false

        return new AuthorViewImpl(
                u.getId(),
                u.getNickname(),
                null, // 상세에서는 email 반환
                u.getProfileImageUrl(),
                followedByMe
        );
    }

    @Override
    public List<AuthorView> findAuthorSummariesByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        // usersRepository는 JpaRepository라서 findAllById 지원
        List<Users> users = usersRepository.findAllById(userIds);

        return users.stream()
                .map(u -> (AuthorView) new AuthorViewImpl(
                        u.getId(),
                        u.getNickname(),
                        null,
                        u.getProfileImageUrl(),
                        null
                ))
                .toList();
    }

    /**
     * AuthorView 구현체 (record로 간단히)
     */
    public record AuthorViewImpl(
            Long userId,
            String nickname,
            String email,
            String profileImageUrl,
            Boolean followedByMe
    ) implements AuthorView {
        @Override public Long getUserId() { return userId; }
        @Override public String getNickname() { return nickname; }
        @Override public String getEmail() { return email; }
        @Override public String getProfileImageUrl() { return profileImageUrl; }
        @Override public Boolean getFollowedByMe() { return followedByMe; }
    }
}
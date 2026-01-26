package com.example.booklog.domain.booklog.port;

import com.example.booklog.domain.booklog.port.UserReadPort;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                .orElseThrow(() -> new IllegalArgumentException("유저 없음. userId=" + userId));

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
                .orElseThrow(() -> new IllegalArgumentException("유저 없음. userId=" + userId));

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
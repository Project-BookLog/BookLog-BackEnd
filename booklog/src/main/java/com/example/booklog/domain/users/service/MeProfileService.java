package com.example.booklog.domain.users.service;

import com.example.booklog.domain.users.dto.MeProfileResponse;
import com.example.booklog.domain.users.dto.MeProfileUpdateRequest;
import com.example.booklog.domain.users.entity.UserSettings;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UserSettingsRepository;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeProfileService {

    private final UsersRepository usersRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Transactional(readOnly = true)
    public MeProfileResponse getMyProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> null);

        // settings가 없을 수도 있으니 기본값 정책 지정(현재 엔티티 기본값 true)
        boolean shelfPublic = settings != null ? Boolean.TRUE.equals(settings.getIsShelfPublic()) : true;
        boolean booklogPublic = settings != null ? Boolean.TRUE.equals(settings.getIsPostPublic()) : true;

        return new MeProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                shelfPublic,
                booklogPublic
        );
    }

    @Transactional
    public MeProfileResponse updateProfile(Long userId, MeProfileUpdateRequest req) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.));

        // settings upsert (없으면 생성)
        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> userSettingsRepository.save(
                        UserSettings.builder()
                                .user(user)
                                .isShelfPublic(true)
                                .isPostPublic(true)
                                .build()
                ));

        // 1) nickname PATCH: null이면 변경 X, 값이 오면 검증 후 변경
        if (req.nickname() != null) {
            String nn = req.nickname().trim();
            if (nn.isEmpty()) throw new IllegalArgumentException("NICKNAME_EMPTY");
            if (nn.length() > 50) throw new IllegalArgumentException("NICKNAME_TOO_LONG");

            // profileImageUrl은 유지 (사진은 PUT /avatar)
            user.updateProfile(nn, user.getProfileImageUrl());
        }

        // 2) 공개 토글 PATCH
        if (req.isShelfPublic() != null) {
            settings.updateShelfPublic(req.isShelfPublic());
        }
        if (req.isBooklogPublic() != null) {
            // DTO: isBooklogPublic -> Entity: isPostPublic
            settings.updatePostPublic(req.isBooklogPublic());
        }

        return new MeProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                settings.getIsShelfPublic(),
                settings.getIsPostPublic()
        );
    }
}

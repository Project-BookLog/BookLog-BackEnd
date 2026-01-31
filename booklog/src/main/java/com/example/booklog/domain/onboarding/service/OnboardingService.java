package com.example.booklog.domain.onboarding.service;

import com.example.booklog.domain.onboarding.dto.OnboardingProfileResponse;
import com.example.booklog.domain.onboarding.dto.UpdateOnboardingAnswersRequest;
import com.example.booklog.domain.onboarding.entity.*;
import com.example.booklog.domain.onboarding.exception.OnboardingProfileNotFoundException;
import com.example.booklog.domain.onboarding.exception.UserNotFoundException;
import com.example.booklog.domain.onboarding.repository.UserOnboardingStatusRepository;
import com.example.booklog.domain.onboarding.repository.UserReadingProfileRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 서비스
 * - 유저 독서 취향 프로필 관리
 * - 온보딩 완료 상태 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final UserReadingProfileRepository profileRepository;
    private final UserOnboardingStatusRepository statusRepository;
    private final UsersRepository usersRepository;

    /**
     * 온보딩 응답 저장 (부분 업데이트)
     * @param userId 인증된 사용자 ID (Security Context에서 추출)
     * @param request 온보딩 응답 데이터
     */
    @Transactional
    public OnboardingProfileResponse updateOnboardingAnswers(Long userId, UpdateOnboardingAnswersRequest request) {
        // 사용자 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        // 프로필 조회 또는 생성
        UserReadingProfile profile = profileRepository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            // 새 프로필 생성
            profile = UserReadingProfile.builder()
                    .user(user)
                    .build();
            profile = profileRepository.save(profile);
        }

        // 부분 업데이트
        profile.updateProfile(
                request.getReaderType(),
                request.getPreferredMood1(),
                request.getPreferredMood2(),
                request.getSentenceBreath(),
                request.getExpressionTexture(),
                request.getExpressionDirection(),
                request.getReadingMoment()
        );

        // 상태 조회
        UserOnboardingStatus status = statusRepository.findByUserId(userId).orElse(null);

        return OnboardingProfileResponse.from(profile, status);
    }

    /**
     * 온보딩 완료 처리
     * @param userId 인증된 사용자 ID (Security Context에서 추출)
     */
    @Transactional
    public OnboardingProfileResponse completeOnboarding(Long userId) {
        // 사용자 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        // 프로필 조회 (온보딩 완료는 프로필이 있어야 가능)
        UserReadingProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new OnboardingProfileNotFoundException());

        // 상태 조회 또는 생성
        UserOnboardingStatus status = statusRepository.findByUserId(userId)
                .orElse(null);

        if (status == null) {
            // 새 상태 생성
            status = UserOnboardingStatus.builder()
                    .user(user)
                    .build();
            status = statusRepository.save(status);
        }

        // 완료 처리 (멱등성 보장)
        status.complete();

        return OnboardingProfileResponse.from(profile, status);
    }

    /**
     * 온보딩 프로필 조회
     * - 프로필이 없는 경우 빈 응답 반환 (200 OK)
     * - 500 에러를 발생시키지 않음
     *
     * @param userId 인증된 사용자 ID
     * @return 온보딩 프로필 응답 (없으면 빈 응답)
     */
    public OnboardingProfileResponse getOnboardingProfile(Long userId) {
        UserReadingProfile profile = profileRepository.findByUserId(userId)
                .orElse(null);

        // 프로필이 없으면 빈 응답 반환
        if (profile == null) {
            return OnboardingProfileResponse.empty();
        }

        UserOnboardingStatus status = statusRepository.findByUserId(userId).orElse(null);

        return OnboardingProfileResponse.from(profile, status);
    }
}


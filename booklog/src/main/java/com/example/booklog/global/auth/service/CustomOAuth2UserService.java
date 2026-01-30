package com.example.booklog.global.auth.service;

import com.example.booklog.domain.users.entity.AuthAccounts;
import com.example.booklog.domain.users.entity.AuthProvider;
import com.example.booklog.domain.users.entity.UserStatus;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.auth.repository.AuthAccountsRepository;
import com.example.booklog.global.auth.dto.KakaoOAuth2UserInfo;
import com.example.booklog.global.auth.dto.OAuth2UserInfo;
import com.example.booklog.global.auth.enums.Role;
import com.example.booklog.global.auth.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 로그인 사용자 정보 처리 서비스
 * 카카오 로그인 시 사용자 정보를 받아 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthAccountsRepository authAccountsRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // OAuth2 제공자로부터 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 제공자 이름 가져오기 (kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 시도: provider={}", registrationId);

        // 카카오 사용자 정보 파싱
        OAuth2UserInfo oAuth2UserInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());

        // 사용자 정보로 AuthAccounts 찾거나 생성
        AuthAccounts authAccount = saveOrUpdate(oAuth2UserInfo);

        // CustomOAuth2User 반환
        return new CustomOAuth2User(authAccount, oAuth2User.getAttributes());
    }

    /**
     * OAuth2 사용자 정보로 AuthAccounts를 찾거나 생성
     */
    private AuthAccounts saveOrUpdate(OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider provider = AuthProvider.valueOf(oAuth2UserInfo.getProvider());
        String providerId = oAuth2UserInfo.getProviderId();

        // 기존 계정이 있는지 확인
        AuthAccounts authAccount = authAccountsRepository
                .findByProviderIdAndProvider(providerId, provider)
                .orElse(null);

        if (authAccount != null) {
            // 기존 계정이면 마지막 로그인 시간 업데이트
            log.info("기존 OAuth2 사용자 로그인: providerId={}, provider={}", providerId, provider);
            authAccount.updateLastLogin();

            // 프로필 정보가 변경되었으면 업데이트
            authAccount.updateProfile(
                    oAuth2UserInfo.getEmail(),
                    oAuth2UserInfo.getName(),
                    oAuth2UserInfo.getProfileImageUrl()
            );

            return authAccountsRepository.save(authAccount);
        }

        // 신규 계정 생성
        log.info("신규 OAuth2 사용자 등록: providerId={}, provider={}", providerId, provider);

        // Users 엔티티 생성
        Users newUser = Users.builder()
                .nickname(oAuth2UserInfo.getName())
                .profileImageUrl(oAuth2UserInfo.getProfileImageUrl())
                .status(UserStatus.ACTIVE)
                .build();

        // Users 먼저 저장
        usersRepository.save(newUser);

        // AuthAccounts 생성
        AuthAccounts newAuthAccount = AuthAccounts.builder()
                .user(newUser)
                .provider(provider)
                .providerId(providerId)
                .email(oAuth2UserInfo.getEmail())
                .displayName(oAuth2UserInfo.getName())
                .profileImageUrl(oAuth2UserInfo.getProfileImageUrl())
                .role(Role.ROLE_USER)
                .build();

        return authAccountsRepository.save(newAuthAccount);
    }
}

package com.example.booklog.domain.users.service;

import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogStatus;
import com.example.booklog.domain.booklog.repository.BooklogBookmarkRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostRepository;
import com.example.booklog.domain.booklog.service.BooklogReadFacade;
import com.example.booklog.domain.users.entity.UserSettings;
import com.example.booklog.domain.users.repository.UserSettingsRepository;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.generalStatus.GeneralErrorCode;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserBooklogReadService {

    private final UsersRepository usersRepository;
    private final UserSettingsRepository userSettingsRepository;

    private final BooklogPostRepository postRepository;
    private final BooklogBookmarkRepository bookmarkRepository;

    private final BooklogReadFacade booklogReadFacade;

    /**
     * 다른 유저 공개 북로그 조회
     */
    @Transactional(readOnly = true)
    public BooklogFeedResponse getUserPublicBooklogs(Long viewerId, Long targetUserId, Pageable pageable) {

        // 1) 유저 존재
        if (!usersRepository.existsById(targetUserId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        // 2) 공개 토글 확인 (settings 없으면 기본 공개 정책이면 true로)
        UserSettings settings = userSettingsRepository.findById(targetUserId).orElse(null);
        boolean isPostPublic = (settings == null) ? true : Boolean.TRUE.equals(settings.getIsPostPublic());

        if (!isPostPublic) {
            // 정책 선택:
            // - 403이 더 정확
            // - 또는 존재 자체 숨기려면 404로 처리
            throw new AccessDeniedException("권한이 없습니다.");
        }

        // 3) slice 조회
        Slice<BooklogPost> slice = postRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                targetUserId,
                BooklogStatus.PUBLISHED,
                pageable
        );

        // 4) 공용 조립
        return booklogReadFacade.assembleFeedCards(viewerId, slice);
    }

    /**
     * 내 북로그 조회
     */
    @Transactional(readOnly = true)
    public BooklogFeedResponse getMyBooklogs(Long meId, Pageable pageable) {

        Slice<BooklogPost> slice = postRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                meId,
                BooklogStatus.PUBLISHED,
                pageable
        );

        return booklogReadFacade.assembleFeedCards(meId, slice);
    }

    /**
     * 내 북마크한 북로그 조회
     * - 북마크 테이블에서 postId를 최신순(id desc)으로 뽑고
     * - posts를 조회한 후, bookmark 순서대로 정렬해서 slice를 만든다
     */
    @Transactional(readOnly = true)
    public BooklogFeedResponse getMyBookmarkedBooklogs(Long meId, Pageable pageable) {

        // (1) bookmark 테이블에서 postIds 최신순
        // hasNext 판단 위해 size+1로 뽑는 게 좋아서 Pageable을 강제 조정
        int size = pageable.getPageSize();
        Pageable plusOne = PageRequest.of(pageable.getPageNumber(), size + 1);

        List<Long> postIds = bookmarkRepository.findMyBookmarkedPostIds(meId, plusOne);

        boolean hasNext = postIds.size() > size;
        List<Long> slicedIds = hasNext ? postIds.subList(0, size) : postIds;

        if (slicedIds.isEmpty()) {
            return BooklogFeedResponse.builder()
                    .items(List.of())
                    .hasNext(false)
                    .build();
        }

        // (2) 게시글 조회 (PUBLISHED만)
        // 주의: findAllByIdIn...은 createdAt desc 정렬이라 bookmark 순서와 달라질 수 있음
        // 그래서 "bookmark 순서" 유지하려면 id 리스트 기반으로 재정렬해야 함.
        List<BooklogPost> fetched = postRepository.findAllById(slicedIds);

        Map<Long, BooklogPost> map = new HashMap<>();
        for (BooklogPost p : fetched) {
            // DELETED일 수도 있으니 필터
            if (p.getStatus() == BooklogStatus.PUBLISHED) {
                map.put(p.getId(), p);
            }
        }

        List<BooklogPost> ordered = new ArrayList<>();
        for (Long id : slicedIds) {
            BooklogPost p = map.get(id);
            if (p != null) ordered.add(p);
        }

        // (3) Slice로 감싸기
        Slice<BooklogPost> slice = new SliceImpl<>(ordered, pageable, hasNext);

        // (4) 공용 조립
        return booklogReadFacade.assembleFeedCards(meId, slice);
    }
}
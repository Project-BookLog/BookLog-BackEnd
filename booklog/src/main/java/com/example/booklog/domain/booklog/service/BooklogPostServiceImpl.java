package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.booklog.converter.BooklogDetailConverter;
import com.example.booklog.domain.booklog.converter.BooklogFeedConverter;
import com.example.booklog.domain.booklog.converter.BooklogPostConverter;
import com.example.booklog.domain.booklog.dto.*;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogPostImage;
import com.example.booklog.domain.booklog.entity.BooklogPostTag;
import com.example.booklog.domain.booklog.repository.BooklogPostImageRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostTagRepository;
import com.example.booklog.domain.booklog.repository.ViewLogRepository;
import com.example.booklog.domain.tags.repository.TagsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BooklogPostServiceImpl implements BooklogPostService {

    private final TagsRepository tagsRepository;
    private final BooklogPostRepository postRepository;
    private final BooklogPostImageRepository postImageRepository;
    private final BooklogPostTagRepository postTagRepository;
    private final ViewLogRepository  viewLogRepository;

    private final BooklogReadFacade booklogReadFacade;

    private final BooklogPostConverter  postConverter;
    private final BooklogFeedConverter feedConverter;
    private final BooklogDetailConverter detailConverter;





    // 1. 게시글 발행

    @Transactional
    @Override
    public BooklogPostCreateResponse create(Long userId, BooklogPostCreateRequest request){

        // null 아님
        List<Long> tagIds = request.getTagIds();

        List<String> imageUrls = request.getImageUrls();

        // 태그 사실 조회 (활성화 상태인지) + 북로그 올릴 때 태그 규칙 적용
        validateBooklogTagRules(tagIds);

        // 이미지 최대 8개
        validateImageLimit(imageUrls);

        // 게시글 저장
        BooklogPost saved = postRepository.save(
                postConverter.toEntityForCreate(userId, request)
        );

        // 태그 매핑 저장
        savePostTags(saved.getId(), tagIds);

        // 이미지 저장
        savePostImages(saved.getId(), imageUrls);

        return postConverter.toCreateResponse(saved.getId());

    }

    private void validateBooklogTagRules(List<Long> tagIds) {
        // 태그 사실 조회(존재/활성/카테고리)
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("태그는 최소 1개 이상 선택해야 합니다.");
        }

        var tags = tagsRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 태그가 포함되어 있습니다.");
        }

        long moodCount = tags.stream()
                .filter(t -> t.getCategory() == TagCategory.MOOD)
                .count();

        long styleCount = tags.stream()
                .filter(t -> t.getCategory() == TagCategory.STYLE)
                .count();

        long immersionCount = tags.stream()
                .filter(t -> t.getCategory() == TagCategory.IMMERSION)
                .count();

        if (moodCount < 1 || moodCount > 2) throw new IllegalArgumentException("MOOD 태그는 1~2개 선택해야 합니다.");
        if (styleCount < 1 || styleCount > 2) throw new IllegalArgumentException("STYLE 태그는 1~2개 선택해야 합니다.");
        if (immersionCount != 1) throw new IllegalArgumentException("IMMERSION 태그는 반드시 1개 선택해야 합니다.");

    }

    // 이미지 최대 8개
    private void validateImageLimit(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > 8) {
            throw new IllegalArgumentException("이미지는 최대 8장까지 가능합니다.");
        }
    }

    // post 태그 매핑
    private void savePostTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;

        for (Long tagId : tagIds) {
            postTagRepository.save(BooklogPostTag.of(postId, tagId));
        }
    }

    // post 이미지 저장
    private void savePostImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        int order = 0;
        for (String url : imageUrls) {
            postImageRepository.save(BooklogPostImage.of(postId, url, order++));
        }
    }

    /**
     * 피드 조회 (Slice 기반)
     */
    @Override
    @Transactional(readOnly = true)
    public BooklogFeedResponse getFeed(
            Long userId,
            BooklogFeedQuery query,
            Pageable pageable
    ) {
        // TODO
        // 1. 조건(query)에 따라 repository 조회
        // 2. Slice<BooklogPost> 반환
        // 3. postId 리스트 추출
        // 4. 태그 / 이미지 / 북 정보 batch 조회
        // 5. CardResponse로 변환
        // 6. hasNext 포함하여 FeedResponse 생성

        throw new UnsupportedOperationException("getFeed not implemented yet");
    }

    /**
     * 북로그 상세 조회
     */
    @Override
    @Transactional(readOnly = true)
    public BooklogDetailResponse getDetail(Long userId, Long postId) {
        // TODO
        // 1. post 조회 (status = PUBLISHED)
        // 2. 이미지 / 태그 / 책 / 작성자 정보 조회
        // 3. bookmarkedByMe 여부 계산
        // 4. DetailResponse 조립

        throw new UnsupportedOperationException("getDetail not implemented yet");
    }

    /**
     * 상세화면 추천 영역 (비슷한 도서 / 비슷한 글)
     */
    @Override
    @Transactional(readOnly = true)
    public BooklogRecommendationResponse getRecommendations(Long userId, Long postId) {
        // TODO
        // 1. 현재 post의 책 / 태그 기반으로 추천 기준 계산
        // 2. 비슷한 도서 목록 조회
        // 3. 비슷한 주제의 인기 글 조회
        // 4. RecommendationResponse 조립

        throw new UnsupportedOperationException("getRecommendations not implemented yet");
    }

    /**
     * 북로그 삭제 (soft delete)
     */
    @Override
    public void softDelete(Long userId, Long postId) {
        // TODO
        // 1. post 조회 (작성자 검증)
        // 2. status = DELETED 변경
        // 3. repository update 또는 엔티티 변경

        throw new UnsupportedOperationException("softDelete not implemented yet");
    }
}

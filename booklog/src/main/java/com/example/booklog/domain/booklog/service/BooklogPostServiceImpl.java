package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.converter.BooklogDetailConverter;
import com.example.booklog.domain.booklog.converter.BooklogFeedConverter;
import com.example.booklog.domain.booklog.converter.BooklogPostConverter;
import com.example.booklog.domain.booklog.dto.*;
import com.example.booklog.domain.booklog.entity.*;
import com.example.booklog.domain.booklog.repository.*;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.PostImageView;
import com.example.booklog.domain.booklog.view.TagView;
import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BooklogPostServiceImpl implements BooklogPostService {

    private final TagsRepository tagsRepository;
    private final BooklogPostRepository postRepository;
    private final BooklogPostImageRepository postImageRepository;
    private final BooklogPostTagRepository postTagRepository;
    private final ViewLogRepository viewLogRepository;
    private final BooklogBookmarkRepository bookmarkRepository;

    private final BooklogReadFacade booklogReadFacade;

    private final BooklogPostConverter postConverter;
    private final BooklogFeedConverter feedConverter;
    private final BooklogDetailConverter detailConverter;

    // 1) 게시글 발행
    @Transactional
    @Override
    public BooklogPostCreateResponse create(Long userId, BooklogPostCreateRequest request) {

        List<Long> tagIds = request.getTagIds();
        List<String> imageUrls = request.getImageUrls();

        validateBooklogTagRules(tagIds);
        validateImageLimit(imageUrls);

        BooklogPost saved = postRepository.save(
                postConverter.toEntityForCreate(userId, request)
        );

        savePostTags(saved.getId(), tagIds);
        savePostImages(saved.getId(), imageUrls);

        return postConverter.toCreateResponse(saved.getId());
    }

    private void validateBooklogTagRules(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new GeneralException(ErrorStatus.TAG_MIN_ONE_REQUIRED);
        }

        var tags = tagsRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            throw new GeneralException(ErrorStatus.TAG_NOT_FOUND_INCLUDED);
        }

        long moodCount = tags.stream().filter(t -> t.getCategory() == TagCategory.MOOD).count();
        long styleCount = tags.stream().filter(t -> t.getCategory() == TagCategory.STYLE).count();
        long immersionCount = tags.stream().filter(t -> t.getCategory() == TagCategory.IMMERSION).count();

        if (moodCount < 1 || moodCount > 2) throw new GeneralException(ErrorStatus.MOOD_TAG_COUNT_INVALID);
        if (styleCount < 1 || styleCount > 2) throw new GeneralException(ErrorStatus.STYLE_TAG_COUNT_INVALID);
        if (immersionCount != 1) throw new GeneralException(ErrorStatus.IMMERSION_TAG_COUNT_INVALID);
    }

    private void validateImageLimit(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > 8) {
            throw new GeneralException(ErrorStatus.IMAGE_MAX_8);
        }
    }

    private void savePostTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;

        List<BooklogPostTag> entities = tagIds.stream()
                .distinct() // 혹시 모를 중복 방지(UK 걸려있음)
                .map(tagId -> BooklogPostTag.of(postId, tagId))
                .toList();

        postTagRepository.saveAll(entities);
    }

    private void savePostImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        List<BooklogPostImage> entities = new java.util.ArrayList<>();
        int order = 0;
        for (String url : imageUrls) {
            entities.add(BooklogPostImage.of(postId, url, order++));
        }
        postImageRepository.saveAll(entities);
    }

    /**
     * 2) 피드 조회 (Slice + 드롭다운 필터)
     * - 성능: images/tags/bookmarkCount는 배치 조회
     */
    @Override
    @Transactional(readOnly = true)
    public BooklogFeedResponse getFeed(Long userId, BooklogFeedQuery query, Pageable pageable) {

        Slice<BooklogPost> slice = booklogReadFacade.findFeedPostsSlice(query, pageable);
        List<BooklogPost> posts = slice.getContent();

        if (posts.isEmpty()) {
            return feedConverter.toFeedResponse(List.of(), slice.hasNext());
        }

        List<Long> postIds = posts.stream().map(BooklogPost::getId).toList();

        List<Long> userIds = posts.stream().map(BooklogPost::getUserId).distinct().toList();
        List<Long> bookIds = posts.stream().map(BooklogPost::getBookId).distinct().toList();

        Map<Long, AuthorView> authorMap = booklogReadFacade.findAuthorSummariesByIds(userIds)
                .stream().collect(Collectors.toMap(AuthorView::getUserId, a -> a));

        Map<Long, BookView> bookMap = booklogReadFacade.findBooksByIds(bookIds)
                .stream().collect(Collectors.toMap(BookView::getBookId, b -> b));


        Set<Long> bookmarkedSet = new HashSet<>(
                bookmarkRepository.findBookmarkedPostIdsByUserIdInPostIds(userId, postIds)
        );

        // (1) 이미지 배치
        List<PostImageView> imageViews = postImageRepository.findByPostIdInOrderByPostIdAscDisplayOrderAsc(postIds);
        Map<Long, List<PostImageView>> imagesMap = imageViews.stream()
                .collect(Collectors.groupingBy(PostImageView::getPostId, LinkedHashMap::new, Collectors.toList()));

        // (2) 태그 배치 (postTag 매핑 배치 -> postId별 tagIds)
        List<BooklogPostTag> mappings = postTagRepository.findAllByPostIdIn(postIds);
        Map<Long, List<Long>> postIdToTagIds = mappings.stream()
                .collect(Collectors.groupingBy(BooklogPostTag::getPostId,
                        Collectors.mapping(BooklogPostTag::getTagId, Collectors.toList())));


        // 카드 조립
        List<BooklogPostCardResponse> cards = new ArrayList<>();
        for (BooklogPost p : posts) {
            Long postId = p.getId();

            AuthorView author = authorMap.get(p.getUserId());
            BookView book = bookMap.get(p.getBookId());

            List<? extends PostImageView> images = imagesMap.getOrDefault(postId, List.of());

            // 태그는 postId -> tagIds 를 TagView로 변환해서 사용 (Facade에 위임)
            List<? extends TagView> tags = booklogReadFacade.findTagsByTagIds(
                    postIdToTagIds.getOrDefault(postId, List.of())
            );

            boolean bookmarkedByMe = bookmarkedSet.contains(postId);
            long bookmarkCount = p.getBookmarkCount();

            cards.add(feedConverter.toCard(
                    postId,
                    author,
                    book,
                    p.getCreatedAt(),
                    p.getContent(),
                    p.getViewCount(),
                    bookmarkCount,
                    bookmarkedByMe,
                    images,
                    tags
            ));
        }

        return feedConverter.toFeedResponse(cards, slice.hasNext());
    }


    /**
     * 3) 상세 조회
     * - post.status=PUBLISHED 확인
     * - 조회수 +1 (update query)
     * - bookmarkCount/북마크여부/이미지/태그/책/작성자 조립
     */
    @Override
    @Transactional // 조회수 증가 때문에 readOnly면 안 됨
    public BooklogDetailResponse getDetail(Long userId, Long postId) {

        BooklogPost post = postRepository.findByIdAndStatus(postId, BooklogStatus.PUBLISHED)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));

        // 조회수 +1
        postRepository.increaseViewCount(postId, BooklogStatus.PUBLISHED);
        // (선택) view log 저장
        viewLogRepository.save(ViewLog.of(postId, userId));

        // 이미지
        List<PostImageView> images = postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId);

        // 태그
        List<? extends TagView> tags = booklogReadFacade.findTagsByPostId(postId);

        // 작성자/책
        AuthorView author = booklogReadFacade.findAuthorDetail(post.getUserId(), userId);
        BookView book = booklogReadFacade.findBook(post.getBookId());

        boolean bookmarkedByMe = booklogReadFacade.isBookmarkedByMe(userId, postId);
        long bookmarkCount = post.getBookmarkCount();

        // viewCount는 증가 쿼리 후 다시 조회 안 하고, 화면 반영만 필요하면 +1 처리
        long viewCount = post.getViewCount() + 1;

        return detailConverter.toDetail(
                postId,
                author,
                book,
                post.getContent(),
                post.getCreatedAt(),
                viewCount,
                bookmarkCount,
                bookmarkedByMe,
                images,
                tags
        );
    }

    /**
     * 4) 추천 (Facade가 조립)
     */
    @Override
    @Transactional(readOnly = true)
    public BooklogRecommendationResponse getRecommendations(Long userId, Long postId) {
        return booklogReadFacade.buildRecommendations(postId);
    }

    /**
     * 5) 삭제 (soft delete)
     */
    @Override
    @Transactional
    public void softDelete(Long userId, Long postId) {

        BooklogPost post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.POST_NOT_FOUND);
        }

        int updated = postRepository.softDelete(
                postId,
                BooklogStatus.PUBLISHED,
                BooklogStatus.DELETED,
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new GeneralException(ErrorStatus.POST_ALREADY_DELETED_OR_CANNOT_DELETE);
        }
    }

    @Override
    @Transactional
    public BookmarkToggleResult toggleBookmark(Long userId, Long postId) {

        // 1) 게시글 존재 + PUBLISHED 확인
        BooklogPost post = postRepository.findByIdAndStatus(postId, BooklogStatus.PUBLISHED)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));

        // 2) 이미 북마크한 row가 있는지 확인
        var existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);

        if (existing.isPresent()) {
            // 북마크 해제
            bookmarkRepository.delete(existing.get());
            postRepository.decreaseBookmarkCount(postId, BooklogStatus.PUBLISHED, LocalDateTime.now());

            // count 쿼리 없이 엔티티 값 기반으로 응답 (주의: 영속성 컨텍스트에 남아있을 수 있어 +1/-1만 반영)
            long count = Optional.ofNullable(
                    postRepository.findBookmarkCount(postId, BooklogStatus.PUBLISHED)
            ).orElse(0L);

            return BookmarkToggleResult.builder()
                    .bookmarkedByMe(false)
                    .bookmarkCount(count)
                    .build();
        }

        // 3) 없으면 북마크 생성 (동시성: unique 충돌 가능)
        try {
            bookmarkRepository.save(BooklogBookmark.of(userId, postId));
            postRepository.increaseBookmarkCount(postId, BooklogStatus.PUBLISHED, LocalDateTime.now());

            long count = Optional.ofNullable(
                    postRepository.findBookmarkCount(postId, BooklogStatus.PUBLISHED)
            ).orElse(0L);

            return BookmarkToggleResult.builder()
                    .bookmarkedByMe(true)
                    .bookmarkCount(count)
                    .build();

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시에 다른 요청이 먼저 insert했을 가능성
            // 토글이므로 "이미 북마크된 상태"로 보고 즉시 해제까지 수행
            var insertedByOther = bookmarkRepository.findByUserIdAndPostId(userId, postId);
            if (insertedByOther.isPresent()) {
                bookmarkRepository.delete(insertedByOther.get());
                postRepository.decreaseBookmarkCount(postId, BooklogStatus.PUBLISHED, LocalDateTime.now());
                long count = Optional.ofNullable(
                        postRepository.findBookmarkCount(postId, BooklogStatus.PUBLISHED)
                ).orElse(0L);

                return BookmarkToggleResult.builder()
                        .bookmarkedByMe(false)
                        .bookmarkCount(count)
                        .build();
            }
            // 정말 다른 무결성 오류라면 그대로 던짐
            throw e;
        }
    }
}
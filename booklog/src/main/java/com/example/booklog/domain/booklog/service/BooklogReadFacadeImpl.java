package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.converter.BooklogFeedConverter;
import com.example.booklog.domain.booklog.converter.BooklogRecommendationConverter;
import com.example.booklog.domain.booklog.dto.BooklogFeedQuery;
import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.booklog.dto.BooklogPostCardResponse;
import com.example.booklog.domain.booklog.dto.BooklogRecommendationResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogStatus;
import com.example.booklog.domain.booklog.entity.BooklogPostTag;
import com.example.booklog.domain.booklog.port.BookReadPort;
import com.example.booklog.domain.booklog.port.UserReadPort;
import com.example.booklog.domain.booklog.repository.*;
import com.example.booklog.domain.booklog.view.*;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BooklogReadFacadeImpl implements BooklogReadFacade {

    private final BooklogPostRepository postRepository;
    private final BooklogPostTagRepository postTagRepository;
    private final TagsRepository tagsRepository;
    private final BooklogBookmarkRepository bookmarkRepository;

    private final BooklogRecommendationConverter recommendationConverter;
    private final BooklogRecommendationRepository recommendationRepository;
    private final UserReadPort userReadPort;
    private final BookReadPort bookReadPort;

    private final BooklogPostImageRepository postImageRepository;
    private final BooklogFeedConverter feedConverter;

    @Override
    public AuthorView findAuthorSummary(Long userId) {
        return userReadPort.findAuthorSummary(userId);
    }

    @Override
    public AuthorView findAuthorDetail(Long userId, Long viewerId) {
        return userReadPort.findAuthorDetail(userId, viewerId);
    }

    @Override
    public List<AuthorView> findAuthorSummariesByIds(Collection<Long> userIds) {
        return userReadPort.findAuthorSummariesByIds(userIds);
    }

    @Override
    public BookView findBook(Long bookId) {
        return bookReadPort.findBook(bookId);
    }

    @Override
    public List<BookView> findBooksByIds(Collection<Long> bookIds) {
        return bookReadPort.findBooksByIds(bookIds);
    }

    @Override
    public List<TagView> findTagsByPostId(Long postId) {
        // postId -> tagId들
        List<BooklogPostTag> mappings = postTagRepository.findAllByPostId(postId);
        if (mappings.isEmpty()) return List.of();

        List<Long> tagIds = mappings.stream().map(BooklogPostTag::getTagId).distinct().toList();
        return findTagsByTagIds(tagIds);
    }


    @Override
    public List<TagView> findTagsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();

        List<Tags> tags = tagsRepository.findAllById(tagIds);

        Map<Long, Tags> tagMap = tags.stream()
                .collect(Collectors.toMap(Tags::getId, t -> t));

        List<TagView> result = new ArrayList<>();
        for (Long id : tagIds) {
            Tags t = tagMap.get(id);
            if (t != null) {
                result.add(new SimpleTagView(
                        t.getId(),
                        t.getName(),
                        String.valueOf(t.getCategory())
                ));
            }
        }
        return result;
    }

    @Override
    public boolean isBookmarkedByMe(Long userId, Long postId) {
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 드롭다운 필터 적용:
     * - 카테고리 내 OR
     * - 카테고리 간 AND
     */
    @Override
    public Slice<BooklogPost> findFeedPostsSlice(BooklogFeedQuery query, Pageable pageable) {

        List<Long> mood = (query == null) ? List.of() : safeList(query.getMoodTagIds());
        List<Long> style = (query == null) ? List.of() : safeList(query.getStyleTagIds());
        List<Long> immersion = (query == null) ? List.of() : safeList(query.getImmersionTagIds());

        boolean moodEmpty = mood.isEmpty();
        boolean styleEmpty = style.isEmpty();
        boolean immersionEmpty = immersion.isEmpty();

        if (moodEmpty && styleEmpty && immersionEmpty) {
            return postRepository.findAllByStatusOrderByCreatedAtDesc(BooklogStatus.PUBLISHED, pageable);
        }

        var page = postRepository.findPublishedFeedByTagFilters(
                moodEmpty, moodEmpty ? List.of(-1L) : mood,      // IN () 방지용 더미
                styleEmpty, styleEmpty ? List.of(-1L) : style,
                immersionEmpty, immersionEmpty ? List.of(-1L) : immersion,
                pageable
        );

        return page; // Page는 Slice를 상속하므로 그대로 반환 가능
    }

    private List<Long> safeList(List<Long> v) {
        return (v == null) ? List.of() : v;
    }

    @Override
    public BooklogRecommendationResponse buildRecommendations(Long postId) {

        BooklogPost base = postRepository.findByIdAndStatus(postId, BooklogStatus.PUBLISHED)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POST_NOT_FOUND));

        // 1) base post의 tagIds
        List<TagView> baseTags = findTagsByPostId(postId);
        List<Long> tagIds = baseTags.stream().map(TagView::getTagId).distinct().toList();

        if (tagIds.isEmpty()) {
            return recommendationConverter.toResponse(List.of(), List.of());
        }

        // 2) 추천 책 후보 집계 -> bookId 리스트
        var bookAgg = recommendationRepository.findSimilarBooksAgg(tagIds, base.getBookId(), 10);
        List<Long> bookIds = bookAgg.stream().map(SimilarBookAggView::getBookId).toList();

        // 3) bookIds 배치 조회 (Port 배치 조회 메서드 활용)
        Map<Long, BookView> bookMap = bookReadPort.findBooksByIds(bookIds).stream()
                .collect(Collectors.toMap(BookView::getBookId, b -> b));

        // 4) similarBooks 카드 생성 (집계 결과 순서 유지)
        var similarBookCards = bookIds.stream()
                .map(id -> bookMap.get(id))
                .filter(Objects::nonNull)
                .map(b -> recommendationConverter.toSimilarBook(b, baseTags))
                .toList();

        // 5) popularPosts
        var popularViews = recommendationRepository.findPopularPostsByTags(tagIds, postId, 10);

        var popularDtos = popularViews.stream()
                .map(recommendationConverter::toPopularPost)   // ✅ 너 converter에 이미 있음
                .toList();

        return recommendationConverter.toResponse(similarBookCards, popularDtos);
    }


    @Override
    public void validateCreateRequest(Long userId, Long bookId) {
        if (!userReadPort.existsById(userId)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }
        if (!bookReadPort.existsById(bookId)) {
            throw new GeneralException(ErrorStatus.BOOK_NOT_FOUND);
        }
    }

    // TagView 구현체
    static class SimpleTagView implements TagView {
        private final Long tagId;
        private final String name;
        private final String category;

        SimpleTagView(Long tagId, String name, String category) {
            this.tagId = tagId;
            this.name = name;
            this.category = category;
        }

        @Override public Long getTagId() { return tagId; }
        @Override public String getName() { return name; }
        @Override public String getCategory() { return category; }
    }

    @Override
    public BooklogFeedResponse assembleFeedCards(Long viewerId, Slice<BooklogPost> slice) {

        List<BooklogPost> posts = slice.getContent();

        if (posts.isEmpty()) {
            return feedConverter.toFeedResponse(List.of(), slice.hasNext());
        }

        List<Long> postIds = posts.stream().map(BooklogPost::getId).toList();

        List<Long> userIds = posts.stream().map(BooklogPost::getUserId).distinct().toList();
        List<Long> bookIds = posts.stream().map(BooklogPost::getBookId).distinct().toList();

        Map<Long, AuthorView> authorMap = findAuthorSummariesByIds(userIds).stream()
                .collect(Collectors.toMap(AuthorView::getUserId, a -> a));

        Map<Long, BookView> bookMap = findBooksByIds(bookIds).stream()
                .collect(Collectors.toMap(BookView::getBookId, b -> b));

        // 내가 북마크한 글 set
        Set<Long> bookmarkedSet = new HashSet<>(
                bookmarkRepository.findBookmarkedPostIdsByUserIdInPostIds(viewerId, postIds)
        );

        // 이미지 배치
        List<PostImageView> imageViews =
                postImageRepository.findByPostIdInOrderByPostIdAscDisplayOrderAsc(postIds);

        Map<Long, List<PostImageView>> imagesMap = imageViews.stream()
                .collect(Collectors.groupingBy(
                        PostImageView::getPostId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 태그 배치 (postId -> tagIds)
        List<BooklogPostTag> mappings = postTagRepository.findAllByPostIdIn(postIds);
        Map<Long, List<Long>> postIdToTagIds = mappings.stream()
                .collect(Collectors.groupingBy(
                        BooklogPostTag::getPostId,
                        Collectors.mapping(BooklogPostTag::getTagId, Collectors.toList())
                ));

        // 카드 조립
        List<BooklogPostCardResponse> cards = new ArrayList<>();
        for (BooklogPost p : posts) {
            Long postId = p.getId();

            AuthorView author = authorMap.get(p.getUserId());
            BookView book = bookMap.get(p.getBookId());

            List<? extends PostImageView> images = imagesMap.getOrDefault(postId, List.of());

            List<? extends TagView> tags = findTagsByTagIds(
                    postIdToTagIds.getOrDefault(postId, List.of())
            );

            boolean bookmarkedByMe = bookmarkedSet.contains(postId);

            cards.add(feedConverter.toCard(
                    postId,
                    author,
                    book,
                    p.getCreatedAt(),
                    p.getContent(),
                    p.getViewCount(),
                    p.getBookmarkCount(),
                    bookmarkedByMe,
                    images,
                    tags
            ));
        }

        return feedConverter.toFeedResponse(cards, slice.hasNext());
    }


}
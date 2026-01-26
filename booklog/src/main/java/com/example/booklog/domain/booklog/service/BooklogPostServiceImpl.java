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
            throw new IllegalArgumentException("태그는 최소 1개 이상 선택해야 합니다.");
        }

        var tags = tagsRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 태그가 포함되어 있습니다.");
        }

        long moodCount = tags.stream().filter(t -> t.getCategory() == TagCategory.MOOD).count();
        long styleCount = tags.stream().filter(t -> t.getCategory() == TagCategory.STYLE).count();
        long immersionCount = tags.stream().filter(t -> t.getCategory() == TagCategory.IMMERSION).count();

        if (moodCount < 1 || moodCount > 2) throw new IllegalArgumentException("MOOD 태그는 1~2개 선택해야 합니다.");
        if (styleCount < 1 || styleCount > 2) throw new IllegalArgumentException("STYLE 태그는 1~2개 선택해야 합니다.");
        if (immersionCount != 1) throw new IllegalArgumentException("IMMERSION 태그는 반드시 1개 선택해야 합니다.");
    }

    private void validateImageLimit(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > 8) {
            throw new IllegalArgumentException("이미지는 최대 8장까지 가능합니다.");
        }
    }

    private void savePostTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        for (Long tagId : tagIds) {
            postTagRepository.save(BooklogPostTag.of(postId, tagId));
        }
    }

    private void savePostImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        int order = 0;
        for (String url : imageUrls) {
            postImageRepository.save(BooklogPostImage.of(postId, url, order++));
        }
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

        // (1) 이미지 배치
        List<PostImageView> imageViews = postImageRepository.findByPostIdInOrderByPostIdAscDisplayOrderAsc(postIds);
        Map<Long, List<PostImageView>> imagesMap = imageViews.stream()
                .collect(Collectors.groupingBy(PostImageView::getPostId, LinkedHashMap::new, Collectors.toList()));

        // (2) 태그 배치 (postTag 매핑 배치 -> postId별 tagIds)
        List<BooklogPostTag> mappings = postTagRepository.findAllByPostIdIn(postIds);
        Map<Long, List<Long>> postIdToTagIds = mappings.stream()
                .collect(Collectors.groupingBy(BooklogPostTag::getPostId,
                        Collectors.mapping(BooklogPostTag::getTagId, Collectors.toList())));

        // (3) 북마크 카운트 배치
        Map<Long, Long> bookmarkCountMap = toCountMap(bookmarkRepository.countByPostIds(postIds));

        // 카드 조립
        List<BooklogPostCardResponse> cards = new ArrayList<>();
        for (BooklogPost p : posts) {
            Long postId = p.getId();

            AuthorView author = booklogReadFacade.findAuthorSummary(p.getUserId());
            BookView book = booklogReadFacade.findBook(p.getBookId());

            List<? extends PostImageView> images = imagesMap.getOrDefault(postId, List.of());

            // 태그는 postId -> tagIds 를 TagView로 변환해서 사용 (Facade에 위임)
            List<? extends TagView> tags = booklogReadFacade.findTagsByTagIds(
                    postIdToTagIds.getOrDefault(postId, List.of())
            );

            boolean bookmarkedByMe = booklogReadFacade.isBookmarkedByMe(userId, postId);
            long bookmarkCount = bookmarkCountMap.getOrDefault(postId, 0L);

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

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        if (rows == null) return m;
        for (Object[] r : rows) {
            Long postId = (Long) r[0];
            Long cnt = (Long) r[1];
            m.put(postId, cnt);
        }
        return m;
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
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

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
        long bookmarkCount = bookmarkRepository.countByPostId(postId);

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
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        int updated = postRepository.softDelete(
                postId,
                BooklogStatus.PUBLISHED,
                BooklogStatus.DELETED,
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new IllegalArgumentException("이미 삭제되었거나 삭제할 수 없습니다.");
        }
    }

    @Override
    @Transactional
    public BookmarkToggleResult toggleBookmark(Long userId, Long postId) {

        // 1) 게시글 존재 + PUBLISHED 확인 (삭제 글 북마크 방지)
        postRepository.findByIdAndStatus(postId, BooklogStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 2) 이미 북마크면 삭제, 아니면 생성
        boolean already = bookmarkRepository.existsByUserIdAndPostId(userId, postId);

        if (already) {
            bookmarkRepository.deleteByUserIdAndPostId(userId, postId);
        } else {
            bookmarkRepository.save(BooklogBookmark.of(userId, postId));
        }

        // 3) 최신 북마크 수 조회해서 응답
        long count = bookmarkRepository.countByPostId(postId);

        return BookmarkToggleResult.builder()
                .bookmarkedByMe(!already)
                .bookmarkCount(count)
                .build();
    }
}
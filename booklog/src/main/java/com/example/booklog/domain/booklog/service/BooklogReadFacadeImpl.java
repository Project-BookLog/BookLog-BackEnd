package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.converter.BooklogRecommendationConverter;
import com.example.booklog.domain.booklog.dto.BooklogFeedQuery;
import com.example.booklog.domain.booklog.dto.BooklogRecommendationResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.entity.BooklogStatus;
import com.example.booklog.domain.booklog.entity.BooklogPostTag;
import com.example.booklog.domain.booklog.port.BookReadPort;
import com.example.booklog.domain.booklog.port.UserReadPort;
import com.example.booklog.domain.booklog.repository.BooklogBookmarkRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostTagRepository;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.TagView;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.repository.TagsRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
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

    private final UserReadPort userReadPort;
    private final BookReadPort bookReadPort;

    @Override
    public AuthorView findAuthorSummary(Long userId) {
        return userReadPort.findAuthorSummary(userId);
    }

    @Override
    public AuthorView findAuthorDetail(Long userId, Long viewerId) {
        return userReadPort.findAuthorDetail(userId, viewerId);
    }

    @Override
    public BookView findBook(Long bookId) {
        return bookReadPort.findBook(bookId);
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

        List<Long> mood = query == null ? null : query.getMoodTagIds();
        List<Long> style = query == null ? null : query.getStyleTagIds();
        List<Long> immersion = query == null ? null : query.getImmersionTagIds();

        boolean noFilter = isEmpty(mood) && isEmpty(style) && isEmpty(immersion);
        if (noFilter) {
            return postRepository.findAllByStatusOrderByCreatedAtDesc(BooklogStatus.PUBLISHED, pageable);
        }

        Set<Long> candidate = null;

        candidate = intersect(candidate, postIdsHavingAnyTag(mood));
        candidate = intersect(candidate, postIdsHavingAnyTag(style));
        candidate = intersect(candidate, postIdsHavingAnyTag(immersion));

        if (candidate == null || candidate.isEmpty()) {
            // 빈 slice가 필요하면 pageable size 0으로 조회하는 꼼수 대신
            // Service에서 empty response 처리하는게 더 깔끔하지만,
            // 여기선 간단히 empty content slice 반환을 위해 published slice를 요청 크기 0으로 호출
            return postRepository.findAllByStatusOrderByCreatedAtDesc(BooklogStatus.PUBLISHED, Pageable.ofSize(0));
        }

        return postRepository.findAllByIdInAndStatusOrderByCreatedAtDesc(candidate, BooklogStatus.PUBLISHED, pageable);
    }

    private boolean isEmpty(List<Long> v) {
        return v == null || v.isEmpty();
    }

    private Set<Long> postIdsHavingAnyTag(List<Long> tagIds) {
        if (isEmpty(tagIds)) return null;

        List<BooklogPostTag> mappings = postTagRepository.findAllByTagIdIn(tagIds);
        if (mappings.isEmpty()) return Set.of();

        return mappings.stream().map(BooklogPostTag::getPostId).collect(Collectors.toSet());
    }

    private Set<Long> intersect(Set<Long> base, Set<Long> incoming) {
        if (incoming == null) return base; // 해당 카테고리 필터 없음
        if (base == null) return new HashSet<>(incoming);
        base.retainAll(incoming);
        return base;
    }

    @Override
    public BooklogRecommendationResponse buildRecommendations(Long postId) {
        BooklogPost base = postRepository.findByIdAndStatus(postId, BooklogStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        List<TagView> baseTags = findTagsByPostId(postId);
        List<Long> tagIds = baseTags.stream().map(TagView::getTagId).distinct().toList();

        List<BookView> similarBooks =
                bookReadPort.findSimilarBooksByTagIdsOrderByRanking(tagIds, base.getBookId(), 10);

        var similarBookCards = similarBooks.stream()
                .map(b -> recommendationConverter.toSimilarBook(b, baseTags))
                .toList();

        return recommendationConverter.toResponse(similarBookCards, List.of());
    }

    // ---- TagView 구현체 (TagView가 interface라서) ----
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
    public void validateCreateRequest(Long userId, Long bookId) {
        if (!userReadPort.existsById(userId)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId);
        }
        if (!bookReadPort.existsById(bookId)) {
            throw new IllegalArgumentException("존재하지 않는 책입니다. bookId=" + bookId);
        }
    }


}
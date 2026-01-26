package com.example.booklog.domain.booklog.controller;

import com.example.booklog.domain.booklog.dto.*;
import com.example.booklog.domain.booklog.service.BooklogPostService;
import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.repository.TagsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Booklog", description = "북로그 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booklogs")
public class BooklogPostController {

    private final BooklogPostService booklogPostService;
    private final TagsRepository tagsRepository;

    // 1) 피드 조회 (북로그 메인 페이지)
    // GET /api/v1/booklogs/feed
    @Operation(summary = "피드 조회 (북로그 메인 페이지)")
    @GetMapping("/feed")
    public ResponseEntity<BooklogFeedResponse> getFeed(
            @RequestHeader("X-USER-ID") Long userId,
            @ParameterObject BooklogFeedQuery query,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(booklogPostService.getFeed(userId, query, pageable));
    }

    // 2) 태그 드롭다운 값
    // GET /api/v1/booklogs/tags/options
    @Operation(summary = "태그 드롭다운 값 (MOOD/STYLE/IMMERSION)")
    @GetMapping("/tags/options")
    public ResponseEntity<TagOptionsResponse> getTagOptions() {
        List<Tags> all = tagsRepository.findAll();

        // 보기 좋게 category -> tag list 로 그룹핑 (이름순 정렬)
        Map<TagCategory, List<TagOptionItem>> grouped = all.stream()
                .sorted(Comparator.comparing(Tags::getName))
                .collect(Collectors.groupingBy(
                        Tags::getCategory,
                        Collectors.mapping(t -> TagOptionItem.builder()
                                .tagId(t.getId())
                                .name(t.getName())
                                .build(), Collectors.toList())
                ));

        TagOptionsResponse response = TagOptionsResponse.builder()
                .mood(grouped.getOrDefault(TagCategory.MOOD, List.of()))
                .style(grouped.getOrDefault(TagCategory.STYLE, List.of()))
                .immersion(grouped.getOrDefault(TagCategory.IMMERSION, List.of()))
                .build();
        return ResponseEntity.ok(response);
    }

    // 3) 상세보기 진입(상세 데이터 조회)
    // GET /api/v1/booklogs/{postId}
    @Operation(summary = "상세보기 진입(상세 데이터 조회)")
    @GetMapping("/{postId}")
    public ResponseEntity<BooklogDetailResponse> getDetail(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(booklogPostService.getDetail(userId, postId));
    }

    // 4) 피드 상세화면 추천 도서 목록
    // GET /api/v1/booklogs/{postId}/recommend/books
    @Operation(summary = "피드 상세화면 추천 도서 목록")
    @GetMapping("/{postId}/recommend/books")
    public ResponseEntity<List<SimilarBookCardResponse>> recommendBooks(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long postId
    ) {
        BooklogRecommendationResponse rec = booklogPostService.getRecommendations(userId, postId);
        return ResponseEntity.ok(rec.getSimilarBooks());
    }

    // 5) 피드 상세화면 추천 글 목록
    // GET /api/v1/booklogs/{postId}/recommend/posts
    @Operation(summary = "피드 상세화면 추천 글 목록")
    @GetMapping("/{postId}/recommend/posts")
    public ResponseEntity<List<SimilarTopicPostItemResponse>> recommendPosts(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long postId
    ) {
        BooklogRecommendationResponse rec = booklogPostService.getRecommendations(userId, postId);
        return ResponseEntity.ok(rec.getPopularPosts());
    }

    // 6) 작성 글 발행 (실제 게시글로 만들기)
    // POST /api/v1/booklogs
    @Operation(summary = "작성 글 발행 (실제 게시글로 만들기)")
    @PostMapping
    public ResponseEntity<BooklogPostCreateResponse> create(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid BooklogPostCreateRequest request
    ) {
        BooklogPostCreateResponse response = booklogPostService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 7) 작성글 삭제
    // DELETE /api/v1/booklogs/{postId}
    @Operation(summary = "작성글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> softDelete(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long postId
    ) {
        booklogPostService.softDelete(userId, postId);
        return ResponseEntity.noContent().build();
    }

    // 8) 북마크 토글 형식으로 관리
    // POST /api/v1/booklogs/{postId}/bookmark/toggle
    @PostMapping("/{postId}/bookmark/toggle")
    public ResponseEntity<BookmarkToggleResult> toggleBookmark(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long postId
    ) {
        BookmarkToggleResult result =
                booklogPostService.toggleBookmark(userId, postId);

        return ResponseEntity.ok(result);
    }
}
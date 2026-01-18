package com.example.booklog.domain.booklog.converter;

import com.example.booklog.domain.booklog.dto.BooklogRecommendationResponse;
import com.example.booklog.domain.booklog.dto.SimilarBookCardResponse;
import com.example.booklog.domain.booklog.dto.SimilarTopicPostItemResponse;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.PopularPostItemView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BooklogRecommendationConverter {

    private final CommonDtoConverter common;

    public BooklogRecommendationConverter(CommonDtoConverter common) {
        this.common = common;
    }

    public SimilarBookCardResponse toSimilarBook(BookView book, List<? extends TagView> tags) {
        return SimilarBookCardResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .publisher(book.getPublisher())
                .coverImageUrl(book.getCoverImageUrl())
                .tags(common.toTagChips(tags))
                .build();
    }

    public SimilarTopicPostItemResponse toPopularPost(PopularPostItemView v) {
        return SimilarTopicPostItemResponse.builder()
                .postId(v.getPostId())
                .bookTitle(v.getBookTitle())
                .excerpt(v.getExcerpt())
                .createdAt(v.getCreatedAt())
                .viewCount(v.getViewCount() == null ? 0 : v.getViewCount())
                .bookmarkCount(v.getBookmarkCount() == null ? 0 : v.getBookmarkCount())
                .thumbnailImageUrl(v.getThumbnailImageUrl())
                .build();
    }

    public BooklogRecommendationResponse toResponse(
            List<SimilarBookCardResponse> similarBooks,
            List<SimilarTopicPostItemResponse> popularPosts
    ) {
        return BooklogRecommendationResponse.builder()
                .similarBooks(similarBooks == null ? List.of() : similarBooks)
                .popularPosts(popularPosts == null ? List.of() : popularPosts)
                .build();
    }
}

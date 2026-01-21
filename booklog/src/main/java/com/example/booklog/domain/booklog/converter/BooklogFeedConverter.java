package com.example.booklog.domain.booklog.converter;

import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.booklog.dto.BooklogPostCardResponse;
import com.example.booklog.domain.booklog.dto.commonDto.PostImageResponse;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.PostImageView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BooklogFeedConverter {

    private final CommonDtoConverter common;

    public BooklogFeedConverter(CommonDtoConverter common) {
        this.common = common;
    }

    public BooklogPostCardResponse toCard(
            Long postId,
            AuthorView author,
            BookView book,
            LocalDateTime createdAt,
            String content,
            long viewCount,
            long bookmarkCount,
            boolean bookmarkedByMe,
            List<? extends PostImageView> images,
            List<? extends TagView> tags
    ) {
        return BooklogPostCardResponse.builder()
                .postId(postId)
                .author(common.toAuthorSummary(author))
                .createdAt(createdAt)
                .viewCount(viewCount)
                .bookmarkCount(bookmarkCount)
                .bookmarkedByMe(bookmarkedByMe)
                .book(common.toBookSummary(book))
                .images(common.toPostImages(images))
                .excerpt(makeExcerpt(content, 120))
                .tags(common.toTagChips(tags))
                .build();
    }

    public BooklogFeedResponse toFeedResponse(List<BooklogPostCardResponse> items, boolean hasNext) {
        return BooklogFeedResponse.builder()
                .items(items)
                .hasNext(hasNext)
                .build();
    }

    private String makeExcerpt(String content, int maxLen) {
        if (content == null) return "";
        String s = content.strip();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}

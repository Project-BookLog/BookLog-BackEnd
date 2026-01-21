package com.example.booklog.domain.booklog.converter;

import com.example.booklog.domain.booklog.dto.BooklogDetailResponse;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.PostImageView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BooklogDetailConverter {

    private final CommonDtoConverter common;

    public BooklogDetailConverter(CommonDtoConverter common) {
        this.common = common;
    }

    public BooklogDetailResponse toDetail(
            Long postId,
            AuthorView author,
            BookView book,
            String content,
            LocalDateTime createdAt,
            long viewCount,
            long bookmarkCount,
            boolean bookmarkedByMe,
            List<? extends PostImageView> images,
            List<? extends TagView> tags
    ) {
        return BooklogDetailResponse.builder()
                .postId(postId)
                .author(common.toAuthorDetail(author))
                .book(common.toBookSummary(book))
                .tags(common.toTagChips(tags))
                .content(content)
                .createdAt(createdAt)
                .viewCount(viewCount)
                .bookmarkCount(bookmarkCount)
                .bookmarkedByMe(bookmarkedByMe)
                .images(common.toPostImages(images))
                .build();
    }
}

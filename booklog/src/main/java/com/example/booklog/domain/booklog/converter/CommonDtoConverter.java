package com.example.booklog.domain.booklog.converter;


import com.example.booklog.domain.booklog.dto.commonDto.*;
import com.example.booklog.domain.booklog.entity.BooklogPostImage;
import com.example.booklog.domain.booklog.view.AuthorView;
import com.example.booklog.domain.booklog.view.BookView;
import com.example.booklog.domain.booklog.view.PostImageView;
import com.example.booklog.domain.booklog.view.TagView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommonDtoConverter {

    public AuthorSummaryResponse toAuthorSummary(AuthorView v) {
        if (v == null) return null;
        return AuthorSummaryResponse.builder()
                .userId(v.getUserId())
                .nickname(v.getNickname())
                .profileImageUrl(v.getProfileImageUrl())
                .build();
    }

    public AuthorDetailResponse toAuthorDetail(AuthorView v) {
        if (v == null) return null;
        return AuthorDetailResponse.builder()
                .userId(v.getUserId())
                .nickname(v.getNickname())
                .email(v.getEmail())
                .profileImageUrl(v.getProfileImageUrl())
                .followedByMe(Boolean.TRUE.equals(v.getFollowedByMe()))
                .build();
    }

    public BookSummaryResponse toBookSummary(BookView v) {
        if (v == null) return null;
        return BookSummaryResponse.builder()
                .bookId(v.getBookId())
                .title(v.getTitle())
                .authorName(v.getAuthorName())
                .publisher(v.getPublisher())
                .coverImageUrl(v.getCoverImageUrl())
                .build();
    }

    public TagChipResponse toTagChip(TagView v) {
        if (v == null) return null;
        return TagChipResponse.builder()
                .tagId(v.getTagId())
                .name(v.getName())
                .category(v.getCategory())
                .build();
    }

    public List<TagChipResponse> toTagChips(List<? extends TagView> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(this::toTagChip).toList();
    }

    public PostImageResponse toPostImage(PostImageView v) {
        if (v == null) return null;
        return PostImageResponse.builder()
                .imageId(v.getImageId())
                .imageUrl(v.getImageUrl())
                .order(v.getDisplayOrder() == null ? 0 : v.getDisplayOrder())
                .build();
    }


    public List<PostImageResponse> toPostImages(List<? extends PostImageView> images) {
        if (images == null) return List.of();
        return images.stream().map(this::toPostImage).toList();
    }
}

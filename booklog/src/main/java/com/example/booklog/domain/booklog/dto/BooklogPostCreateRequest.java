package com.example.booklog.domain.booklog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;


import java.util.List;

@Getter
@Builder
public class BooklogPostCreateRequest {

    @NotNull
    private Long bookId;

    private String title;

    @NotBlank
    private String content;

    private List<Long> tagIds;

    private List<String> imageUrls;
}

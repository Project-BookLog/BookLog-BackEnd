package com.example.booklog.domain.booklog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;


import java.util.List;

@Getter
public class BooklogPostCreateRequest {

    @NotNull
    private Long bookId;

    private String title;

    @NotBlank
    private String content;

    private List<Long> tagIds;

    private List<Long> imageIds;
}

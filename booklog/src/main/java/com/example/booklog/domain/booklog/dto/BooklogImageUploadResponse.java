package com.example.booklog.domain.booklog.dto;

import lombok.Builder;

@Builder
public record BooklogImageUploadResponse(
        String imageUrl
) {}
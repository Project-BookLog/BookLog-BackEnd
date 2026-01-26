package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TagOptionsResponse {
    private List<TagOptionItem> mood;
    private List<TagOptionItem> style;
    private List<TagOptionItem> immersion;
}
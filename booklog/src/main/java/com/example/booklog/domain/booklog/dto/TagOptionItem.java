package com.example.booklog.domain.booklog.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagOptionItem {
    private Long tagId;
    private String name;
}

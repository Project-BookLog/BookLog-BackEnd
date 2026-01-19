package com.example.booklog.domain.booklog.contract.tag;

public record TagInfo(
        Long tagId,
        TagCategory category,
        boolean active
) {}
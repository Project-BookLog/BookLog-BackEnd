package com.example.booklog.domain.booklog.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class BooklogFeedQuery {

    private List<Long> moodTagIds;
    private List<Long> styleTagIds;
    private List<Long> immersionTagIds;
}

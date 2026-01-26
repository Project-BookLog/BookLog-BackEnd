package com.example.booklog.domain.booklog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BooklogFeedQuery {

    private List<Long> moodTagIds;
    private List<Long> styleTagIds;
    private List<Long> immersionTagIds;
}

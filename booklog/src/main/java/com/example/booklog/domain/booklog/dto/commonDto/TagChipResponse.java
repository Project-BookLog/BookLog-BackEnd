package com.example.booklog.domain.booklog.dto.commonDto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagChipResponse {

    private Long tagId;
    private String name;      // UI 표시용(필수)
    private String category;  // "MOOD"/"STYLE"/"IMMERSION" (선택이지만 있으면 좋음)
}


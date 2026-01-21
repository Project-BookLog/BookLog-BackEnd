package com.example.booklog.domain.booklog.converter;

import com.example.booklog.domain.booklog.dto.BooklogPostCreateRequest;
import com.example.booklog.domain.booklog.dto.BooklogPostCreateResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import org.springframework.stereotype.Component;

@Component
public class BooklogPostConverter {

    public BooklogPost toEntityForCreate(Long userId, BooklogPostCreateRequest req) {
        // 엔티티 팩토리 메서드 publish 사용
        return BooklogPost.publish(userId, req.getBookId(), null, req.getContent());
        // title을 쓸 거면 req에 title 추가 후 null 대신 넣기 (선택적으로)
    }

    public BooklogPostCreateResponse toCreateResponse(Long postId) {
        return BooklogPostCreateResponse.builder()
                .postId(postId)
                .build();
    }
}

package com.example.booklog.domain.booklog.controller;

import com.example.booklog.domain.booklog.dto.BooklogFeedResponse;
import com.example.booklog.domain.booklog.service.BooklogReadFacade;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Book", description = "도서 상세 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
public class BooklogByBookController {

    private final BooklogReadFacade booklogReadFacade;

    @Operation(summary = "특정 책의 북로그 목록 조회")
    @GetMapping("/{bookId}/booklogs")
    public ResponseEntity<BooklogFeedResponse> getBooklogsByBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookId,
            @ParameterObject Pageable pageable
    ) {
        Long viewerId = userDetails.getUserId();

        // 1) 해당 bookId 기준으로 Slice 조회
        var slice = booklogReadFacade.findBookPostsSlice(bookId, pageable);

        // 2) 기존 피드 카드 조립 로직 재사용
        BooklogFeedResponse response =
                booklogReadFacade.assembleFeedCards(viewerId, slice);

        return ResponseEntity.ok(response);
    }
}

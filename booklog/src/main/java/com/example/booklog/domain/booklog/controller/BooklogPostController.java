package com.example.booklog.domain.booklog.controller;

import com.example.booklog.domain.booklog.dto.BooklogPostCreateRequest;
import com.example.booklog.domain.booklog.dto.BooklogPostCreateResponse;
import com.example.booklog.domain.booklog.service.BooklogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booklogs")
public class BooklogPostController {

    private final BooklogPostService booklogPostService;

    @PostMapping
    public ResponseEntity<BooklogPostCreateResponse> create(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid BooklogPostCreateRequest request
    ) {
        BooklogPostCreateResponse response = booklogPostService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package com.example.booklog.domain.booklog.controller;

import com.example.booklog.domain.booklog.dto.BooklogImageUploadResponse;
import com.example.booklog.domain.booklog.service.BooklogImageService;
import com.example.booklog.global.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Profile("!local")
@Tag(name = "Booklog", description = "북로그 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booklogs")
public class BooklogImageController {

    private final BooklogImageService booklogImageService;

    @Operation(summary = "북로그 이미지 업로드")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BooklogImageUploadResponse> uploadBooklogImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(booklogImageService.uploadBooklogImage(userId, file));
    }
}
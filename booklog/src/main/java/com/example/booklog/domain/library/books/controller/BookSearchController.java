package com.example.booklog.domain.library.books.controller;

import com.example.booklog.domain.library.books.dto.BookSearchResponse;
import com.example.booklog.domain.library.books.service.BookImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
public class BookSearchController {

    private final BookImportService bookImportService;

    @GetMapping("/search")
    public BookSearchResponse search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookImportService.searchAndUpsert(query, page, size);
    }
}

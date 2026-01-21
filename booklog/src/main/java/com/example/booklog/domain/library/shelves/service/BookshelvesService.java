package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookshelvesService {

    private final BookshelvesRepository bookshelvesRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public BookshelfCreateResponse create(Long userId, BookshelfCreateRequest req) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Bookshelves shelf = Bookshelves.builder()
                .user(user)
                .name(req.name())
                .isPublic(req.isPublic())
                .sortOrder(req.sortOrder())
                .build();

        bookshelvesRepository.save(shelf);
        return new BookshelfCreateResponse(shelf.getId());
    }
}

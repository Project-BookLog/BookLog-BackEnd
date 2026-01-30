package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.BookshelfItems;
import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import com.example.booklog.domain.library.shelves.entity.UserBookSort;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookshelvesService {

    private final BookshelvesRepository bookshelvesRepository;
    private final BookshelfItemsRepository bookshelfItemsRepository;
    private final UsersRepository usersRepository;

    /** 1) 서재 생성 */
    @Transactional
    public BookshelfCreateResponse create(Long userId, BookshelfCreateRequest req) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BOOK_NOT_FOUND));

        if (bookshelvesRepository.existsByUser_IdAndName(userId, req.name())) {
            throw new GeneralException(ErrorStatus.DUPLICATE_SHELF_NAME);
        }

        boolean isPublic = (req.isPublic() != null) && req.isPublic();
        UserBookSort sortOrder = (req.sortOrder() == null) ? UserBookSort.LATEST : req.sortOrder();

        Bookshelves shelf = Bookshelves.builder()
                .user(user)
                .name(req.name())
                .isPublic(isPublic)
                .sortOrder(sortOrder)
                .build();

        bookshelvesRepository.save(shelf);
        return new BookshelfCreateResponse(shelf.getId());
    }

    /** 2) 서재 목록 조회(프리뷰 3권 포함) */
    @Transactional(readOnly = true)
    public List<BookshelfListItemResponse> list(Long userId) {
        List<Bookshelves> shelves = bookshelvesRepository.findAllByUser_IdOrderByIdAsc(userId);

        return shelves.stream().map(shelf -> {
            List<BookshelfItems> items =
                    bookshelfItemsRepository.findByShelf_IdOrderByAddedAtDesc(shelf.getId());

            List<ShelfPreviewBookResponse> previewBooks = items.stream()
                    .limit(3)
                    .map(bi -> {
                        Books b = bi.getBook();

                        // ✅ 여기만 Books 엔티티에 맞게 1줄 수정!
                        String authorName = null;
                        // 예시 후보:
                        // authorName = b.getAuthorName();
                        // authorName = b.getAuthorsText();
                        // authorName = String.join(", ", b.getAuthors());

                        return new ShelfPreviewBookResponse(
                                b.getId(),
                                b.getTitle(),
                                b.getThumbnailUrl(),
                                authorName,
                                b.getPublisherName()
                        );
                    })
                    .toList();

            return new BookshelfListItemResponse(
                    shelf.getId(),
                    shelf.getName(),
                    shelf.isPublic(),
                    shelf.getSortOrder(),
                    previewBooks
            );
        }).toList();
    }

    /** 3) 서재 수정 */
    @Transactional
    public void update(Long userId, Long shelfId, BookshelfUpdateRequest req) {
        Bookshelves shelf = bookshelvesRepository.findByIdAndUser_Id(shelfId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND_OR_NOT_OWNED));

        if (req.name() != null && !req.name().isBlank()) shelf.updateName(req.name());
        if (req.isPublic() != null) shelf.updatePublic(req.isPublic());
        if (req.sortOrder() != null) shelf.updateSortOrder(req.sortOrder());
    }

    /** 4) 서재 삭제(UNASSIGN 고정) */
    @Transactional
    public void delete(Long userId, Long shelfId) {
        Bookshelves shelf = bookshelvesRepository.findByIdAndUser_Id(shelfId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND_OR_NOT_OWNED));

        // ✅ 라이브러리(user_books)는 건드리지 않음
        bookshelfItemsRepository.deleteByShelfId(shelfId);
        bookshelvesRepository.delete(shelf);
    }
}

package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.BookshelfItems;
import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBooksService {

    private final UserBooksRepository userBooksRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final BookshelvesRepository bookshelvesRepository;
    private final BookshelfItemsRepository bookshelfItemsRepository;

    /** 1) 도서 저장 /api/v1/user-books
     *  - 없으면: user_books 생성
     *  - 있으면: shelfId가 있으면 서재만 추가
     */
    @Transactional
    public UserBookCreateResponse create(Long userId, UserBookCreateRequest req) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Books book = booksRepository.findById(req.bookId())
                .orElseThrow(() -> new IllegalArgumentException("책 없음"));

        UserBooks userBook = userBooksRepository.findByUser_IdAndBook_Id(userId, req.bookId())
                .orElseGet(() -> {
                    String status = (req.status() != null) ? req.status() : "TO_READ";

                    UserBooks created = UserBooks.builder()
                            .user(user)
                            .book(book)
                            .status(status)
                            .build();

                    if ("READING".equals(status)) {
                        created.setStartDateIfNull(LocalDate.now());
                    } else if ("DONE".equals(status)) {
                        created.setStartDateIfNull(LocalDate.now());
                        created.setEndDate(LocalDate.now());
                        created.updateProgress(created.getCurrentPage(), 100);
                    }

                    return userBooksRepository.save(created);
                });

        // shelfId가 있으면 "서재에 추가"
        if (req.shelfId() != null) {
            Bookshelves shelf = bookshelvesRepository.findById(req.shelfId())
                    .orElseThrow(() -> new IllegalArgumentException("서재 없음"));

            // (권한 아직 준비 전이라 했지만, 기존 로직 유지)
            if (!shelf.getUser().getId().equals(userId)) {
                throw new IllegalStateException("내 서재가 아닙니다.");
            }

            if (!bookshelfItemsRepository.existsByShelf_IdAndBook_Id(shelf.getId(), book.getId())) {
                bookshelfItemsRepository.save(new BookshelfItems(shelf, book));
            }
        }

        return new UserBookCreateResponse(userBook.getId());
    }

    /** 3) 저장 도서 목록 조회 /api/v1/user-books (전체) */
    @Transactional(readOnly = true)
    public UserBookListResponse listAll(Long userId, Long shelfId, String status, String sort) {

        Sort s = switch (sort == null ? "LATEST" : sort) {
            case "OLDEST" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "TITLE"  -> Sort.by(Sort.Direction.ASC, "book.title");
            case "AUTHOR" -> Sort.by(Sort.Direction.ASC, "book.title"); // TODO 확장
            default       -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        long totalCount = userBooksRepository.countByFilter(userId, shelfId, status);

        List<UserBooks> result = userBooksRepository.list(userId, shelfId, status, s);

        List<UserBookListItemResponse> items = result.stream()
                .map(ub -> new UserBookListItemResponse(
                        ub.getId(),
                        ub.getStatus(),
                        ub.getProgressPercent(),
                        ub.getCurrentPage(),
                        ub.getBook().getId(),
                        ub.getBook().getTitle(),
                        ub.getBook().getThumbnailUrl(),
                        ub.getBook().getPublisherName()
                ))
                .toList();

        return new UserBookListResponse(totalCount, items);
    }

    /** 4) 저장 도서 삭제(전체/선택/서재내 전체/서재 선택 제거/상태별) /api/v1/user-books */
    @Transactional
    public int delete(Long userId, List<Long> ids, Long shelfId, String status) {

        // 1) 서재에서 선택 제거(= 라이브러리는 유지)  [shelfId + ids]
        if (shelfId != null && ids != null && !ids.isEmpty()) {
            // (권한 체크를 완전히 빼려면 아래 2줄 삭제해도 됨)
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new IllegalArgumentException("서재 없음"));
            if (!shelf.getUser().getId().equals(userId)) throw new IllegalStateException("내 서재가 아닙니다.");

            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndUserBookIds(userId, ids);
            if (bookIds.isEmpty()) return 0;

            return bookshelfItemsRepository.deleteByShelfIdAndBookIds(shelfId, bookIds);
        }

        // 2) 선택 삭제(= 라이브러리에서 완전 삭제)  [ids only]
        if (ids != null && !ids.isEmpty()) {
            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndUserBookIds(userId, ids);
            if (!bookIds.isEmpty()) {
                // ✅ 모든 서재에서 제거 (권한 없는 repo 기준)
                bookshelfItemsRepository.deleteByBookIds(bookIds);
            }
            return userBooksRepository.deleteByUserIdAndIds(userId, ids);
        }

        // 3) 서재 내 전체 삭제(= 그 서재에서만 제거. user_books는 유지)  [shelfId only]
        if (shelfId != null) {
            // (권한 체크를 완전히 빼려면 아래 2줄 삭제해도 됨)
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new IllegalArgumentException("서재 없음"));
            if (!shelf.getUser().getId().equals(userId)) throw new IllegalStateException("내 서재가 아닙니다.");

            return bookshelfItemsRepository.deleteByShelfId(shelfId);
        }

        // 4) 상태별 전체 삭제(= 라이브러리에서 완전 삭제)  [status]
        if (status != null) {
            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndStatus(userId, status);
            if (!bookIds.isEmpty()) {
                // ✅ 모든 서재에서 제거
                bookshelfItemsRepository.deleteByBookIds(bookIds);
            }
            return userBooksRepository.deleteByUserIdAndStatus(userId, status);
        }

        // 5) 전체 삭제(= 라이브러리 전체 삭제)
        // ✅ 모든 서재에서 제거하려면, 먼저 내 라이브러리 bookIds를 조회 후 매핑 제거
        List<Long> allBookIds = userBooksRepository.findAllBookIdsByUserId(userId);
        if (!allBookIds.isEmpty()) {
            bookshelfItemsRepository.deleteByBookIds(allBookIds);
        }
        return userBooksRepository.deleteAllByUserId(userId);
    }

    /** 5) 저장 도서 상세 조회 /api/v1/user-books/{userBookId} */
    @Transactional(readOnly = true)
    public UserBookDetailResponse detail(Long userId, Long userBookId) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new IllegalArgumentException("저장 도서 없음"));

        Books b = ub.getBook();

        return new UserBookDetailResponse(
                ub.getId(),
                ub.getStatus(),
                ub.getProgressPercent(),
                ub.getCurrentPage(),
                ub.getStartDate(),
                ub.getEndDate(),
                ub.getFormat(),
                ub.getPageCountSnapshot(),

                b.getId(),
                b.getTitle(),
                b.getDescription(),
                b.getThumbnailUrl(),
                b.getPublisherName(),
                b.getPublishedDate(),
                b.getDetailUrl()
        );
    }

    /** 6) 저장 도서 수정(상태 변경 + (옵션) 특정 서재에 추가) */
    @Transactional
    public void update(Long userId, Long userBookId, UserBookUpdateRequest req) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new IllegalArgumentException("저장 도서 없음"));

        if (req.status() != null) {
            ub.updateStatus(req.status());

            if ("READING".equals(req.status())) {
                ub.setStartDateIfNull(LocalDate.now());
            } else if ("DONE".equals(req.status())) {
                ub.setStartDateIfNull(LocalDate.now());
                ub.setEndDate(LocalDate.now());
                ub.updateProgress(ub.getCurrentPage(), 100);
            }
        }

        // A방식: shelfId는 "추가"
        if (req.shelfId() != null) {
            Bookshelves shelf = bookshelvesRepository.findById(req.shelfId())
                    .orElseThrow(() -> new IllegalArgumentException("서재 없음"));
            if (!shelf.getUser().getId().equals(userId)) throw new IllegalStateException("내 서재가 아닙니다.");

            Long bookId = ub.getBook().getId();
            if (!bookshelfItemsRepository.existsByShelf_IdAndBook_Id(req.shelfId(), bookId)) {
                bookshelfItemsRepository.save(new BookshelfItems(shelf, ub.getBook()));
            }
        }
    }
}

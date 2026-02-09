package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.books.entity.AuthorRole;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.*;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.library.shelves.repository.ReadingLogsRepository;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBooksService {

    private final UserBooksRepository userBooksRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final BookshelvesRepository bookshelvesRepository;
    private final BookshelfItemsRepository bookshelfItemsRepository;
    private final ReadingLogsRepository readingLogsRepository;

    /** 1) 도서 저장 /api/v1/user-books */
    @Transactional
    public UserBookCreateResponse create(Long userId, UserBookCreateRequest req) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        Books book = booksRepository.findById(req.bookId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.BOOK_NOT_FOUND));

        UserBooks userBook = userBooksRepository.findByUser_IdAndBook_Id(userId, req.bookId())
                .orElseGet(() -> {
                    ReadingStatus status = (req.status() != null) ? req.status() : ReadingStatus.TO_READ;

                    UserBooks created = UserBooks.builder()
                            .user(user)
                            .book(book)
                            .status(status)
                            .build();

                    // ✅ 기존 created.setCurrentPage(created.getCurrentPage(), 100); 컴파일 에러 → changeStatus로 대체
                    if (status == ReadingStatus.READING) {
                        created.changeStatus(ReadingStatus.READING);
                    } else if (status == ReadingStatus.COMPLETED) {
                        created.changeStatus(ReadingStatus.COMPLETED);
                    }

                    return userBooksRepository.save(created);
                });

        // shelfId가 있으면 "서재에 추가"
        if (req.shelfId() != null) {
            Bookshelves shelf = bookshelvesRepository.findById(req.shelfId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));

            if (!shelf.getUser().getId().equals(userId)) {
                throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);
            }

            if (!bookshelfItemsRepository.existsByShelf_IdAndBook_Id(shelf.getId(), book.getId())) {
                bookshelfItemsRepository.save(new BookshelfItems(shelf, book));
            }
        }

        return new UserBookCreateResponse(userBook.getId());
    }

    /** 3) 저장 도서 목록 조회 /api/v1/user-books (전체) */
    @Transactional(readOnly = true)
    public UserBookListResponse listAll(Long userId, Long shelfId, ReadingStatus status, UserBookSort sort) {

        UserBookSort sortKey = (sort == null ? UserBookSort.LATEST : sort);

        Sort s = switch (sortKey) {
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case TITLE  -> Sort.by(Sort.Direction.ASC, "book.title");
            case AUTHOR -> Sort.by(Sort.Direction.DESC, "createdAt"); // fallback
            default     -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        long totalCount = userBooksRepository.countByFilter(userId, shelfId, status);

        List<UserBooks> result = userBooksRepository.list(userId, shelfId, status, s);

        if (sortKey == UserBookSort.AUTHOR) {
            result = result.stream()
                    .sorted((a, b) -> {
                        String aName = normalizeForSort(getPrimaryAuthorName(a));
                        String bName = normalizeForSort(getPrimaryAuthorName(b));

                        if (aName == null && bName == null) return 0;
                        if (aName == null) return 1;
                        if (bName == null) return -1;

                        int cmp = aName.compareTo(bName);
                        if (cmp != 0) return cmp;

                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .toList();
        }

        List<UserBookListItemResponse> items = result.stream()
                .map(ub -> {
                    String authorName = getPrimaryAuthorName(ub);

                    return new UserBookListItemResponse(
                            ub.getId(),
                            ub.getStatus(),
                            ub.getProgressPercent(),
                            ub.getCurrentPage(),
                            ub.getBook().getId(),
                            ub.getBook().getTitle(),
                            ub.getBook().getThumbnailUrl(),
                            ub.getBook().getPublisherName(),
                            authorName
                    );
                })
                .toList();

        return new UserBookListResponse(totalCount, items);
    }

    private String getPrimaryAuthorName(UserBooks ub) {
        if (ub == null || ub.getBook() == null || ub.getBook().getBookAuthors() == null) {
            return null;
        }

        return ub.getBook().getBookAuthors().stream()
                .filter(ba -> ba.getRole() == AuthorRole.AUTHOR)
                .sorted((x, y) -> Integer.compare(x.getDisplayOrder(), y.getDisplayOrder()))
                .map(ba -> ba.getAuthor() != null ? ba.getAuthor().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String normalizeForSort(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase();
    }

    /** 4) 저장 도서 삭제 /api/v1/user-books */
    @Transactional
    public int delete(Long userId, List<Long> ids, Long shelfId, ReadingStatus status) {

        // 0) [서재에서 상태별 제거] shelfId + status
        // => bookshelf_items만 삭제. UserBooks/ReadingLogs는 유지.
        if (shelfId != null && status != null) {
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);

            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndShelfIdAndStatus(userId, shelfId, status);
            if (bookIds.isEmpty()) return 0;

            return bookshelfItemsRepository.deleteByShelfIdAndBookIds(shelfId, bookIds);
        }


        // 1) [서재에서만 제거] shelfId + ids
        // => 서재 매핑만 제거. UserBooks/ReadingLogs는 삭제하면 안 됨.
        if (shelfId != null && ids != null && !ids.isEmpty()) {
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);

            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndUserBookIds(userId, ids);
            if (bookIds.isEmpty()) return 0;

            return bookshelfItemsRepository.deleteByShelfIdAndBookIds(shelfId, bookIds);
        }

        // 2) [저장도서 완전 삭제] ids만 있는 경우
        if (ids != null && !ids.isEmpty()) {
            // ✅ 2-1) 로그 먼저 삭제 (캘린더에서 사라짐)
            readingLogsRepository.deleteByUserIdAndUserBookIds(userId, ids);

            // ✅ 2-2) 내 서재에서만 책 제거 (중요)
            List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndUserBookIds(userId, ids);
            if (!bookIds.isEmpty()) {
                bookshelfItemsRepository.deleteByUserIdAndBookIds(userId, bookIds);
            }

            // ✅ 2-3) 마지막에 user_books 삭제
            return userBooksRepository.deleteByUserIdAndIds(userId, ids);
        }

        // 3) [서재 전체 비우기] shelfId만 있는 경우
        // => 서재 매핑만 삭제. UserBooks/ReadingLogs는 유지.
        if (shelfId != null) {
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);

            return bookshelfItemsRepository.deleteByShelfId(shelfId);
        }

        // 4) [상태 기준 완전 삭제] status 있는 경우
        if (status != null) {
            // ✅ 4-1) status에 해당하는 userBookIds를 먼저 구해서 로그 삭제
            // (지금 repo에 userBookIds 조회가 없으니 간단히 하나 추가하는 걸 추천)
            // 아래는 "추가할 repo 메서드" 참고

            List<Long> userBookIds = userBooksRepository.findUserBookIdsByUserIdAndStatus(userId, status);
            if (!userBookIds.isEmpty()) {
                readingLogsRepository.deleteByUserIdAndUserBookIds(userId, userBookIds);

                List<Long> bookIds = userBooksRepository.findBookIdsByUserIdAndStatus(userId, status);
                if (!bookIds.isEmpty()) {
                    bookshelfItemsRepository.deleteByUserIdAndBookIds(userId, bookIds);
                }
            }
            return userBooksRepository.deleteByUserIdAndStatus(userId, status);
        }

        // 5) [전체 완전 삭제]
        // ✅ 전부 삭제는 reading_logs -> shelf_items -> user_books 순서
        readingLogsRepository.deleteByUserIdAndStatus(userId, null);

        List<Long> allBookIds = userBooksRepository.findAllBookIdsByUserId(userId);
        if (!allBookIds.isEmpty()) {
            bookshelfItemsRepository.deleteByUserIdAndBookIds(userId, allBookIds);
        }
        return userBooksRepository.deleteAllByUserId(userId);
    }


    /** 5) 저장 도서 상세 조회 /api/v1/user-books/{userBookId} */
    @Transactional(readOnly = true)
    public UserBookDetailResponse detail(Long userId, Long userBookId) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND));

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

    /** 6) 저장 도서 수정(상태 변경 + (옵션) 특정 서재에 추가 + 책종류 변경) */
    @Transactional
    public void update(Long userId, Long userBookId, UserBookUpdateRequest req) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND));

        // ✅ 상태 변경: 기존 수동 세팅 → 엔티티 changeStatus로 통일
        if (req.status() != null) {
            ub.changeStatus(req.status());
        }

        if (req.format() != null) {
            ub.updateFormat(req.format());
        }

        if (req.shelfId() != null) {
            Bookshelves shelf = bookshelvesRepository.findById(req.shelfId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) {
                throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);
            }

            Long bookId = ub.getBook().getId();
            if (!bookshelfItemsRepository.existsByShelf_IdAndBook_Id(req.shelfId(), bookId)) {
                bookshelfItemsRepository.save(new BookshelfItems(shelf, ub.getBook()));
            }
        }
    }

    /** 총 페이지 입력 */
    @Transactional
    public void saveTotalPage(Long userId, Long userBookId, TotalPageSaveRequest req) {
        UserBooks ub = userBooksRepository.findByUser_IdAndId(userId, userBookId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND));

        int total = req.pageCountSnapshot();
        if (total <= 0) {
            throw new GeneralException(ErrorStatus.TOTAL_PAGE_INVALID);
        }

        Integer current = ub.getCurrentPage();
        if (current != null && current > total) {
            throw new GeneralException(ErrorStatus.CURRENT_PAGE_EXCEEDS_TOTAL);
        }

        ub.setTotalPages(total);

    }

    public CurrentReadingResponse currentReading(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20); // 1~20 제한

        // 읽는 중 총 개수(홈 문구용)
        long count = userBooksRepository.countByFilter(userId, null, ReadingStatus.READING);

        // 홈 카드 리스트(limit 만큼만)
        var items = userBooksRepository.listCurrentReading(
                userId,
                PageRequest.of(0, safeLimit)
        );

        return new CurrentReadingResponse(count, items);
    }
}

package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.books.entity.AuthorRole;
import com.example.booklog.domain.library.books.entity.Books;
import com.example.booklog.domain.library.books.repository.BooksRepository;
import com.example.booklog.domain.library.shelves.dto.*;
import com.example.booklog.domain.library.shelves.entity.*;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.library.shelves.repository.UserBooksRepository;
import com.example.booklog.domain.users.entity.Users;
import com.example.booklog.domain.users.repository.UsersRepository;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
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

                    if (status == ReadingStatus.READING) {
                        created.setStartDateIfNull(LocalDate.now());
                    } else if (status == ReadingStatus.COMPLETED) {
                        created.setStartDateIfNull(LocalDate.now());
                        created.setEndDate(LocalDate.now());
                        created.updateProgress(created.getCurrentPage(), 100);
                    }

                    return userBooksRepository.save(created);
                });

        // shelfId가 있으면 "서재에 추가"
        if (req.shelfId() != null) {
            Bookshelves shelf = bookshelvesRepository.findById(req.shelfId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));

            // (권한 아직 준비 전이라 했지만, 기존 로직 유지)
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

        // ✅ sort null 방지 + 기준 통일
        UserBookSort sortKey = (sort == null ? UserBookSort.LATEST : sort);

        // ✅ AUTHOR는 DB 정렬이 아니라 "자바 정렬"로 처리할 거라서 fallback만 둠
        Sort s = switch (sortKey) {
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case TITLE  -> Sort.by(Sort.Direction.ASC, "book.title");
            case AUTHOR -> Sort.by(Sort.Direction.DESC, "createdAt"); // fallback (실제로는 아래서 자바정렬)
            default     -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        long totalCount = userBooksRepository.countByFilter(userId, shelfId, status);

        // ✅ 기본 조회 (book은 EntityGraph로 가져오는 상태)
        List<UserBooks> result = userBooksRepository.list(userId, shelfId, status, s);

        // ✅ AUTHOR일 때만 "자바에서 정렬"
        if (sortKey == UserBookSort.AUTHOR) {
            result = result.stream()
                    .sorted((a, b) -> {
                        String aName = normalizeForSort(getPrimaryAuthorName(a));
                        String bName = normalizeForSort(getPrimaryAuthorName(b));

                        // 1) authorName ASC (null은 뒤로)
                        if (aName == null && bName == null) return 0;
                        if (aName == null) return 1;
                        if (bName == null) return -1;

                        int cmp = aName.compareTo(bName);
                        if (cmp != 0) return cmp;

                        // 2) 같은 authorName이면 최신순(createdAt DESC)
                        // createdAt이 없으면 getId()로 바꿔도 됨
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

    /**
     * ✅ 대표 저자명 1개만 뽑기
     * - role=AUTHOR만
     * - displayOrder 기준 (너 DB가 1부터 들어간다 했으니까 "작은 값 우선")
     */
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

    /**
     * ✅ 정렬용 normalize
     * - 공백 제거 + 소문자 처리
     */
    private String normalizeForSort(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase();
    }


    /** 4) 저장 도서 삭제(전체/선택/서재내 전체/서재 선택 제거/상태별) /api/v1/user-books */
    @Transactional
    public int delete(Long userId, List<Long> ids, Long shelfId, ReadingStatus status) {

        // 1) 서재에서 선택 제거(= 라이브러리는 유지)  [shelfId + ids]
        if (shelfId != null && ids != null && !ids.isEmpty()) {
            // (권한 체크를 완전히 빼려면 아래 2줄 삭제해도 됨)
            Bookshelves shelf = bookshelvesRepository.findById(shelfId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) throw new GeneralException(ErrorStatus.SHELF_NOT_FOUND);

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
                    .orElseThrow(() -> new GeneralException(ErrorStatus.SHELF_NOT_FOUND));
            if (!shelf.getUser().getId().equals(userId)) throw new GeneralException(ErrorStatus.SHELF_NOT_OWNED);

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

        // 1) 상태 변경
        if (req.status() != null) {
            ub.updateStatus(req.status());

            if (req.status() == ReadingStatus.READING) {
                ub.setStartDateIfNull(LocalDate.now());
                ub.setEndDate(null); // 정책: 다시 읽기 시작하면 end_date 초기화
            } else if (req.status() == ReadingStatus.COMPLETED ) {
                ub.setStartDateIfNull(LocalDate.now());
                ub.setEndDate(LocalDate.now());
                ub.updateProgress(ub.getCurrentPage(), 100);
            } else {
                // TO_READ / STOPPED 정책
                ub.setEndDate(null);
            }
        }

        // 2) 책 종류 변경
        if (req.format() != null) {
            ub.updateFormat(req.format()); // UserBooks에 updateFormat(BookFormat) 필요
        }

        // 3) A방식: shelfId는 "추가"
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
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_BOOK_NOT_FOUND_OR_FORBIDDEN));

        ub.updatePageCountSnapshot(req.pageCountSnapshot());
    }

}

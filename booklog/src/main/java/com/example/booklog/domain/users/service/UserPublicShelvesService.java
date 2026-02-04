package com.example.booklog.domain.users.service;

import com.example.booklog.domain.library.books.entity.AuthorRole;
import com.example.booklog.domain.library.books.entity.BookAuthors;
import com.example.booklog.domain.library.shelves.entity.BookshelfItems;
import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
import com.example.booklog.domain.users.dto.UserPublicShelfListResponse;
import com.example.booklog.global.common.apiPayload.code.status.ErrorStatus;
import com.example.booklog.global.common.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPublicShelvesService {

    private final BookshelvesRepository bookshelvesRepository;
    private final BookshelfItemsRepository bookshelfItemsRepository;

    /** 정렬 enum(서비스에 남겨도 되고, dto 패키지로 빼도 됨) */
    public enum PublicShelfBookSort { LATEST, OLDEST, TITLE, AUTHOR }

    /** 다른 유저 공개 서재 목록 + 서재별 top3 프리뷰 */
    public UserPublicShelfListResponse listPublicShelves(Long userId) {

        List<Bookshelves> shelves = bookshelvesRepository.findByUser_IdAndIsPublicTrueOrderByIdAsc(userId);

        List<UserPublicShelfListResponse.UserPublicShelfItem> items = shelves.stream()
                .map(shelf -> {
                    long bookCount = bookshelfItemsRepository.countByShelf_Id(shelf.getId());

                    // ✅ top3 프리뷰
                    List<UserPublicShelfListResponse.ShelfBookPreview> top3 = bookshelfItemsRepository
                            .findTop3ByShelf_IdOrderByAddedAtDesc(shelf.getId())
                            .stream()
                            .map(this::toPreview)
                            .toList();

                    return new UserPublicShelfListResponse.UserPublicShelfItem(
                            shelf.getId(),
                            shelf.getName(),
                            (int) bookCount,
                            top3
                    );
                })
                .toList();

        return new UserPublicShelfListResponse(items.size(), items);
    }

    /** 특정 공개 서재의 전체 도서 목록(상태 없음, 정렬만) */
    public UserPublicShelfListResponse.UserPublicShelfBooksResponse listPublicShelfBooks(
            Long userId, Long shelfId, PublicShelfBookSort sort
    ) {
        // ✅ 공개 서재 검증
        boolean ok = bookshelvesRepository.existsByIdAndUser_IdAndIsPublicTrue(shelfId, userId);
        if (!ok) {
            throw new GeneralException(ErrorStatus.SHELF_NOT_FOUND);
        }

        List<BookshelfItems> rows = switch (sort) {
            case OLDEST -> bookshelfItemsRepository.findByShelf_IdOrderByAddedAtAsc(shelfId);
            case TITLE  -> bookshelfItemsRepository.findByShelf_IdOrderByBook_TitleAsc(shelfId);
            case AUTHOR -> bookshelfItemsRepository.findByShelf_IdOrderByAddedAtDesc(shelfId); // 가져온 뒤 자바 정렬
            default     -> bookshelfItemsRepository.findByShelf_IdOrderByAddedAtDesc(shelfId); // LATEST
        };

        // ✅ AUTHOR는 자바에서 정렬
        if (sort == PublicShelfBookSort.AUTHOR) {
            rows = rows.stream()
                    .sorted(Comparator
                            .comparing((BookshelfItems bi) -> normalize(getPrimaryAuthorName(bi)))
                            .thenComparing(BookshelfItems::getAddedAt, Comparator.reverseOrder())
                    )
                    .toList();
        }

        List<UserPublicShelfListResponse.UserPublicShelfBookItem> items = rows.stream()
                .map(this::toItem)
                .toList();

        return new UserPublicShelfListResponse.UserPublicShelfBooksResponse(items.size(), items);
    }

    // -----------------------
    // mapping helpers
    // -----------------------

    private UserPublicShelfListResponse.ShelfBookPreview toPreview(BookshelfItems bi) {
        var b = bi.getBook();
        return new UserPublicShelfListResponse.ShelfBookPreview(
                b.getId(),
                b.getThumbnailUrl(),
                b.getPublisherName(),
                getPrimaryAuthorName(bi)
        );
    }

    private UserPublicShelfListResponse.UserPublicShelfBookItem toItem(BookshelfItems bi) {
        var b = bi.getBook();
        return new UserPublicShelfListResponse.UserPublicShelfBookItem(
                b.getId(),
                b.getThumbnailUrl(),
                b.getPublisherName(),
                getPrimaryAuthorName(bi)
        );
    }

    /** ✅ 대표 저자명 가져오기: 엔티티 구조에 맞게 구현 */
    private String getPrimaryAuthorName(BookshelfItems bi) {
        var book = bi.getBook();
        if (book == null) return null;

        var mappings = book.getBookAuthors();
        if (mappings == null || mappings.isEmpty()) return null;

        // 1) role=AUTHOR 중 대표 1명
        String author = mappings.stream()
                .filter(m -> m.getRole() == AuthorRole.AUTHOR)
                .sorted(Comparator.comparingInt(m -> safeOrder(m.getDisplayOrder())))
                .map(BookAuthors::getAuthor)
                .map(a -> a != null ? a.getName() : null)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);

        if (author != null) return author;

        // 2) fallback: role이 이상하거나 누락된 데이터 대비
        return mappings.stream()
                .sorted(Comparator.comparingInt(m -> safeOrder(m.getDisplayOrder())))
                .map(BookAuthors::getAuthor)
                .map(a -> a != null ? a.getName() : null)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);
    }

    private int safeOrder(Integer order) {
        return order == null ? Integer.MAX_VALUE : order;
    }


    private String normalize(String s) {
        if (s == null) return "\uFFFF";
        return s.trim().toLowerCase();
    }
}

package com.example.booklog.domain.library.shelves.service;

import com.example.booklog.domain.library.shelves.entity.BookshelfItems;
import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import com.example.booklog.domain.library.shelves.repository.BookshelfItemsRepository;
import com.example.booklog.domain.library.shelves.repository.BookshelvesRepository;
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

    /** 다른 유저 공개 서재 목록 + 서재별 top3 프리뷰 */
    public PublicShelfListResponse listPublicShelves(Long userId) {

        List<Bookshelves> shelves = bookshelvesRepository.findByUser_IdAndIsPublicTrueOrderByIdAsc(userId);

        List<PublicShelfItem> items = shelves.stream()
                .map(shelf -> {
                    long bookCount = bookshelfItemsRepository.countByShelf_Id(shelf.getId());

                    // ✅ top3 프리뷰
                    List<PublicBookPreview> top3 = bookshelfItemsRepository
                            .findTop3ByShelf_IdOrderByAddedAtDesc(shelf.getId())
                            .stream()
                            .map(this::toPreview)
                            .toList();

                    return new PublicShelfItem(
                            shelf.getId(),
                            shelf.getName(),
                            (int) bookCount,
                            top3
                    );
                })
                .toList();

        return new PublicShelfListResponse(items.size(), items);
    }

    /** 특정 공개 서재의 전체 도서 목록(상태 없음, 정렬만) */
    public PublicShelfBooksResponse listPublicShelfBooks(Long userId, Long shelfId, PublicShelfBookSort sort) {

        // ✅ 공개 서재 검증
        boolean ok = bookshelvesRepository.existsByIdAndUser_IdAndIsPublicTrue(shelfId, userId);
        if (!ok) {
            // 너희 프로젝트 예외 코드로 바꿔도 됨 (SHELF_NOT_FOUND / PRIVATE 등)
            throw new IllegalArgumentException("SHELF_NOT_FOUND_OR_PRIVATE");
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

        List<PublicBookItem> items = rows.stream()
                .map(this::toItem)
                .toList();

        return new PublicShelfBooksResponse(items.size(), items);
    }

    // -----------------------
    // mapping helpers
    // -----------------------

    private PublicBookPreview toPreview(BookshelfItems bi) {
        var b = bi.getBook();
        return new PublicBookPreview(
                b.getId(),
                b.getThumbnailUrl(),
                b.getPublisherName(),
                getPrimaryAuthorName(bi)
        );
    }

    private PublicBookItem toItem(BookshelfItems bi) {
        var b = bi.getBook();
        return new PublicBookItem(
                b.getId(),
                b.getThumbnailUrl(),
                b.getPublisherName(),
                getPrimaryAuthorName(bi)
        );
    }

    /**
     * ✅ 대표 저자명 가져오기
     * - 너희 엔티티 구조에 맞게 구현해줘야 함
     * - BookAuthors 같은 매핑 엔티티가 있으면 authorOrder=1 을 대표로 쓰는 형태가 일반적
     */
    private String getPrimaryAuthorName(BookshelfItems bi) {
        var book = bi.getBook();

        // TODO: 아래 중 너희 구조에 맞는 걸로 구현
        // 예시) book.getBookAuthors()가 있으면:
        // return book.getBookAuthors().stream()
        //        .sorted(Comparator.comparingInt(BookAuthors::getAuthorOrder))
        //        .map(ba -> ba.getAuthor().getName())
        //        .findFirst().orElse(null);

        // 예시) book.getAuthors()가 List<String>이면:
        // return book.getAuthors().isEmpty() ? null : book.getAuthors().get(0);

        return null;
    }

    private String normalize(String s) {
        if (s == null) return "\uFFFF"; // null은 뒤로 보내기
        return s.trim().toLowerCase();
    }

    // -----------------------
    // Response DTOs (서비스 안에 중첩으로 두면 파일명 문제 없음)
    // -----------------------

    public enum PublicShelfBookSort { LATEST, OLDEST, TITLE, AUTHOR }

    public record PublicShelfListResponse(
            int totalCount,
            List<PublicShelfItem> items
    ) {}

    public record PublicShelfItem(
            Long shelfId,
            String name,
            int bookCount,
            List<PublicBookPreview> topBooks
    ) {}

    public record PublicBookPreview(
            Long bookId,
            String thumbnailUrl,
            String publisherName,
            String authorName
    ) {}

    public record PublicShelfBooksResponse(
            int totalCount,
            List<PublicBookItem> items
    ) {}

    public record PublicBookItem(
            Long bookId,
            String thumbnailUrl,
            String publisherName,
            String authorName
    ) {}
}

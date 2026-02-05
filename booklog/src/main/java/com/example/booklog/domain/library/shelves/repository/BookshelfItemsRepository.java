package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.entity.BookshelfItems;
import com.example.booklog.domain.library.shelves.entity.BookshelfItems.BookshelfItemId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookshelfItemsRepository extends JpaRepository<BookshelfItems, BookshelfItemId> {

    /** 같은 서재에 같은 책 중복 방지 체크 */
    boolean existsByShelf_IdAndBook_Id(Long shelfId, Long bookId);

    /** 특정 서재에 담긴 bookId 목록 */
    @Query("select bi.book.id from BookshelfItems bi where bi.shelf.id = :shelfId")
    List<Long> findBookIdsByShelfId(@Param("shelfId") Long shelfId);

    /** 특정 서재에 담긴 BookshelfItems 전체 */
    @Query("select bi from BookshelfItems bi where bi.shelf.id = :shelfId")
    List<BookshelfItems> findAllByShelfId(@Param("shelfId") Long shelfId);

    // ------------------------
    // Delete
    // ------------------------

    /** 특정 서재 전체 비우기 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.shelf.id = :shelfId")
    int deleteByShelfId(@Param("shelfId") Long shelfId);

    /** 특정 서재에서 특정 책 1권 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.shelf.id = :shelfId and bi.book.id = :bookId")
    int deleteByShelfIdAndBookId(@Param("shelfId") Long shelfId, @Param("bookId") Long bookId);

    /** 특정 서재에서 선택한 여러 권 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.shelf.id = :shelfId and bi.book.id in :bookIds")
    int deleteByShelfIdAndBookIds(@Param("shelfId") Long shelfId, @Param("bookIds") List<Long> bookIds);

    // ------------------------
    // Query (for public shelves)
    // ------------------------

    /** ✅ (추가) 서재 카드 프리뷰용: 최근 담은 책 3권만 조회 (book 즉시 로딩) */
    @EntityGraph(attributePaths = "book")
    List<BookshelfItems> findTop3ByShelf_IdOrderByAddedAtDesc(Long shelfId);

    /** ✅ (추가) 서재 카드에 "n권" 표시용: 해당 서재의 전체 도서 수 */
    long countByShelf_Id(Long shelfId);

    /** ✅ (추가) 전체보기 정렬(최신순)용: 서재의 전체 도서 목록을 최근 담은 순으로 조회 */
    @EntityGraph(attributePaths = "book")
    List<BookshelfItems> findByShelf_IdOrderByAddedAtDesc(Long shelfId);

    /** ✅ (추가) 전체보기 정렬(오래된순)용: 서재의 전체 도서 목록을 오래된 순으로 조회 */
    @EntityGraph(attributePaths = "book")
    List<BookshelfItems> findByShelf_IdOrderByAddedAtAsc(Long shelfId);

    /** ✅ (추가) 전체보기 정렬(제목순)용: 서재의 전체 도서 목록을 제목 오름차순으로 조회 */
    @EntityGraph(attributePaths = "book")
    List<BookshelfItems> findByShelf_IdOrderByBook_TitleAsc(Long shelfId);

    /** ✅ (필수) 내 서재들에서만 bookIds 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from BookshelfItems bi
        where bi.book.id in :bookIds
          and bi.shelf.user.id = :userId
    """)
    int deleteByUserIdAndBookIds(@Param("userId") Long userId,
                                 @Param("bookIds") List<Long> bookIds);

    /** ✅ 내 서재들에서만 bookId 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from BookshelfItems bi
        where bi.book.id = :bookId
          and bi.shelf.user.id = :userId
    """)
    int deleteByUserIdAndBookId(@Param("userId") Long userId,
                                @Param("bookId") Long bookId);

}

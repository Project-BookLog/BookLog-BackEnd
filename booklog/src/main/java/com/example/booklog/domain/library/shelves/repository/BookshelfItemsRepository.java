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

    /** (선택) 특정 서재에 담긴 BookshelfItems 전체 */
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

    /** ✅ (추가) 특정 서재에서 선택한 여러 권 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.shelf.id = :shelfId and bi.book.id in :bookIds")
    int deleteByShelfIdAndBookIds(@Param("shelfId") Long shelfId, @Param("bookIds") List<Long> bookIds);

    /** 특정 bookIds를 모든 서재에서 제거 (라이브러리 완전 삭제 시 사용) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.book.id in :bookIds")
    int deleteByBookIds(@Param("bookIds") List<Long> bookIds);

    /** (선택) 특정 책 1권을 모든 서재에서 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BookshelfItems bi where bi.book.id = :bookId")
    int deleteByBookId(@Param("bookId") Long bookId);
}

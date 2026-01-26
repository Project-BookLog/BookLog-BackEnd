package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.dto.UserBookListItemResponse;
import com.example.booklog.domain.library.shelves.entity.UserBooks;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserBooksRepository extends JpaRepository<UserBooks, Long> {

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    Optional<UserBooks> findByUser_IdAndId(Long userId, Long userBookId);

    @EntityGraph(attributePaths = {
            "book",
            "book.bookAuthors",
            "book.bookAuthors.author"
    })
    @Query("""
    select ub
    from UserBooks ub
    where ub.user.id = :userId
      and (:status is null or ub.status = :status)
      and (:shelfId is null or exists (
          select 1
          from BookshelfItems bi
          where bi.shelf.id = :shelfId
            and bi.book.id = ub.book.id
      ))
""")
    List<UserBooks> list(
            @Param("userId") Long userId,
            @Param("shelfId") Long shelfId,
            @Param("status") String status,
            Sort sort
    );

    // ✅ AUTHOR 정렬 전용
    // AUTHOR 정렬은 DB에서 정렬, 필요한 값만 DTO 조회
    @EntityGraph(attributePaths = {
            "book",
            "book.bookAuthors",
            "book.bookAuthors.author"
    })
    @Query("""
    select new com.example.booklog.domain.library.shelves.dto.UserBookListItemResponse(
        ub.id,
        ub.status,
        ub.progressPercent,
        ub.currentPage,

        b.id,
        b.title,
        b.thumbnailUrl,
        b.publisherName,

        a.name
    )
    from UserBooks ub
    join ub.book b

    left join b.bookAuthors ba
        on ba.role = com.example.booklog.domain.library.books.entity.AuthorRole.AUTHOR
        and ba.displayOrder = 1

    left join ba.author a

    where ub.user.id = :userId
      and (:status is null or ub.status = :status)
      and (:shelfId is null or exists (
          select 1
          from BookshelfItems bi
          where bi.shelf.id = :shelfId
            and bi.book.id = b.id
      ))
    order by
        case when a.name is null then 1 else 0 end,
        a.name asc,
        ub.createdAt desc
""")
    List<UserBookListItemResponse> listOrderByAuthorAsc(
            @Param("userId") Long userId,
            @Param("shelfId") Long shelfId,
            @Param("status") String status
    );



    @Query("""
        select ub.book.id
        from UserBooks ub
        where ub.user.id = :userId
          and ub.status = :status
    """)
    List<Long> findBookIdsByUserIdAndStatus(@Param("userId") Long userId,
                                            @Param("status") String status);

    @Query("""
        select ub.book.id
        from UserBooks ub
        where ub.user.id = :userId
          and ub.id in :userBookIds
    """)
    List<Long> findBookIdsByUserIdAndUserBookIds(@Param("userId") Long userId,
                                                 @Param("userBookIds") Collection<Long> userBookIds);

    @Modifying
    @Query("delete from UserBooks ub where ub.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserBooks ub where ub.user.id = :userId and ub.id in :ids")
    int deleteByUserIdAndIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from UserBooks ub where ub.user.id = :userId and ub.status = :status")
    int deleteByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    Optional<UserBooks> findByUser_IdAndBook_Id(Long userId, Long bookId);

    @Query("select ub.book.id from UserBooks ub where ub.user.id = :userId")
    List<Long> findAllBookIdsByUserId(@Param("userId") Long userId);

    @Query("""
    select count(ub)
    from UserBooks ub
    where ub.user.id = :userId
      and (:status is null or ub.status = :status)
      and (:shelfId is null or exists (
          select 1
          from BookshelfItems bi
          where bi.shelf.id = :shelfId
            and bi.book.id = ub.book.id
      ))
""")
    long countByFilter(
            @Param("userId") Long userId,
            @Param("shelfId") Long shelfId,
            @Param("status") String status
    );

}

package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.entity.ReadingLogs;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadingLogsRepository extends JpaRepository<ReadingLogs, Long> {

    @Query("""
        select rl
        from ReadingLogs rl
        join fetch rl.userBook ub
        join ub.user u
        where rl.id = :logId
          and u.id = :userId
    """)
    Optional<ReadingLogs> findOwned(@Param("userId") Long userId, @Param("logId") Long logId);

    List<ReadingLogs> findByUserBook_IdOrderByReadDateAscCreatedAtAsc(Long userBookId);

    Optional<ReadingLogs> findTopByUserBook_IdOrderByReadDateDescCreatedAtDesc(Long userBookId);

    /** 캘린더: 날짜별 최신 로그의 썸네일 1개 */
    interface CalendarDayThumbnailRow {
        LocalDate getReadDate();
        String getThumbnailUrl();
    }

    @Query("""
        select
            rl.readDate as readDate,
            b.thumbnailUrl as thumbnailUrl
        from ReadingLogs rl
            join rl.userBook ub
            join ub.user u
            join ub.book b
        where u.id = :userId
          and rl.readDate >= :startDate
          and rl.readDate < :endDate
          and b.thumbnailUrl is not null
          and rl.createdAt = (
              select max(rl2.createdAt)
              from ReadingLogs rl2
                  join rl2.userBook ub2
              where ub2.user.id = :userId
                and rl2.readDate = rl.readDate
          )
          and rl.id = (
              select max(rl3.id)
              from ReadingLogs rl3
                  join rl3.userBook ub3
              where ub3.user.id = :userId
                and rl3.readDate = rl.readDate
                and rl3.createdAt = rl.createdAt
          )
        order by rl.readDate asc
    """)
    List<CalendarDayThumbnailRow> findCalendarDayThumbnails(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

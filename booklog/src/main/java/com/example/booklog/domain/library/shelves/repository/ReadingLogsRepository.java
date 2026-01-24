package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.entity.ReadingLogs;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReadingLogsRepository extends JpaRepository<ReadingLogs, Long> {

    // 로그 소유권 체크(= 내 userBook의 로그인지)
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
}

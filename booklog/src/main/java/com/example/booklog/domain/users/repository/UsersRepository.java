package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.dto.MutualFriendRow;
import com.example.booklog.domain.users.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    @Query("""
        select
            u.id as userId,
            u.nickname as nickname,
            u.profileImageUrl as profileImageUrl
        from Users u
        where (:cursor is null or u.id < :cursor)
          and exists (
              select 1
              from UserFollows uf1
              where uf1.followerId = :meId
                and uf1.followeeId = u.id
          )
          and exists (
              select 1
              from UserFollows uf2
              where uf2.followerId = u.id
                and uf2.followeeId = :meId
          )
        order by u.id desc
    """)
    List<MutualFriendRow> findMutualFriendsCursor(@Param("meId") Long meId,
                                                  @Param("cursor") Long cursor,
                                                  Pageable pageable);
}

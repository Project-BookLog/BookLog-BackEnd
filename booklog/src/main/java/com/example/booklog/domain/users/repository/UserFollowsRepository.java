package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.UserFollows;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFollowsRepository extends JpaRepository<UserFollows, UserFollows.UserFollowId> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 맞팔 친구 userId 리스트 */
    @Query("""
        select uf1.followeeId
        from UserFollows uf1
        where uf1.followerId = :meId
          and exists (
              select 1
              from UserFollows uf2
              where uf2.followerId = uf1.followeeId
                and uf2.followeeId = :meId
          )
    """)
    List<Long> findMutualFriendIds(@Param("meId") Long meId);
}

package com.example.booklog.domain.users.repository;

import com.example.booklog.domain.users.entity.UserFollows;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserFollowsRepository extends JpaRepository<UserFollows, UserFollows.UserFollowId> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    Optional<UserFollows> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    @Modifying
    @Query("""
        delete from UserFollows uf
        where uf.followerId = :followerId
          and uf.followeeId = :followeeId
    """)
    int deleteByFollowerIdAndFolloweeId(@Param("followerId") Long followerId,
                                        @Param("followeeId") Long followeeId);
}

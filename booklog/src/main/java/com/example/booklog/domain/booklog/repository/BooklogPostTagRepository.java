package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogPostTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BooklogPostTagRepository extends JpaRepository<BooklogPostTag, Long> {

    // 한 피드에 모든 tag 조회
    List<BooklogPostTag> findAllByPostId(Long id);

    // N+1 문제 해결 (여러개의 피드 조회 시)
    List<BooklogPostTag> findAllByTagIdIn(Collection<Long> postIds);


    List<BooklogPostTag> findAllByPostIdIn(Collection<Long> postIds);

    // 태그 중복성 확인 (db에서 이미 제약하지만 에러 메세지 등을 고려)
    boolean existsByPostIdAndTagId(Long postId, Long tagId);

}

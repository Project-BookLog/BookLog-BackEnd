package com.example.booklog.domain.booklog.repository;

import com.example.booklog.domain.booklog.entity.BooklogPostImage;
import com.example.booklog.domain.booklog.view.PostImageView;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BooklogPostImageRepository {

    List<PostImageView> findByPostIdOrderByOrderAsc(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BooklogPostImage i where i.postId = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);


    // 피드 최적화 -> 옆으로 사진 넘길때 가져오기
    List<PostImageView> findByPostIdInOrderByPostIdAscOrderAsc(Collection<Long> postIds);

}

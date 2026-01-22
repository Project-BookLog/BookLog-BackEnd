package com.example.booklog.domain.tags.repository;

import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagsRepository extends JpaRepository<Tags, Long> {
    boolean existsByCategoryAndName(TagCategory category, String name);
}

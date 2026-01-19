package com.example.booklog.domain.booklog.contract.tag;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class FakeTagDomainClient implements TagDomainClient {

    @Override
    public List<TagInfo> findTagInfosByIds(List<Long> tagIds) {
        return tagIds.stream()
                .map(this::createFakeTagInfo)
                .toList();
    }

    private TagInfo createFakeTagInfo(Long id) {
        TagCategory category = switch ((int) (id % 3)) {
            case 1 -> TagCategory.MOOD;
            case 2 -> TagCategory.STYLE;
            default -> TagCategory.IMMERSION;
        };

        return new TagInfo(
                id,
                category,
                true // 모두 활성 태그로 가정
        );
    }
}
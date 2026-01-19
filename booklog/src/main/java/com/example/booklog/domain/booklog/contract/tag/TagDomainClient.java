package com.example.booklog.domain.booklog.contract.tag;

import java.util.List;

public interface TagDomainClient {

    List<TagInfo> findTagInfosByIds(List<Long> tagIds);
}

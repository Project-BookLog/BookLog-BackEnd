package com.example.booklog.domain.booklog.service;

import com.example.booklog.domain.booklog.contract.tag.TagCategory;
import com.example.booklog.domain.booklog.contract.tag.TagDomainClient;
import com.example.booklog.domain.booklog.contract.tag.TagInfo;
import com.example.booklog.domain.booklog.converter.BooklogPostConverter;
import com.example.booklog.domain.booklog.dto.BooklogPostCreateRequest;
import com.example.booklog.domain.booklog.dto.BooklogPostCreateResponse;
import com.example.booklog.domain.booklog.entity.BooklogPost;
import com.example.booklog.domain.booklog.repository.BooklogPostImageRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostRepository;
import com.example.booklog.domain.booklog.repository.BooklogPostTagRepository;
import com.example.booklog.domain.booklog.repository.ViewLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BooklogPostServiceImplTest {


    @InjectMocks
    private BooklogPostServiceImpl booklogPostService;

    @Mock
    private BooklogPostRepository booklogPostRepository;

    @Mock
    private BooklogPostTagRepository booklogPostTagRepository;

    @Mock
    private BooklogPostImageRepository booklogPostImageRepository;

    @Mock
    private ViewLogRepository viewLogRepository;

    @Mock
    private TagDomainClient tagDomainClient;

    @Mock
    private BooklogPostConverter postConverter;


    private BooklogPostCreateRequest createRequest() {
        return BooklogPostCreateRequest.builder()
                .bookId(1L)
                .content("테스트 북로그 내용")
                .tagIds(List.of(10L, 11L, 20L, 21L, 30L)) // MOOD 2, STYLE 2, IMMERSION 1
                .imageUrls(List.of(
                        "https://image1.jpg",
                        "https://image2.jpg"
                ))
                .build();
    }

    private List<TagInfo> validTagInfos() {
        return List.of(
                new TagInfo(10L, TagCategory.MOOD, true),
                new TagInfo(11L, TagCategory.MOOD, true),
                new TagInfo(20L, TagCategory.STYLE, true),
                new TagInfo(21L, TagCategory.STYLE, true),
                new TagInfo(30L, TagCategory.IMMERSION, true)
        );
    }




    @Test
    void create_success() {
        // given
        Long userId = 1L;
        BooklogPostCreateRequest request = createRequest();

        when(tagDomainClient.findTagInfosByIds(request.getTagIds()))
                .thenReturn(validTagInfos());

        BooklogPost entity = BooklogPost.publish(
                userId,
                request.getBookId(),
                null,
                request.getContent()
        );

        when(postConverter.toEntityForCreate(eq(userId), any(BooklogPostCreateRequest.class)))
                .thenReturn(entity);

        BooklogPost savedPost = BooklogPost.publish(
                userId,
                request.getBookId(),
                null,
                request.getContent()
        );

        ReflectionTestUtils.setField(savedPost, "id", 100L);

        when(booklogPostRepository.save(any(BooklogPost.class)))
                .thenReturn(savedPost);

        when(postConverter.toCreateResponse(100L))
                .thenReturn(
                        BooklogPostCreateResponse.builder()
                                .postId(100L)
                                .build()
                );

        // when
        BooklogPostCreateResponse response =
                booklogPostService.create(userId, request);

        // then
        assertThat(response.getPostId()).isEqualTo(100L);

        verify(booklogPostRepository).save(any(BooklogPost.class));
        verify(booklogPostTagRepository, atLeastOnce()).save(any());
        verify(booklogPostImageRepository, atLeastOnce()).save(any());
    }
}
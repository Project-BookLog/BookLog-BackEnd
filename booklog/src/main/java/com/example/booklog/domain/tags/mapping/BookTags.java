package com.example.booklog.domain.tags.mapping;

import com.example.booklog.domain.library.shelves.entity.UserBooks;
import com.example.booklog.domain.tags.entity.Tags;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "book_tags")
@IdClass(BookTags.BookTagId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookTags {

    @Id
    @Column(name = "user_book_id")
    private Long userBookId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_book_tags_user_book"))
    private UserBooks userBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_book_tags_tag"))
    private Tags tag;

    @Builder
    public BookTags(UserBooks userBook, Tags tag) {
        this.userBookId = userBook.getId();
        this.tagId = tag.getId();
        this.userBook = userBook;
        this.tag = tag;
    }

    // 복합키 클래스
    public static class BookTagId implements Serializable {
        private Long userBookId;
        private Long tagId;

        public BookTagId() {}

        public BookTagId(Long userBookId, Long tagId) {
            this.userBookId = userBookId;
            this.tagId = tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BookTagId that = (BookTagId) o;
            return Objects.equals(userBookId, that.userBookId) &&
                   Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userBookId, tagId);
        }
    }
}


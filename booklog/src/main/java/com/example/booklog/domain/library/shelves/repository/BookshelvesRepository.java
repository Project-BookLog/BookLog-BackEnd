package com.example.booklog.domain.library.shelves.repository;

import com.example.booklog.domain.library.shelves.entity.Bookshelves;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookshelvesRepository extends JpaRepository<Bookshelves, Long> {

    List<Bookshelves> findAllByUser_IdOrderByIdAsc(Long userId);

    Optional<Bookshelves> findByIdAndUser_Id(Long shelfId, Long userId);

    boolean existsByUser_IdAndName(Long userId, String name);

    /** ✅ (추가) 다른 유저 공개 서재 목록 조회 */
    List<Bookshelves> findByUser_IdAndIsPublicTrueOrderByIdAsc(Long userId);

    /** ✅ (추가) 다른 유저 공개 서재 접근 검증(소유 + 공개) */
    boolean existsByIdAndUser_IdAndIsPublicTrue(Long shelfId, Long userId);
}

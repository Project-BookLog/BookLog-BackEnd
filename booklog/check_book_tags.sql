-- 책의 태그 확인
-- bookId 4, 5, 1, 3, 6, 10, 2, 8, 131, 7

-- 1. 각 책의 태그 개수 확인
SELECT
    b.book_id,
    b.title,
    COUNT(bt.tag_id) as tag_count
FROM books b
LEFT JOIN book_tags bt ON b.book_id = bt.book_id
WHERE b.book_id IN (4, 5, 1, 3, 6, 10, 2, 8, 131, 7)
GROUP BY b.book_id, b.title
ORDER BY b.book_id;

-- 2. bookId 5와 10의 태그 상세 (이 책들은 실제 태그 값이 나옴)
SELECT
    b.book_id,
    b.title,
    t.tag_id,
    t.name as tag_name,
    t.category as tag_category
FROM books b
INNER JOIN book_tags bt ON b.book_id = bt.book_id
INNER JOIN tags t ON bt.tag_id = t.tag_id
WHERE b.book_id IN (5, 10)
ORDER BY b.book_id, t.category;

-- 3. 전체 태그 시드 데이터 확인
SELECT
    tag_id,
    name,
    category,
    display_name
FROM tags
ORDER BY category, tag_id;

-- 4. book_tags 테이블 전체 확인
SELECT COUNT(*) as total_book_tags FROM book_tags;


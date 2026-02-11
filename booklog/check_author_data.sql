-- 작가 ID 1 (김영하)의 현재 데이터 상태 확인

-- 1. Authors 테이블 확인
SELECT
    id,
    name,
    profile_image_url,
    biography,
    wikidata_id,
    profile_json,
    LENGTH(wikidata_raw_json) as wikidata_json_size,
    created_at,
    updated_at
FROM authors
WHERE id = 1;

-- 2. Books 확인 (첫 3개만)
SELECT
    b.id,
    b.title,
    b.publisher_name,
    LENGTH(b.taste_analysis) as taste_analysis_size,
    b.taste_analysis
FROM books b
INNER JOIN book_authors ba ON b.id = ba.book_id
WHERE ba.author_id = 1
LIMIT 3;

-- 3. Awards 확인
SELECT
    id,
    author_id,
    year,
    award_name,
    work_title
FROM author_awards
WHERE author_id = 1
ORDER BY year DESC;

-- 4. profile_json 컬럼 존재 여부 확인
SHOW COLUMNS FROM authors LIKE 'profile_json';


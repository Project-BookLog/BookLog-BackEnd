# 홈 화면 API 명세서

## 📌 API 개요
- **엔드포인트**: `GET /api/v1/home`
- **설명**: 홈 화면 진입 시 단 한 번 호출되며, 모든 섹션 데이터를 한 번에 반환
- **인증**: 필요 (추후 구현)
- **응답 형식**: JSON

---

## 🎯 응답 구조 (Response Schema)

```json
{
  "realTimeRanking": {
    "sectionTitle": "string",
    "rankings": [
      {
        "bookId": "number",
        "title": "string",
        "author": "string | null",
        "publisher": "string | null",
        "coverImageUrl": "string | null",
        "ranking": "number"
      }
    ]
  },
  "moodBestsellers": [
    {
      "tagName": "string",
      "books": [
        {
          "bookId": "number",
          "title": "string",
          "author": "string | null",
          "publisher": "string | null",
          "coverImageUrl": "string | null",
          "ranking": null
        }
      ]
    }
  ],
  "writingStyleBestsellers": [
    {
      "tagName": "string",
      "books": [ /* 동일한 BookSummary 구조 */ ]
    }
  ],
  "immersionBestsellers": [
    {
      "tagName": "string",
      "books": [ /* 동일한 BookSummary 구조 */ ]
    }
  ]
}
```

---

## 📦 실제 응답 예시 (PM 제공 데이터 기반)

```json
{
  "realTimeRanking": {
    "sectionTitle": "2030 인기 도서 TOP 20",
    "rankings": [
      {
        "bookId": 1,
        "title": "트렌드 코리아 2026",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 1
      },
      {
        "bookId": 2,
        "title": "비가 오면 열리는 상점",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 2
      },
      {
        "bookId": 3,
        "title": "이중 하나는 거짓말",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 3
      },
      {
        "bookId": 4,
        "title": "모순",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 4
      },
      {
        "bookId": 5,
        "title": "메리골드 마음 세탁소",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 5
      },
      {
        "bookId": 6,
        "title": "시대예보: 핵개인의 시대",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 6
      },
      {
        "bookId": 7,
        "title": "마흔에 읽는 쇼펜하우어",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 7
      },
      {
        "bookId": 8,
        "title": "불편한 편의점",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 8
      },
      {
        "bookId": 9,
        "title": "돈의 속성 (300쇄 리미티드)",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 9
      },
      {
        "bookId": 10,
        "title": "채식주의자",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 10
      },
      {
        "bookId": 11,
        "title": "나의 서투른 위로가 너에게 닿기를",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 11
      },
      {
        "bookId": 12,
        "title": "달러구트 꿈 백화점",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 12
      },
      {
        "bookId": 13,
        "title": "모든 삶은 기록을 남긴다",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 13
      },
      {
        "bookId": 14,
        "title": "데미안",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 14
      },
      {
        "bookId": 15,
        "title": "기분이 태도가 되지 않게",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 15
      },
      {
        "bookId": 16,
        "title": "작별인사",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 16
      },
      {
        "bookId": 17,
        "title": "당신도 느리게 재생할 수 있습니다",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 17
      },
      {
        "bookId": 18,
        "title": "1cm 다이빙",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 18
      },
      {
        "bookId": 19,
        "title": "초격차",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 19
      },
      {
        "bookId": 20,
        "title": "물고기는 존재하지 않는다",
        "author": null,
        "publisher": null,
        "coverImageUrl": null,
        "ranking": 20
      }
    ]
  },
  "moodBestsellers": [
    {
      "tagName": "따뜻한",
      "books": [
        {
          "bookId": 2,
          "title": "비가 오면 열리는 상점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 5,
          "title": "메리골드 마음 세탁소",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 8,
          "title": "불편한 편의점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "잔잔한",
      "books": [
        {
          "bookId": 3,
          "title": "이중 하나는 거짓말",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 4,
          "title": "모순",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 7,
          "title": "마흔에 읽는 쇼펜하우어",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "유쾌한",
      "books": [
        {
          "bookId": 1,
          "title": "트렌드 코리아 2026",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 6,
          "title": "시대예보: 핵개인의 시대",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 8,
          "title": "불편한 편의점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "어두운",
      "books": [
        {
          "bookId": 7,
          "title": "마흔에 읽는 쇼펜하우어",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 10,
          "title": "채식주의자",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 14,
          "title": "데미안",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "서늘한",
      "books": [
        {
          "bookId": 1,
          "title": "트렌드 코리아 2026",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 3,
          "title": "이중 하나는 거짓말",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 4,
          "title": "모순",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "몽환적인",
      "books": [
        {
          "bookId": 2,
          "title": "비가 오면 열리는 상점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 5,
          "title": "메리골드 마음 세탁소",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 12,
          "title": "달러구트 꿈 백화점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    }
  ],
  "writingStyleBestsellers": [
    {
      "tagName": "간결한",
      "books": [
        {
          "bookId": 1,
          "title": "트렌드 코리아 2026",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 6,
          "title": "시대예보: 핵개인의 시대",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 7,
          "title": "마흔에 읽는 쇼펜하우어",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "화려한",
      "books": [
        {
          "bookId": 12,
          "title": "달러구트 꿈 백화점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 20,
          "title": "물고기는 존재하지 않는다",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "담백한",
      "books": [
        {
          "bookId": 4,
          "title": "모순",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 5,
          "title": "메리골드 마음 세탁소",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 8,
          "title": "불편한 편의점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "섬세한",
      "books": [
        {
          "bookId": 2,
          "title": "비가 오면 열리는 상점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 3,
          "title": "이중 하나는 거짓말",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 5,
          "title": "메리골드 마음 세탁소",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "직설적",
      "books": [
        {
          "bookId": 1,
          "title": "트렌드 코리아 2026",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 6,
          "title": "시대예보: 핵개인의 시대",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 7,
          "title": "마흔에 읽는 쇼펜하우어",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "은유적",
      "books": [
        {
          "bookId": 2,
          "title": "비가 오면 열리는 상점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 3,
          "title": "이중 하나는 거짓말",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 4,
          "title": "모순",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    }
  ],
  "immersionBestsellers": [
    {
      "tagName": "가볍게 읽기 좋은",
      "books": [
        {
          "bookId": 1,
          "title": "트렌드 코리아 2026",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 9,
          "title": "돈의 속성 (300쇄 리미티드)",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 11,
          "title": "나의 서투른 위로가 너에게 닿기를",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "생각이 필요한",
      "books": [
        {
          "bookId": 4,
          "title": "모순",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 6,
          "title": "시대예보: 핵개인의 시대",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 7,
          "title": "마흔에 읽는 쇼펜하우어",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "쉽게 빠져드는",
      "books": [
        {
          "bookId": 2,
          "title": "비가 오면 열리는 상점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 5,
          "title": "메리골드 마음 세탁소",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 8,
          "title": "불편한 편의점",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    },
    {
      "tagName": "여운이 남는",
      "books": [
        {
          "bookId": 3,
          "title": "이중 하나는 거짓말",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 16,
          "title": "작별인사",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        },
        {
          "bookId": 20,
          "title": "물고기는 존재하지 않는다",
          "author": null,
          "publisher": null,
          "coverImageUrl": null,
          "ranking": null
        }
      ]
    }
  ]
}
```

---

## 🏗️ DTO 설계

### 1. BookSummary
```java
public record BookSummary(
    Long bookId,          // 서버 DB Book 테이블의 PK
    String title,         // 도서명
    String author,        // 저자 (nullable)
    String publisher,     // 출판사 (nullable)
    String coverImageUrl, // 표지 이미지 URL (nullable)
    Integer ranking       // 순위 정보 (실시간 랭킹에서만 사용, 나머지는 null)
)
```

### 2. RealTimeRankingSection
```java
public record RealTimeRankingSection(
    String sectionTitle,        // "2030 인기 도서 TOP 20"
    List<BookSummary> rankings  // TOP 20 도서 목록
)
```

### 3. TaggedBooksSection
```java
public record TaggedBooksSection(
    String tagName,           // 태그명 (예: "따뜻한", "간결한")
    List<BookSummary> books   // 해당 태그의 도서 목록 (PM 제공 데이터 전체)
)
```

### 4. HomeResponse
```java
public record HomeResponse(
    RealTimeRankingSection realTimeRanking,
    List<TaggedBooksSection> moodBestsellers,
    List<TaggedBooksSection> writingStyleBestsellers,
    List<TaggedBooksSection> immersionBestsellers
)
```

---

## 🔑 bookId 매핑 및 시드 전략

### 현재 구현 방식
- **하드코딩 방식**: PM 제공 데이터 기반으로 `Map<String, Long>` 사용
- 도서명(title) → bookId 고정 매핑 (1~20번)

### 실제 운영 시 전략

#### 방안 1: Book 테이블 title 필드 unique 제약 조건 설정
```sql
CREATE TABLE books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) UNIQUE NOT NULL,
    author VARCHAR(255),
    publisher VARCHAR(255),
    cover_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
- 장점: 간단한 구조, title로 직접 조회 가능
- 단점: 동명의 책 처리 불가 (에디션, 개정판 등)

#### 방안 2: 별도 매핑 테이블 운영
```sql
CREATE TABLE book_rankings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    ranking_type ENUM('REAL_TIME', 'MOOD', 'STYLE', 'IMMERSION'),
    tag_name VARCHAR(50),
    rank_position INT,
    FOREIGN KEY (book_id) REFERENCES books(id)
);
```
- 장점: 유연한 랭킹 관리, 이력 추적 가능
- 단점: 복잡한 쿼리 필요

#### 방안 3: 시드 데이터 적재 시 고정 ID 할당 (권장)
```java
@Component
public class BookDataSeeder {
    
    @Transactional
    public void seedBooks() {
        Map<String, Long> bookMapping = new HashMap<>();
        
        // PM 제공 데이터 기반 시드 적재
        saveBookWithId(1L, "트렌드 코리아 2026");
        saveBookWithId(2L, "비가 오면 열리는 상점");
        // ... 이하 생략
    }
    
    private void saveBookWithId(Long id, String title) {
        // Book 엔티티 저장 로직
        // 추후 카카오 API 연동으로 author, publisher 보강
    }
}
```

#### 방안 4: 카카오 API ISBN 기반 관리 (장기)
- 각 도서를 ISBN으로 관리
- 카카오 도서 검색 API를 통해 메타데이터 자동 수집
- Book 테이블에 isbn 필드 추가 및 unique 제약 조건 설정

---

## ⚠️ 예외 및 운영 고려사항

### 1. 데이터 누락 처리
**문제**: PM 제공 데이터에 저자/출판사/이미지 정보가 없음
**해결**:
- 현재: `null` 반환 (nullable 필드로 설계)
- 추후: 카카오 API 연동 후 캐싱 전략 적용
  - Redis에 도서 메타데이터 캐시
  - TTL 설정으로 주기적 갱신

### 2. 중복 도서 처리
**문제**: "불편한 편의점"이 여러 태그에 중복 등장
**해결**:
- 현재: 동일한 bookId로 여러 섹션에 포함 (정상 동작)
- 프론트엔드에서 중복 표시 처리 가능

### 3. 태그별 도서 수
**특징**: 각 태그별로 PM이 제공한 모든 도서를 반환
**처리**:
- PM 데이터에 명시된 도서만 포함 (개수 제한 없음)
- "화려한" 태그는 2개 도서만 존재 (PM 제공 데이터 그대로 반영)
- 프론트엔드는 가변 길이 배열로 처리 필요

### 4. 성능 최적화
**캐싱 전략**:
```java
@Cacheable(value = "homeData", key = "'home'")
public HomeResponse getHomeData() {
    // ...
}
```
- Spring Cache 또는 Redis 활용
- 홈 데이터는 변경 빈도가 낮으므로 TTL 1시간 권장

### 5. API 버전 관리
- 현재: `/api/v1/home`
- 향후 구조 변경 시 `/api/v2/home`으로 분리
- Deprecated 정책 명확히 전달

### 6. 에러 응답 정의
```json
{
  "timestamp": "2026-01-27T02:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "홈 데이터 조회 중 오류가 발생했습니다.",
  "path": "/api/v1/home"
}
```

---

## 📊 추후 개선 사항

1. **DB 연동**: JPA 엔티티 및 Repository 구현
2. **카카오 API 연동**: 도서 메타데이터 자동 수집
3. **캐싱 레이어**: Redis 기반 성능 최적화
4. **동적 랭킹 시스템**: 실시간 조회수/좋아요 기반 순위 갱신
5. **태그 시스템**: 태그 관리 테이블 및 다대다 관계 설정
6. **페이지네이션**: 실시간 랭킹 무한 스크롤 지원
7. **개인화**: 사용자 취향 기반 맞춤 추천 섹션 추가

---

## 🧪 테스트 방법

### cURL 예시
```bash
curl -X GET http://localhost:8080/api/v1/home \
  -H "Content-Type: application/json"
```

### 응답 검증 포인트
1. ✅ `realTimeRanking.rankings` 배열이 정확히 20개인가?
2. ✅ 각 섹션의 `tagName`이 PM 데이터와 일치하는가?
3. ✅ `bookId`가 중복 없이 올바르게 매핑되었는가?
4. ✅ `ranking` 필드가 실시간 랭킹에만 존재하는가?
5. ✅ nullable 필드들이 `null`로 반환되는가?
6. ✅ 각 태그별로 PM 제공 데이터의 모든 도서가 포함되었는가?

---

## 📝 체크리스트

- [x] DTO 설계 완료
- [x] Service 레이어 구현
- [x] Controller 구현
- [x] PM 제공 데이터 100% 반영
- [x] bookId 매핑 전략 수립
- [x] 예외 처리 정책 정의
- [ ] JPA 엔티티 설계 (추후)
- [ ] 카카오 API 연동 (추후)
- [ ] 캐싱 적용 (추후)
- [ ] 통합 테스트 작성 (추후)


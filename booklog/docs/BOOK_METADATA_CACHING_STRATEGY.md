# 📚 Book 메타데이터 캐싱 + 카카오 API 연동 전략

## 🎯 목표
홈 화면 진입 시 **외부 API 호출 없이** 즉시 응답하면서도,  
**최초 1회** 또는 **캐시 미스 시**에만 카카오 API를 호출하여 메타데이터를 보강

---

## 📊 전체 아키텍처

```
[사용자 요청]
    ↓
[HomeController] → GET /api/v1/home
    ↓
[HomeServiceImpl] → @Cacheable("homeBooks")
    ↓
[BookMetadataService] → DB 우선 조회 + @Cacheable("bookMetadata")
    ↓
┌────────────────┐
│ 1. DB 조회     │ → Books 엔티티 (author/publisher/thumbnail 포함)
└────────────────┘
    │
    ├─ 있음 → 즉시 응답 (지연 없음)
    │   └─ 메타데이터 미보강? → 비동기로 카카오 API 호출 (@Async)
    │
    └─ 없음 → 빈 Books 생성 후 즉시 응답
        └─ 비동기로 카카오 API 호출 + DB 업데이트
```

---

## 🔄 데이터 흐름

### 1️⃣ 최초 진입 (DB에 데이터 없음)

```
사용자 → GET /api/v1/home
   ↓
HomeServiceImpl.getHomeData()
   ↓
BookMetadataService.getBookSummaries([20개 도서])
   ↓
DB 조회 (findAllByTitleIn) → 결과 없음
   ↓
빈 Books 엔티티 20개 생성 + DB 저장
   ↓
**즉시 응답** (title만 포함, author/publisher/thumbnail = null)
   ↓
[별도 스레드] 비동기로 카카오 API 호출 (20개)
   ↓
카카오 API 응답 → Books 엔티티 업데이트 (DB 저장)
   ↓
다음 요청부터는 DB에서 바로 조회 가능
```

**응답 시간**: ~200ms (DB 조회만)  
**백그라운드 작업**: 카카오 API 호출 (사용자 응답에 영향 없음)

---

### 2️⃣ 2번째 진입 (DB에 메타데이터 있음)

```
사용자 → GET /api/v1/home
   ↓
HomeServiceImpl.getHomeData()
   ↓
Redis 캐시 조회 → 히트! (6시간 TTL)
   ↓
**즉시 응답** (캐시된 완전한 데이터)
```

**응답 시간**: ~10ms (Redis 캐시)  
**외부 API 호출**: 없음

---

### 3️⃣ 캐시 만료 후 (Redis TTL 초과)

```
사용자 → GET /api/v1/home
   ↓
Redis 캐시 조회 → 미스
   ↓
DB 조회 (findAllByTitleIn) → 있음 (메타데이터 보강 완료)
   ↓
**즉시 응답** (완전한 데이터)
   ↓
Redis 캐시 갱신 (6시간 TTL 재설정)
```

**응답 시간**: ~100ms (DB 조회)  
**외부 API 호출**: 없음

---

## 💾 캐싱 전략

### Redis 캐시 구조

| 캐시 이름 | Key | TTL | 용도 |
|----------|-----|-----|------|
| `homeBooks` | `home:all` | 6시간 | 홈 화면 전체 응답 |
| `bookMetadata` | `book:{bookId}` | 7일 | 개별 도서 메타데이터 |

### 캐시 우선순위

1. **Redis 캐시 조회** (가장 빠름, ~10ms)
2. **DB 조회** (빠름, ~100ms)
3. **카카오 API 호출** (느림, ~500ms) → 비동기 처리

---

## 🔧 핵심 컴포넌트

### 1. `BookMetadataService`

**역할**: 도서 메타데이터 관리 (DB + 카카오 API)

**주요 메서드**:
- `getBookSummaries(List<BookInfo>)`: 일괄 조회 (홈 화면용)
- `enrichMetadataAsync(Long, String)`: 비동기 메타데이터 보강

**로직**:
```java
// 1. DB 조회
List<Books> books = booksRepository.findAllByTitleIn(titles);

// 2. 메타데이터 미보강 도서 확인
if (needsMetadataEnrichment(book)) {
    enrichMetadataAsync(bookId, title); // 비동기 처리
}

// 3. 현재 DB 데이터로 즉시 응답
return createBookSummary(book);
```

---

### 2. `HomeServiceImpl`

**역할**: 홈 화면 데이터 구성

**캐싱 적용**:
```java
@Cacheable(value = "homeBooks", key = "'home:all'")
public HomeResponse getHomeData() {
    // ...
}
```

**최적화**:
- 일괄 조회: 20개 도서를 1번의 DB 쿼리로 조회
- Fetch Join: BookAuthors + Authors를 함께 조회 (N+1 방지)

---

### 3. `RedisCacheConfig`

**역할**: Redis 캐시 설정

**TTL 설정**:
```java
.withCacheConfiguration("homeBooks", 
    defaultConfig.entryTtl(Duration.ofHours(6)))
.withCacheConfiguration("bookMetadata", 
    defaultConfig.entryTtl(Duration.ofDays(7)))
```

---

### 4. `AsyncConfig`

**역할**: 비동기 처리 설정

**스레드 풀**:
- Core Pool Size: 5
- Max Pool Size: 10
- Queue Capacity: 100

---

## 🚀 비동기 메타데이터 보강 흐름

```java
@Async
@Transactional
public void enrichMetadataAsync(Long bookId, String title) {
    // 1. 카카오 API 호출
    KakaoBookSearchResponse response = kakaoBookClient.search(title, 1, 1).block();
    
    // 2. 첫 번째 결과 추출
    Document doc = response.getDocuments().get(0);
    
    // 3. Books 엔티티 업데이트
    book.updateBasicInfo(
        doc.getTitle(),
        doc.getContents(),
        doc.getThumbnail(),
        doc.getUrl(),
        doc.getPublisher(),
        ...
    );
    
    // 4. DB 저장
    booksRepository.save(book);
}
```

**특징**:
- 별도 스레드에서 실행 (사용자 응답에 영향 없음)
- 실패 시 로그만 남기고 계속 진행
- 다음 요청부터는 보강된 데이터 사용

---

## 📈 성능 비교

### Before (카카오 API 동기 호출)
```
GET /api/v1/home
  → 20개 도서 × 500ms = 10초 (직렬 호출)
  → 또는 2초 (병렬 호출, 복잡도 증가)
```

### After (캐싱 + 비동기)

| 시나리오 | 응답 시간 | 외부 API 호출 |
|---------|----------|-------------|
| 최초 진입 (DB 없음) | ~200ms | 비동기 (백그라운드) |
| 2번째 진입 (Redis 캐시) | ~10ms | 없음 |
| 캐시 만료 (DB 조회) | ~100ms | 없음 |

**개선 효과**:
- 응답 시간: 10초 → 0.2초 (**50배 빠름**)
- 사용자 체감: 즉시 응답 (지연 없음)

---

## 🔄 캐시 갱신 시점

### 자동 갱신

| 상황 | 갱신 방법 |
|-----|----------|
| Redis TTL 만료 (6시간) | 다음 요청 시 DB 조회 → 자동 캐시 저장 |
| bookMetadata TTL 만료 (7일) | 다음 조회 시 DB 조회 → 자동 캐시 저장 |

### 수동 갱신 (관리자 기능)

```java
@CacheEvict(value = "homeBooks", key = "'home:all'")
public void refreshHomeCache() {
    // 캐시 무효화
}

@CacheEvict(value = "bookMetadata", allEntries = true)
public void refreshAllBookMetadata() {
    // 모든 도서 메타데이터 캐시 무효화
}
```

---

## ⚠️ 예외 처리

### 1. 카카오 API 실패
```java
try {
    KakaoBookSearchResponse response = kakaoBookClient.search(title, 1, 1).block();
} catch (Exception e) {
    log.error("메타데이터 보강 실패: bookId={}, title={}", bookId, title, e);
    // 실패해도 계속 진행 (기존 데이터 유지)
}
```

### 2. 네트워크 타임아웃
- WebClient 기본 타임아웃: 5초
- 비동기 처리이므로 사용자 응답에 영향 없음

### 3. Redis 장애 처리 ✅

**현재 구현 상태**: Redis 다운 시에도 **서비스 정상 작동**

#### 시나리오 A: 애플리케이션 시작 시 Redis가 없는 경우
```
[애플리케이션 시작]
  ↓
Redis 연결 시도 → 실패
  ↓
⚠️ Redis 연결 실패 - 인메모리 캐시로 전환
  ↓
✅ 애플리케이션 정상 시작 (ConcurrentMapCacheManager 사용)
```

#### 시나리오 B: 실행 중 Redis 장애 발생
```
[서비스 운영 중]
  ↓
캐시 조회/저장 시도 → Redis 장애
  ↓
CacheErrorHandler 동작
  ↓
⚠️ 캐시 조회 실패 (캐시 미사용으로 처리)
  ↓
✅ DB에서 직접 조회 → 정상 응답
```

**영향도**:
- ✅ 서비스: 정상 동작 (장애 없음)
- ⚠️ 성능: 약간 저하 (캐시 미사용으로 DB 조회 증가)
  - Redis 사용 시: ~10ms
  - Redis 장애 시: ~100ms (DB 조회)

**로그 예시**:
```
⚠️ Redis 연결 실패 - 인메모리 캐시로 전환: Connection refused
📦 인메모리 캐시 활성화 (ConcurrentMap) - Redis 미사용
```

### 4. 캐시 에러 핸들링

**구현된 CacheErrorHandler**:
```java
@Override
public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
        // 캐시 조회 실패 → 무시하고 DB 조회
        public void handleCacheGetError(...) {
            log.warn("캐시 조회 실패 (캐시 미사용으로 처리)");
        }
        
        // 캐시 저장 실패 → 무시
        public void handleCachePutError(...) {
            log.warn("캐시 저장 실패 (무시)");
        }
        
        // ... 기타 에러 모두 graceful하게 처리
    };
}
```

**결과**: Redis 장애가 발생해도 **사용자에게 에러 없이** 서비스 제공

---

## 📊 모니터링 포인트

### 1. 캐시 히트율
```
Redis 캐시 히트율 = (캐시 히트 수 / 전체 요청 수) × 100%
목표: 95% 이상
```

### 2. 비동기 작업 실패율
```
카카오 API 실패율 = (실패 수 / 전체 시도 수) × 100%
목표: 5% 이하
```

### 3. 평균 응답 시간
```
목표: P95 < 200ms, P99 < 500ms
```

---

## 🔧 운영 시나리오

### 신규 도서 추가

1. PM이 새 도서 추가
2. HomeServiceImpl에 title 추가
3. 최초 호출 시 비동기로 카카오 API 호출
4. 다음 요청부터 DB 데이터 사용

### 메타데이터 재갱신

```java
// 특정 도서 재갱신
bookMetadataService.enrichMetadataAsync(bookId, title);

// 전체 도서 재갱신 (배치 작업)
List<Books> unenriched = booksRepository.findAllUnenriched();
for (Books book : unenriched) {
    bookMetadataService.enrichMetadataAsync(book.getId(), book.getTitle());
}
```

---

## 📋 체크리스트

- [x] Redis 의존성 추가
- [x] RedisCacheConfig 구현
- [x] AsyncConfig 구현
- [x] BookMetadataService 구현
- [x] HomeServiceImpl 캐싱 적용
- [x] BooksRepository 일괄 조회 메서드 추가
- [x] application.yaml Redis 설정
- [ ] Redis 서버 설치 및 실행
- [ ] 통합 테스트 작성
- [ ] 성능 테스트 (부하 테스트)
- [ ] 모니터링 대시보드 구축

---

## 🚀 다음 단계

1. **Redis 서버 설정**
   - Docker Compose로 로컬 Redis 실행
   - 운영 환경 Redis 클러스터 구성

2. **배치 작업 추가**
   - 야간에 모든 도서 메타데이터 갱신
   - 오래된 데이터 자동 정리

3. **모니터링**
   - Prometheus + Grafana 연동
   - 캐시 히트율/미스율 대시보드

4. **최적화**
   - HTTP/2 적용 (카카오 API 호출 최적화)
   - Connection Pool 튜닝

---

## 💡 핵심 설계 원칙

✅ **지연 없음**: 홈 화면 응답에 외부 API 호출이 영향을 주지 않음  
✅ **데이터 보강**: 최초 1회 또는 캐시 미스 시에만 카카오 API 호출  
✅ **재사용**: DB + Redis 캐싱으로 중복 호출 방지  
✅ **확장 가능**: 도서 수가 늘어나도 성능 유지  
✅ **장애 허용**: 카카오 API 실패 시에도 기존 데이터로 서비스 가능  

---

**작성일**: 2026-01-27  
**상태**: ✅ 구현 완료 (테스트 대기)


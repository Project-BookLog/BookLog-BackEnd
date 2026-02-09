- 코드 컨벤션 설정 정리 페이지

## 커밋 메시지 규칙 (Conventional Commits)

| 태그 | 설명 |
| --- | --- |
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 리팩토링 |
| style | 코드 스타일 변경 (포맷팅 등) |
| chore | 빌드, 설정 등 잡무 (배포 포함) |
| docs | 문서 수정 |
| hotfix | *주로 사용하지 않습니다* !! 배포 후 오류 수정 |

예시:

```
feat: 초기 엔티티 설계 및 DB 매핑 완료
fix: 알림 읽음 처리 로직 버그 수정
refactor: 리뷰 엔티티 BaseEntity 상속 적용
```

## PR 작성 규칙

템플릿 맞춰서 작성해주시면 됩니다!

- **PR 제목**: [태그] 간단한 설명
- **PR 설명**: #{이슈번호} 작업 내용, 관련 이슈, 스크린샷(선택)

예시:

```
[feat] 사용자 로그인 기능 추가

- #23
- JWT 로그인 로직 추가
- 이메일/비밀번호 검증
- 로그인 성공 시 토큰 발급 및 헤더 세팅
```

## 브랜치 구조

```
main ← dev ← feature/*
           ← bug/*
           ← refactor/*
```

### 브랜치 역할

| 브랜치 | 역할 |
| --- | --- |
| main | 운영(배포)용, 항상 안정 버전 |
| dev | 통합 개발, QA 및 테스트용 |
| feature/* | 신규 기능 개발 브랜치 |
| bug/* | 버그 수정 브랜치 |
| refactor/* | 리팩토링 및 QA 후 개선 |

## 디폴트 브랜치

- **dev 브랜치**를 디폴트 브랜치로 설정해 두었습니다.
- 모든 개발 브랜치는 dev에서 분기합니다.
- **PR의 기본 대상도 dev입니다!**

## 브랜치 명명 규칙

- 웬만하면 작업 내용 이슈 파놓고 진행해주세요.

```
feature/{issue-number}
bug/{issue-number}
refactor/{issue-number}
```

## 작업 흐름

1️⃣ **dev에서 브랜치 분기**

```
git checkout dev
git pull
git checkout -b feature/23
```

**2️⃣ 기능 개발 및 커밋**

```
git add .
git commit -m "feat: 사용자 로그인 기능 추가"
```

3️⃣ **dev로 PR 생성** (main으로 직접 PR ❌)

4️⃣ 타인 리뷰 & 머지 → dev

5️⃣ 배포 시 dev → main PR 생성 & QA

- 프로젝트 구조 정리 페이지
    
    ```
    com.example.booklog
     ├─ aws.s3
     │   └─ AmazonS3Manager
     ├─ domain
     │   ├─ ai
     │   ├─ book
     │   ├─ booklog
     │   │   ├─ controller
     │   │   ├─ converter
     │   │   ├─ dto
     │   │   ├─ entity
     │   │   ├─ port
     │   │   ├─ repository
     │   │   ├─ service
     │   │   └─ view
     │   ├─ home
     │   ├─ library
     │   ├─ onboarding
     │   ├─ search
     │   ├─ tags
     │   └─ users
     ├─ global
     │   ├─ auth
     │   ├─ common
     │   └─ config
     ├─ web.controller
     │   └─ RootController
     └─ BookLogApplication
    resources
     └─ application.yaml
    ```
    

    1. 주석 규칙 - 공유하는 코드 중심으로 작성
    
    ```java
    /**
     * 사용자 정보를 조회합니다.
     *@paramuserId 사용자 ID
     *@return 사용자 정보
     */
     
     // 추가적인 부가 설명
    ```
    
    1. 네이밍 컨벤션
        1. DB
            1. 테이블 : 소문자로 시작 : 스네이크 케이스 : snake_case
            2. 칼럼명 : 소문자로 시작 : 스네이크 케이스 : snake_case
        2. 상수, enum : 전체 대문자 : 스네이크 케이스 ex. TEACHER_STATUS  : UPPER_SNAKE_CASE
        3. 변수 : 소문자 : 카멜 케이스 : camelCase
        4. 클래스 : 대문자 시작 : 파스칼 케이스 : PascalCase
        5. 메소드 : 소문자 : 카멜 케이스 : camelCase
    
- PR 규칙 정리 페이지
    1. 기능마다 issue를 파서 그 안에서 브랜치 파서 PR 
    2. 코드리뷰 최소 한명

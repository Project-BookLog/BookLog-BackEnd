# Redis 설치 및 실행 가이드 (Windows)

## 🎯 가장 간단한 방법: Memurai (Redis for Windows)

### 1. Memurai 다운로드 및 설치
1. https://www.memurai.com/get-memurai 접속
2. "Download Memurai Developer" 클릭 (무료)
3. 설치 파일 실행 → Next → Install
4. 설치 완료되면 자동으로 Redis 서버가 실행됨

### 2. 서비스 확인
```powershell
# PowerShell에서 실행
Get-Service Memurai
```

**상태가 "Running"이면 성공!** ✅

---

## 🐳 Docker 사용 방법 (권장)

### 1. Docker Desktop 설치
1. https://www.docker.com/products/docker-desktop/ 접속
2. "Download for Windows" 클릭
3. 설치 후 재부팅
4. Docker Desktop 실행

### 2. Redis 컨테이너 실행
```powershell
# PowerShell에서 실행
docker run -d --name booklog-redis -p 6379:6379 redis:7-alpine
```

### 3. 확인
```powershell
docker ps
```

**booklog-redis 컨테이너가 보이면 성공!** ✅

---

## 🔧 직접 설치 방법 (MSI)

### 1. Redis for Windows 다운로드
```powershell
# PowerShell에서 실행 (관리자 권한)
winget install Redis.Redis
```

또는 수동 다운로드:
https://github.com/tporadowski/redis/releases

### 2. 서비스 시작
```powershell
# PowerShell (관리자 권한)
redis-server --service-start
```

### 3. 확인
```powershell
redis-cli ping
```

**"PONG" 응답이 오면 성공!** ✅

---

## ⚡ 빠른 테스트 (Redis 서버 연결 확인)

### PowerShell에서 실행
```powershell
# Redis 서버가 실행 중인지 확인
Test-NetConnection localhost -Port 6379
```

**TcpTestSucceeded: True 이면 성공!** ✅

---

## 🧪 Spring Boot 애플리케이션과 연결 테스트

### 1. application.yaml 확인
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 2. 애플리케이션 실행
```powershell
.\gradlew bootRun
```

### 3. 로그 확인
```
INFO ... - Lettuce connection initialized
INFO ... - Created new connection to localhost:6379
```

**위 로그가 보이면 연결 성공!** ✅

---

## 🎯 권장 방법 순서

### 1순위: Docker Desktop (가장 깔끔)
- ✅ 설치/제거가 쉬움
- ✅ 버전 관리 용이
- ✅ 개발 환경과 운영 환경 동일

### 2순위: Memurai (Windows 전용)
- ✅ Windows 서비스로 자동 실행
- ✅ 설치가 매우 간단
- ✅ Redis 명령어 100% 호환

### 3순위: 직접 설치 (MSI)
- ⚠️ 버전이 오래될 수 있음
- ⚠️ Windows 서비스 관리 필요

---

## 🔍 문제 해결

### Redis 서버가 실행되지 않을 때

#### 1. 포트 충돌 확인
```powershell
# 6379 포트 사용 확인
netstat -ano | findstr :6379
```

#### 2. 다른 포트로 실행
```powershell
# Docker
docker run -d --name booklog-redis -p 6380:6379 redis:7-alpine

# application.yaml 수정
spring:
  data:
    redis:
      port: 6380
```

#### 3. 방화벽 확인
```powershell
# 방화벽 규칙 추가 (관리자 권한)
New-NetFirewallRule -DisplayName "Redis" -Direction Inbound -Protocol TCP -LocalPort 6379 -Action Allow
```

---

## 📝 다음 단계

Redis 서버 실행 후:

```powershell
# 1. Spring Boot 애플리케이션 실행
.\gradlew bootRun

# 2. 홈 화면 API 호출
curl http://localhost:8080/api/v1/home

# 3. Redis 캐시 확인 (Redis CLI 사용)
redis-cli
> KEYS *
> GET "homeBooks::home:all"
```

---

## 💡 추천: Docker Desktop

**아직 Redis를 설치하지 않았다면 Docker Desktop을 권장합니다.**

이유:
- ✅ 1줄 명령어로 실행 가능
- ✅ 삭제도 간단 (`docker rm -f booklog-redis`)
- ✅ 여러 버전 테스트 가능
- ✅ 운영 환경과 동일한 구성


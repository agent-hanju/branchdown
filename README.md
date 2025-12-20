# Branchdown

브랜치 기반 Append-Only 트리 구조 API

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen)](https://spring.io/projects/spring-boot)

## 프로젝트 개요

특정 지점부터 **분기(Branch)** 를 생성하여 여러 흐름을 관리하는 순수 트리 구조 자료구조 API입니다.

### 핵심 개념

```
Stream
├─ Branch 0 (path: "")
│  ├─ Point 0 (branch: 0, depth: 0, root)
│  ├─ Point 1 (branch: 0, depth: 1)           "날씨 알려줘"
│  └─ Point 2 (branch: 0, depth: 2)           "서울은 맑음"
│
└─ Branch 1 (path: "0")
   ├─ Point 0 (branch: 0, depth: 0, root)     (Branch 0와 공유)
   ├─ Point 1 (branch: 0, depth: 1)           "날씨 알려줘" (Branch 0와 공유)
   └─ Point 2 (branch: 1, depth: 2)           "부산은 흐림" (분기됨)
```

**데이터 모델:**

- **Stream**: 여러 Branch를 포함하는 최상위 컨테이너
- **Branch**: 특정 시점에서 분기된 흐름 (Composite Key: streamId + branchNum)
- **Point**: 각 데이터 포인트 (depth 기반 계층 구조)

### 핵심 특징

**Append-Only 구조**: 전체 Stream 삭제 외에는 Point와 Branch 추가만 가능합니다. 개별 수정/삭제는 불가능하며, 모든 히스토리가 보존됩니다.

## 빠른 시작

### 로컬 개발 환경 (Testcontainers)

Testcontainers를 사용하여 MariaDB와 함께 로컬에서 실행합니다. Docker가 실행 중이어야 합니다.

```bash
git clone https://github.com/agent-hanju/branchdown.git
cd branchdown

# 실행 (Testcontainers로 MariaDB 자동 시작)
./gradlew bootTestRun

# 동작 확인
curl http://localhost:8083/actuator/health
open http://localhost:8083/docs
```

### 테스트 실행

```bash
# 전체 테스트 (Testcontainers 사용)
./gradlew test
```

---

## API 문서

### Swagger UI

- **URL**: http://localhost:8083/docs
- **API Docs**: http://localhost:8083/api-docs

### 주요 엔드포인트

#### Stream API

| Method | Endpoint                                        | 설명                                       |
| ------ | ----------------------------------------------- | ------------------------------------------ |
| POST   | `/api/streams`                                  | Stream 생성                                |
| GET    | `/api/streams/{id}`                             | Stream 조회                                |
| DELETE | `/api/streams/{id}`                             | Stream 삭제                                |
| GET    | `/api/streams/{id}/points`                      | 최신 Branch의 전체 Point 조회              |
| GET    | `/api/streams/{id}/branches/{branchNum}/points` | 특정 Branch의 Point 조회 (depth 지정 가능) |

#### Point API

| Method | Endpoint                     | 설명                                               |
| ------ | ---------------------------- | -------------------------------------------------- |
| POST   | `/api/points/{id}/down`      | Point 추가 (지정한 Point 아래에 추가, 브랜칭 포함) |
| GET    | `/api/points/{id}/ancestors` | 조상 Point 조회 (자신 포함, 루트 제외)             |

자세한 API 명세는 [DATABASE_DESIGN.md](DATABASE_DESIGN.md) 참조

## 프로젝트 구조

```
branchdown/
├── src/main/java/me/hanju/branchdown/
│   ├── config/              # Spring 설정
│   │   ├── GlobalExceptionHandler.java
│   │   └── ...
│   ├── controller/          # REST API 컨트롤러
│   │   ├── StreamController.java
│   │   └── PointController.java
│   ├── dto/                 # 요청/응답 DTO
│   │   ├── StreamDto.java
│   │   ├── PointDto.java
│   │   └── CommonResponseDto.java
│   ├── entity/              # JPA 엔티티
│   │   ├── StreamEntity.java
│   │   ├── BranchEntity.java
│   │   ├── PointEntity.java
│   │   └── id/
│   │       └── BranchId.java       # Composite Key
│   ├── repository/          # JPA Repository
│   │   ├── StreamRepository.java
│   │   ├── BranchRepository.java
│   │   └── PointRepository.java
│   ├── service/             # 비즈니스 로직
│   │   ├── StreamService.java
│   │   └── PointService.java
│   └── util/                # 유틸리티
│       └── PathUtils.java          # 브랜치 경로 계산
└── src/main/resources/
    ├── application.yml             # 기본 설정
    ├── application-prod.yml        # 운영 환경 설정
    └── schema.sql                  # MariaDB 스키마
```

## 환경별 설정

| 환경        | 프로파일 | 데이터베이스           |
| ----------- | -------- | ---------------------- |
| 개발/테스트 | (기본)   | Testcontainers MariaDB |
| 운영        | prod     | 외부 MariaDB           |

### 운영 환경 (prod profile)

Docker Compose 예시:

```yaml
services:
  branchdown:
    build: .
    ports:
      - '8083:8083'
      - '8084:8084'
    environment:
      MARIADB_URL: jdbc:mariadb://mariadb:3306/${MARIADB_DATABASE}
      MARIADB_USER: ${MARIADB_USER}
      MARIADB_PASSWORD: ${MARIADB_PASSWORD}
      # Consul (선택)
      CONSUL_ENABLED: false
      # CONSUL_HOST: consul
      # CONSUL_PORT: 8500
    depends_on:
      mariadb:
        condition: service_healthy

  mariadb:
    image: mariadb:10.11
    environment:
      MARIADB_ROOT_PASSWORD: ${MARIADB_ROOT_PASSWORD}
      MARIADB_DATABASE: ${MARIADB_DATABASE}
      MARIADB_USER: ${MARIADB_USER}
      MARIADB_PASSWORD: ${MARIADB_PASSWORD}
    volumes:
      - mariadb_data:/var/lib/mysql
      - ./src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    healthcheck:
      test: ['CMD', 'healthcheck.sh', '--connect', '--innodb_initialized']
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mariadb_data:
```

환경변수는 `.env` 파일로 관리 (`.env.example` 참조):

```bash
MARIADB_ROOT_PASSWORD=your_root_password
MARIADB_DATABASE=branchdown_db
MARIADB_USER=branchdown
MARIADB_PASSWORD=your_app_password
```

**운영 환경 특징:**

- Swagger UI 비활성화
- Actuator 포트 분리 (8084) 및 엔드포인트 제한
- SQL 로깅 비활성화

## 문서

- **[DATABASE_DESIGN.md](DATABASE_DESIGN.md)** - 데이터베이스 설계 (테이블 명세)
- **[Swagger UI](http://localhost:8083/docs)** - API 문서 (실행 중일 때)

---

Copyright (c) 2025 Hanju.

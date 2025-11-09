# Branchdown

브랜치 기반 Append-Only 트리 구조 데이터 관리

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)

## 프로젝트 개요

특정 지점부터 **분기(Branch)** 를 생성하여 여러 흐름을 관리하는 트리 구조 데이터 시스템입니다.

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

### Docker Compose 실행 (권장)

MariaDB와 애플리케이션을 한 번에 실행합니다.

```bash
git clone https://github.com/agent-hanju/branchdown.git
cd branchdown

# 실행
docker-compose up -d

# 동작 확인 (운영 환경 - Swagger 비활성화)
curl http://localhost:8084/actuator/health  # 관리 포트 8084
curl http://localhost:8083/api/streams      # API 포트 8083

# 종료
docker-compose down
```

### 로컬 개발 환경 (H2 In-Memory)

빠른 개발 및 테스트를 위한 로컬 실행입니다.

```bash
git clone https://github.com/agent-hanju/branchdown.git
cd branchdown

# 실행 (H2 인메모리 DB 사용)
./gradlew bootRun

# 동작 확인
curl http://localhost:8083/actuator/health
open http://localhost:8083/swagger-ui.html
```

**H2 Console**: http://localhost:8083/h2-console

- JDBC URL: `jdbc:h2:mem:branchdown`
- Username: `sa`
- Password: (비워두기)

---

## API 문서

### Swagger UI

- **URL**: http://localhost:8083/swagger-ui.html
- **API Docs**: http://localhost:8083/api-docs

### 주요 엔드포인트

#### Stream API

| Method | Endpoint                                                        | 설명                                       |
| ------ | --------------------------------------------------------------- | ------------------------------------------ |
| POST   | `/api/streams`                                                  | Stream 생성                                |
| GET    | `/api/streams/{uuid}`                                           | Stream 조회                                |
| GET    | `/api/streams?q={query}&page={page}&size={size}`                | Stream 목록 (검색, 페이징)                 |
| PATCH  | `/api/streams/{uuid}`                                           | Stream 수정                                |
| DELETE | `/api/streams/{uuid}`                                           | Stream 삭제                                |
| GET    | `/api/streams/{uuid}/points`                                    | 최신 Branch의 전체 Point 조회              |
| GET    | `/api/streams/{uuid}/branches/{branchNum}/points?depth={depth}` | 특정 Branch의 Point 조회 (depth 지정 가능) |

#### Point API

| Method | Endpoint                  | 설명                                               |
| ------ | ------------------------- | -------------------------------------------------- |
| POST   | `/api/points/{uuid}/down` | Point 추가 (지정한 Point 아래에 추가, 브랜칭 포함) |

자세한 API 명세는 [DATABASE_DESIGN.md](DATABASE_DESIGN.md) 참조

## 📖 프로젝트 구조

```
branchdown/
├── src/main/java/me/hanju/branchdown/
│   ├── config/              # Spring 설정
│   │   ├── SecurityConfig.java     # Spring Security + JWT 인증
│   │   ├── JpaConfig.java          # JPA Auditing 설정
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
│   │   ├── StreamEntity.java       # 스트림 (대화 세션)
│   │   ├── BranchEntity.java       # 브랜치 (분기)
│   │   ├── PointEntity.java        # 포인트 (메시지)
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
    ├── application.yml             # 개발 환경 설정
    └── application-prod.yml        # 운영 환경 설정
├── scripts/
    └── schema.sql                  # MariaDB 초기화 스키마 (Docker Compose용)
```

## 모니터링

### Spring Boot Actuator

**개발 환경 (로컬)**:

- **Health**: http://localhost:8083/actuator/health
- **Info**: http://localhost:8083/actuator/info
- **Metrics**: http://localhost:8083/actuator/metrics

**운영 환경 (Docker Compose)**:

- **Health**: http://localhost:8084/actuator/health (포트 8084 - 분리된 관리 포트)
- Info, Metrics 등 제한적 노출 (보안)

## 보안

### 인증/인가

**개발 환경 (로컬)**:

- JWT 인증 **비활성화** (모든 API 요청 인증 없이 사용 가능)
- 빠른 테스트 및 개발을 위해 인증 우회

**운영 환경 (Docker Compose, prod profile)**:

- JWT 기반 인증 필수 ([hanju-auth](https://github.com/agent-hanju/hanju-auth) 라이브러리)
- Spring Security 통합
- ROLE_ADMIN, ROLE_USER 권한 관리
- **JWT 토큰 발급을 위해 [hanju-authenticator](https://github.com/agent-hanju/hanju-authenticator) 서버 필요**

### 운영 환경 보안

- JWT 인증 필수 (hanju-auth validator)
- Swagger UI 비활성화
- Actuator 포트 분리 (8084) 및 엔드포인트 제한
- SQL 로깅 비활성화
- 환경변수 필수화 (JWT_SECRET_KEY, DB 정보)
- MariaDB 초기화 스크립트 분리 (scripts/schema.sql)

## 문서

- **[DATABASE_DESIGN.md](DATABASE_DESIGN.md)** - 데이터베이스 설계 (ERD, 테이블 명세)
- **[Swagger UI](http://localhost:8083/swagger-ui.html)** - API 문서 (실행 중일 때)

## 관련 프로젝트

- **[hanju-auth](https://github.com/agent-hanju/hanju-auth)** - JWT 토큰 발급 서버 및 JWT 검증 라이브러리 (운영 환경에서 필요)

---

**Copyright (c) 2025 Hanju.**

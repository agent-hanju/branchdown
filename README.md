# Branchdown

브랜치 기반 트리 구조 데이터 관리

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)

## 📦 프로젝트 개요

특정 지점부터 **분기(Branch)** 를 생성하여 여러 흐름을 관리하는 트리 구조 데이터 시스템입니다.

### 핵심 개념

```
Stream (대화 세션)
├─ Branch 0 (Main)
│  ├─ Point 0 (depth: 0, root)     "안녕하세요"
│  ├─ Point 1 (depth: 1)           "날씨 알려줘"
│  └─ Point 2 (depth: 2)           "서울은 맑음"
│
└─ Branch 1 (Alternative)
   ├─ Point 0 (depth: 0, root)     "안녕하세요" (공유)
   ├─ Point 1 (depth: 1)           "날씨 알려줘" (공유)
   └─ Point 2 (depth: 2)           "부산은 흐림" (다른 응답)
```

**데이터 모델:**
- **Stream**: 여러 브랜치를 포함하는 대화 세션
- **Branch**: 분기된 대화 가닥 (Composite Key: streamId + branchNum)
- **Point**: 각 메시지 포인트 (depth 기반 계층 구조)

## 🚀 빠른 시작

### 필수 요구사항

- **Java**: 21 이상
- **Gradle**: 8.x (Wrapper 포함)
- **Database**: MariaDB 10.6+ (개발 시 H2 사용 가능)

### 1. 프로젝트 클론 및 빌드

```bash
git clone <repository-url>
cd branchdown
chmod +x gradlew  # Linux/Mac only

# 전체 빌드
./gradlew build
```

### 2. JWT Validator 라이브러리 설치

이 프로젝트는 `me.hanju.auth:validator:1.0.0` 라이브러리를 사용합니다.

```bash
# hanju-auth 프로젝트에서 validator 빌드 및 로컬 배포
cd ../hanju-auth
./gradlew :validator:publishToMavenLocal

# 배포 확인
ls ~/.m2/repository/me/hanju/auth/validator/1.0.0/
```

### 3. 데이터베이스 설정

#### 옵션 A: MariaDB 사용 (권장)

```bash
# Docker로 MariaDB 실행
docker run -d \
  --name branchdown-mariadb \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=lex_ai \
  mariadb:10.6

# 환경변수 설정
export MARIADB_HOST=localhost
export MARIADB_PORT=3306
export MARIADB_DB=lex_ai
export MARIADB_USERNAME=root
export MARIADB_PASSWORD=rootpassword
```

#### 옵션 B: H2 인메모리 DB (테스트용)

환경변수 없이 실행하면 자동으로 H2 사용 (테스트 시에만 권장)

### 4. JWT 설정

```bash
# JWT Public Key 설정 (hanju-auth에서 생성된 키 사용)
export JWT_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----"
```

### 5. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/branchdown-1.0.0-SNAPSHOT.jar
```

### 6. 동작 확인

```bash
# Health check
curl http://localhost:8083/actuator/health

# Swagger UI
open http://localhost:8083/swagger-ui.html
```

---

## 🐳 Docker로 실행 (권장)

Docker Compose를 사용하면 MariaDB와 애플리케이션을 한 번에 실행할 수 있습니다.

### 1. .env 파일 준비

```bash
# .env.example을 복사
cp .env.example .env

# JWT Public Key 설정 (필수!)
vim .env
```

**최소 필수 설정:**
```env
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----
```

### 2. Docker Compose 실행

```bash
# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f branchdown

# 서비스 확인
docker-compose ps
```

### 3. 동작 확인

```bash
# Health check
curl http://localhost:8083/actuator/health

# Swagger UI
open http://localhost:8083/swagger-ui.html
```

### 4. 종료

```bash
# 서비스 중지
docker-compose down

# 데이터까지 삭제 (주의!)
docker-compose down -v
```

### Docker Compose 구성

- **MariaDB**: 3306 포트, 데이터는 Volume에 영속화
- **Branchdown**: 8083 (API), 8084 (Actuator)
- **Network**: 전용 브릿지 네트워크
- **Volumes**:
  - `mariadb_data`: 데이터베이스 데이터
  - `branchdown_logs`: 애플리케이션 로그

### Dockerfile 특징

- **Multi-stage Build**: Builder stage (JDK) + Runtime stage (JRE)
- **보안**: Non-root 사용자 (`branchdown`) 실행
- **최적화**: Layer 캐싱, JVM 컨테이너 최적화
- **이미지 크기**: ~250MB (JRE only)

## 📚 API 문서

### Swagger UI

- **URL**: http://localhost:8083/swagger-ui.html
- **API Docs**: http://localhost:8083/api-docs

### 주요 엔드포인트

#### Stream API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/streams` | 스트림 생성 |
| GET | `/api/streams/{uuid}` | 스트림 조회 |
| GET | `/api/streams` | 스트림 목록 조회 (페이징) |
| PATCH | `/api/streams/{uuid}` | 스트림 수정 |
| DELETE | `/api/streams/{uuid}` | 스트림 삭제 |
| GET | `/api/streams/{uuid}/points` | 스트림 전체 포인트 조회 |
| GET | `/api/streams/{uuid}/branches/{branchNum}/points` | 특정 브랜치 포인트 조회 |

#### Point API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/points` | 포인트 생성 (메시지 추가) |

자세한 API 명세는 [DATABASE_DESIGN.md](DATABASE_DESIGN.md) 참조

## 🔨 주요 명령어

### 빌드

```bash
# 전체 빌드
./gradlew build

# 클린 빌드
./gradlew clean build

# JAR 생성 (실행 가능)
./gradlew bootJar
```

### 테스트

```bash
# 전체 테스트
./gradlew test

# 통합 테스트만 실행
./gradlew test --tests "*IntegrationTest"

# 특정 테스트 클래스
./gradlew test --tests "StreamServiceIntegrationTest"

# 테스트 리포트 확인
# build/reports/tests/test/index.html
```

### 실행

```bash
# Gradle로 실행 (개발 모드)
./gradlew bootRun

# JAR로 실행 (프로덕션)
java -jar build/libs/branchdown-1.0.0-SNAPSHOT.jar

# 프로필 지정
java -jar build/libs/branchdown-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔨 Docker 이미지 빌드

### 로컬 이미지 빌드

```bash
# 이미지 빌드
docker build -t branchdown:latest .

# 빌드 확인
docker images | grep branchdown

# 단독 실행 (개발용)
docker run -d \
  -p 8083:8083 \
  -e MARIADB_HOST=host.docker.internal \
  -e JWT_PUBLIC_KEY="..." \
  --name branchdown \
  branchdown:latest
```

## 🛠️ 개발 환경 설정

### Java 21 설치 (SDKMAN 권장)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk use java 21-tem
```

### IDE 설정

#### IntelliJ IDEA

1. `File > Open` - build.gradle 선택
2. `File > Project Structure > Project SDK` - Java 21 선택
3. Lombok 플러그인 설치
4. `Preferences > Build > Compiler > Annotation Processors` - Enable

#### VS Code

확장 설치:
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support

## 🔧 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6.3.3 + JWT Validator
- **Database**: MariaDB 10.6+ (H2 for testing)
- **ORM**: Spring Data JPA / Hibernate
- **Build**: Gradle 8.x
- **Documentation**: Swagger/OpenAPI 3.0
- **Testing**: JUnit 5, Mockito
- **Monitoring**: Spring Boot Actuator

## 📖 프로젝트 구조

```
branchdown/
├── src/main/java/me/hanju/branchdown/
│   ├── config/              # Spring 설정
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
```

## 🔑 핵심 설계

### 1. Composite Key 패턴 (BranchEntity)

```java
@Embeddable
public class BranchId {
    private Long streamId;    // 스트림 ID
    private Integer branchNum; // 브랜치 번호
}
```

**이유**: Branch는 Stream 내에서만 고유하므로 (streamId + branchNum) 조합을 PK로 사용

### 2. Path-based Query

```java
// BranchEntity
private String path = "0,1,5"; // 브랜치 0 → 1 → 5 경로

// PointRepository
@Query("... WHERE (p.depth, p.branch_num) IN :branchNums AND p.depth > :depth ...")
List<PointEntity> findAllUsingPath(...);
```

**이유**: 특정 브랜치까지의 모든 메시지를 효율적으로 조회

### 3. Depth-based Filtering

```java
// depth > :depth 의미: "이미 받은 메시지(depth) 이후부터"
streamService.getStreamPoints(uuid); // depth=-1 (처음부터)
streamService.getBranchMessages(uuid, branchNum, depth=5); // depth 5 이후부터
```

**이유**: 증분 로딩 지원 (이미 받은 메시지는 다시 안 받음)

### 4. Read-Only Lazy Field (PointEntity.stream)

```java
@Getter(AccessLevel.PRIVATE) // Java 코드에서 접근 금지
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(..., insertable = false, updatable = false) // 읽기 전용
private StreamEntity stream; // JPQL 쿼리 전용
```

**이유**: JPQL에서는 작동하지만 Java getter는 H2 테스트 환경에서 프록시 초기화 실패

**해결**: `point.getBranch().getStream()` 사용 (안전한 경로)

## 🐛 트러블슈팅

### Gradle 캐시 문제

```bash
./gradlew clean --refresh-dependencies
./gradlew --stop
./gradlew build
```

### JWT Validator를 찾을 수 없음

```bash
# 로컬 Maven 저장소 확인
ls ~/.m2/repository/me/hanju/auth/validator/1.0.0/

# 재배포 (hanju-auth 프로젝트에서)
cd ../hanju-auth
./gradlew :validator:publishToMavenLocal
```

### 테스트 실패: "Named parameter not bound"

JPQL 쿼리의 `:placeholder`와 `@Param("name")`이 일치하는지 확인하세요.

```java
// WRONG
@Query("SELECT p FROM PointEntity p WHERE p.branch = :branch")
List<PointEntity> find(@Param("branchEntity") BranchEntity branch);

// CORRECT
@Query("SELECT p FROM PointEntity p WHERE p.branch = :branch")
List<PointEntity> find(@Param("branch") BranchEntity branch);
```

### 데이터베이스 연결 실패

```bash
# MariaDB가 실행 중인지 확인
docker ps | grep mariadb

# 환경변수 확인
echo $MARIADB_HOST
echo $MARIADB_USERNAME

# 연결 테스트
mysql -h localhost -u root -p
```

## 📊 모니터링

### Spring Boot Actuator

- **Health**: http://localhost:8083/actuator/health
- **Info**: http://localhost:8083/actuator/info
- **Metrics**: http://localhost:8083/actuator/metrics

운영 환경에서는 health만 공개됩니다 (보안).

## 🔒 보안

### 인증/인가

- JWT 기반 인증 (`me.hanju.auth:validator` 라이브러리)
- Spring Security 통합
- ROLE_ADMIN, ROLE_USER 권한 관리

### 운영 환경 보안

- Swagger UI 비활성화
- Actuator 엔드포인트 제한 (health만 공개)
- SQL 로깅 비활성화
- 환경변수 필수화 (JWT_PUBLIC_KEY, DB 정보)

## 📝 문서

- **[DATABASE_DESIGN.md](DATABASE_DESIGN.md)** - 데이터베이스 설계 (ERD, 테이블 명세)
- **[Swagger UI](http://localhost:8083/swagger-ui.html)** - API 문서 (실행 중일 때)

## 🤝 관련 프로젝트

- **[hanju-auth](../hanju-auth)** - JWT 인증/인가 라이브러리

---

**Copyright (c) 2025 Hanju.**

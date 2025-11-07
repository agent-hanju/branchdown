# Database Design

Branchdown 프로젝트의 데이터베이스 설계 문서

## 📊 ERD (Entity Relationship Diagram)

```
┌─────────────────────────┐
│      StreamEntity       │
├─────────────────────────┤
│ PK: stream_id (BIGINT)  │
│     uuid (UUID)         │
│     title (VARCHAR 64)  │
│     next_branch_num     │
│     created_by          │
│     created_at          │
│     updated_by          │
│     updated_at          │
└────────────┬────────────┘
             │ 1:N
             │
             ↓
┌─────────────────────────┐
│     BranchEntity        │
├─────────────────────────┤
│ PK: (stream_id, branch_num) ← Composite Key
│     path (VARCHAR 500)  │
└────────────┬────────────┘
             │ 1:N
             │
             ↓
┌─────────────────────────┐
│      PointEntity        │
├─────────────────────────┤
│ PK: point_id (BIGINT)   │
│ FK: stream_id           │
│ FK: branch_num          │
│     uuid (UUID)         │
│     item_id (VARCHAR)   │
│     depth (INT)         │
│     child_branch_nums   │
│     created_by          │
│     created_at          │
└─────────────────────────┘
```

## 📋 테이블 명세

### 1. streams 테이블

여러 브랜치를 포함하는 최상위 컨테이너

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `stream_id` | BIGINT | PK, AUTO_INCREMENT | 내부 ID (데이터베이스 전용) |
| `uuid` | UUID | UNIQUE, NOT NULL | 공개 UUID (외부 API 노출용) |
| `title` | VARCHAR(64) | NOT NULL, DEFAULT '' | 스트림 제목 |
| `next_branch_num` | INT | NOT NULL, DEFAULT 0 | 다음에 생성될 브랜치 번호 |
| `created_by` | VARCHAR(10) | NOT NULL | 생성자 (publicId) |
| `created_at` | TIMESTAMP(6) | NOT NULL | 생성 시간 |
| `updated_by` | VARCHAR(10) | NOT NULL | 최종 수정자 |
| `updated_at` | TIMESTAMP(6) | NOT NULL, DEFAULT NOW() | 최종 수정 시간 |

**인덱스:**
- `UK_stream_uuid`: UNIQUE (`uuid`)
- `idx_stream_created_by`: (`created_by`, `updated_at`)

**설계 노트:**
- `uuid`를 외부 API에 노출하여 `stream_id` 노출 방지 (보안)
- `next_branch_num`은 브랜치 추가 시 자동 증가
- `updated_at`은 비즈니스 로직에서 명시적으로 업데이트 (JPA Auditing과 별도)

---

### 2. branches 테이블

스트림 내의 분기된 흐름

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `stream_id` | BIGINT | PK, FK → streams(stream_id) | 소속 스트림 ID |
| `branch_num` | INT | PK | 스트림 내 브랜치 번호 (0부터 시작) |
| `path` | VARCHAR(500) | NOT NULL, DEFAULT '' | 브랜치 경로 (예: "0,1,5") |

**Primary Key:** (`stream_id`, `branch_num`) - Composite Key

**Foreign Key:**
- `FK_branch_to_stream`: `stream_id` → `streams(stream_id)` ON DELETE CASCADE

**인덱스:**
- `idx_branch_stream_id`: (`stream_id`)

**설계 노트:**
- **Composite Key 사용 이유**: 브랜치는 스트림 내에서만 고유하므로 (stream_id + branch_num) 조합 사용
- `path`: 경로 계산에 사용 (예: "0,1,5" = Branch 0 → 1 → 5)
- 브랜치 0은 항상 Main 브랜치

---

### 3. points 테이블

각 브랜치의 메시지 포인트

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `point_id` | BIGINT | PK, AUTO_INCREMENT | 포인트 고유 ID |
| `uuid` | UUID | NOT NULL | 공개 UUID (외부 API 노출용) |
| `stream_id` | BIGINT | FK → branches(stream_id), NOT NULL | 소속 스트림 ID (읽기 전용) |
| `branch_num` | INT | FK → branches(branch_num), NOT NULL | 소속 브랜치 번호 (읽기 전용) |
| `item_id` | VARCHAR(255) | NULLABLE | 저장할 아이템의 ID (root는 NULL) |
| `depth` | INT | NOT NULL | 메시지 깊이 (0부터 시작) |
| `child_branch_nums` | VARCHAR(256) | DEFAULT '' | 이 포인트에서 분기된 브랜치 번호들 (쉼표 구분) |
| `created_by` | VARCHAR(10) | NOT NULL | 생성자 (publicId) |
| `created_at` | TIMESTAMP(6) | NOT NULL | 생성 시간 |

**Foreign Keys:**
- `FK_point_to_branch`: (`stream_id`, `branch_num`) → `branches(stream_id, branch_num)` ON DELETE CASCADE
- `FK_point_to_stream`: `stream_id` → `streams(stream_id)` (읽기 전용, JPQL 쿼리용)

**인덱스:**
- `idx_point_stream_branch_depth`: (`stream_id`, `branch_num`, `depth`)
- `idx_point_uuid`: (`uuid`)

**설계 노트:**
- `stream_id`, `branch_num`: **읽기 전용 필드** (`insertable=false, updatable=false`)
  - `FK_point_to_branch`에 의해 자동으로 값이 채워짐
  - Repository JPQL 쿼리에서 사용 (`LEFT JOIN PointEntity p ON p.stream = s`)
- `item_id`: Root Point는 NULL (분기 시작점)
- `depth`: 0부터 시작 (Root = 0)
- `child_branch_nums`: JSON이 아닌 CSV 포맷 (예: "1,2,5")

---

## 🔑 주요 설계 결정

### 1. Composite Key (BranchEntity)

**결정:** Branch는 (stream_id, branch_num) 조합을 Primary Key로 사용

**이유:**
- Branch는 Stream 내에서만 고유함
- Stream A의 Branch 0과 Stream B의 Branch 0은 별개
- 자연스러운 도메인 모델 표현

**구현:**
```java
@Embeddable
public class BranchId implements Serializable {
    private Long streamId;
    private int branchNum;
}

@EmbeddedId
private BranchId id;
```

---

### 2. Path-based Query

**결정:** Branch에 `path` 필드 저장 (예: "0,1,5")

**이유:**
- 특정 브랜치까지의 모든 포인트를 효율적으로 조회하기 위함
- SQL IN 절 활용 가능

**사용 예시:**
```sql
-- Branch 5의 모든 포인트 조회 (경로: 0 → 1 → 5)
SELECT * FROM points
WHERE stream_id = 1
  AND branch_num IN (0, 1, 5)  -- path를 파싱한 결과
  AND depth > 0
ORDER BY depth;
```

**장점:**
- 재귀 쿼리 없이 단일 쿼리로 조회 가능
- 인덱스 활용 가능

**단점:**
- 브랜치 구조 변경 시 path 재계산 필요 (현재는 불변)

---

### 3. Read-Only Fields (PointEntity)

**결정:** `stream_id`, `branch_num` 필드를 읽기 전용으로 설정

**이유:**
- Point는 Branch에 종속되므로 stream_id, branch_num은 branch FK로부터 자동 결정
- JPQL 쿼리에서 `p.stream = s` 형태로 사용하기 위해 필드 필요
- Java 코드에서는 `point.getBranch().getStream()` 사용 (안전)

**주의사항:**
- H2 테스트 환경에서 Lazy Loading 프록시 초기화 실패 가능
- `PointEntity.stream` 필드는 `@Getter(AccessLevel.PRIVATE)` 적용
- Java 코드에서는 **절대** `point.getStream()` 호출 금지

**참고:**
- [StreamEntity.java:98-101](src/main/java/me/hanju/branchdown/entity/PointEntity.java#L98-L101)

---

### 4. Depth-based Filtering

**결정:** 쿼리에서 `depth > :depth` 조건 사용

**의미:**
- "이미 받은 메시지(depth) 이후부터 조회"
- 증분 로딩 지원

**사용 예시:**
```java
// 처음 로드 (depth 0부터)
List<PointDto> points = streamService.getStreamPoints(uuid); // depth=-1 전달

// 증분 로드 (depth 5 이후부터)
List<PointDto> newPoints = streamService.getBranchMessages(uuid, branchNum, 5);
```

**주의사항:**
- `depth > 0`은 depth 1부터 (root 제외)
- `depth > -1`은 depth 0부터 (root 포함)

---

### 5. UUID vs Primary Key

**결정:** 외부 API는 UUID 사용, 내부 DB는 BIGINT PK 사용

**이유:**
- **보안**: Primary Key(1, 2, 3...) 노출 시 데이터 추측 가능
- **성능**: JOIN 시 BIGINT가 UUID보다 빠름
- **호환성**: UUID는 URL-safe, 분산 시스템 호환

**구현:**
```java
// API 엔드포인트
GET /api/streams/{uuid}  // UUID 사용

// Entity 내부
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;  // DB 성능

@Column(nullable = false)
private UUID uuid;  // API 노출
```

---

## 🔍 인덱스 전략

### streams 테이블

```sql
CREATE UNIQUE INDEX UK_stream_uuid ON streams(uuid);
CREATE INDEX idx_stream_created_by ON streams(created_by, updated_at);
```

- `uuid`: 외부 API 조회용 (UNIQUE)
- `(created_by, updated_at)`: 사용자별 스트림 목록 조회 + 정렬

### branches 테이블

```sql
CREATE INDEX idx_branch_stream_id ON branches(stream_id);
```

- `stream_id`: 스트림의 모든 브랜치 조회

### points 테이블

```sql
CREATE INDEX idx_point_stream_branch_depth ON points(stream_id, branch_num, depth);
CREATE INDEX idx_point_uuid ON points(uuid);
```

- `(stream_id, branch_num, depth)`: 브랜치별 포인트 조회 + 정렬 (복합 인덱스)
- `uuid`: 외부 API 조회용

---

## 🗂️ 데이터 예시

### 시나리오: 사용자가 depth 1에서 분기 생성

#### streams 테이블

| stream_id | uuid | title | next_branch_num | created_by | created_at |
|-----------|------|-------|----------------|------------|-----------|
| 1 | `a1b2c3d4-...` | "내 대화" | 2 | `user123` | 2025-11-07 10:00:00 |

#### branches 테이블

| stream_id | branch_num | path |
|-----------|-----------|------|
| 1 | 0 | "" (Main) |
| 1 | 1 | "0" (Branch 0에서 분기) |

#### points 테이블

| point_id | uuid | stream_id | branch_num | item_id | depth | child_branch_nums |
|----------|------|-----------|-----------|---------|-------|------------------|
| 1 | `p1-uuid` | 1 | 0 | NULL | 0 | "1" |
| 2 | `p2-uuid` | 1 | 0 | "msg1" | 1 | "1" |
| 3 | `p3-uuid` | 1 | 0 | "msg2" | 2 | "" |
| 4 | `p4-uuid` | 1 | 1 | "msg3" | 2 | "" |

**시각화:**
```
Stream 1
├─ Branch 0 (Main)
│  ├─ Point 1 (depth=0, root) ← 분기점
│  ├─ Point 2 (depth=1, msg1) ← 분기점
│  └─ Point 3 (depth=2, msg2)
│
└─ Branch 1
   ├─ Point 1 (depth=0, root) - 공유
   ├─ Point 2 (depth=1, msg1) - 공유
   └─ Point 4 (depth=2, msg3) - 새 응답
```

---

## 🚀 주요 쿼리 패턴

### 1. 스트림의 최신 브랜치 포인트 조회

```sql
-- 최근 업데이트된 브랜치의 경로 계산 후 포인트 조회
SELECT p.*
FROM points p
WHERE p.stream_id = :streamId
  AND p.branch_num IN (:branchNums)  -- path를 파싱한 배열
  AND p.depth > -1
ORDER BY p.depth;
```

**구현:** [PointRepository.java:findAllUsingPath()](src/main/java/me/hanju/branchdown/repository/PointRepository.java)

### 2. 특정 브랜치의 포인트 조회 (depth 필터링)

```sql
SELECT p.*
FROM points p
WHERE p.stream_id = :streamId
  AND p.branch_num IN (:branchNums)
  AND p.depth > :depth  -- 증분 로딩
ORDER BY p.depth;
```

### 3. 사용자의 스트림 목록 조회 (페이징)

```sql
SELECT s.*
FROM streams s
WHERE s.created_by = :publicId
  AND (:query IS NULL OR s.title LIKE :query)
ORDER BY s.updated_at DESC
LIMIT :size OFFSET :offset;
```

**구현:** [StreamRepository.java:findAllByCreatedBy()](src/main/java/me/hanju/branchdown/repository/StreamRepository.java)

---

## 🛡️ 데이터 무결성

### Cascade 설정

1. **Stream 삭제 시**: 모든 Branch, Point 삭제 (CASCADE)
   ```java
   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
   private List<BranchEntity> branches;
   ```

2. **Branch 삭제 시**: 소속 Point 모두 삭제 (CASCADE)
   ```java
   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
   private List<PointEntity> points;
   ```

### 제약 조건

- `branches.stream_id`: NOT NULL, FK → streams
- `points.stream_id`: NOT NULL, FK → branches (Composite)
- `points.depth`: NOT NULL, >= 0
- `streams.uuid`: UNIQUE
- `streams.title`: NOT NULL

---

## 📈 확장성 고려사항

### 1. 파티셔닝 전략 (대규모 데이터 시)

```sql
-- stream_id 기준 파티셔닝
ALTER TABLE points PARTITION BY RANGE (stream_id) (
    PARTITION p0 VALUES LESS THAN (1000000),
    PARTITION p1 VALUES LESS THAN (2000000),
    ...
);
```

### 2. 아카이빙 전략

오래된 스트림을 별도 테이블로 이동:

```sql
CREATE TABLE streams_archive LIKE streams;
CREATE TABLE branches_archive LIKE branches;
CREATE TABLE points_archive LIKE points;

-- 6개월 이상 비활성 스트림 아카이빙
INSERT INTO streams_archive SELECT * FROM streams WHERE updated_at < NOW() - INTERVAL 6 MONTH;
```

### 3. 읽기 복제본

- 조회가 많은 경우 Read Replica 구성
- `@Transactional(readOnly = true)` 활용

---

## 📊 통계 정보

프로젝트 규모 추정:

| 항목 | 예상값 (중규모) |
|------|----------------|
| 활성 사용자 | 10,000명 |
| 사용자당 평균 스트림 | 20개 |
| 스트림당 평균 포인트 | 50개 |
| **총 Points 레코드** | **10,000,000개 (1천만)** |
| 스트림당 평균 브랜치 | 3개 |
| **총 Branches 레코드** | **600,000개** |

**스토리지 예상:**
- Points 테이블: ~1GB (인덱스 포함)
- Branches 테이블: ~50MB
- Streams 테이블: ~10MB

---

## 🔗 관련 문서

- [README.md](README.md) - 프로젝트 개요 및 실행 가이드
- [Entity 소스 코드](src/main/java/me/hanju/branchdown/entity/)
- [Repository 소스 코드](src/main/java/me/hanju/branchdown/repository/)

---

**문서 작성일**: 2025-11-07
**최종 수정일**: 2025-11-07

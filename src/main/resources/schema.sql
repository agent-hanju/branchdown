-- Branchdown Database Schema
-- 운영 환경에서 ddl-auto: validate 사용 시 이 스키마로 테이블 생성 필요

CREATE TABLE IF NOT EXISTS streams (
    stream_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NOW() NOT NULL,
    next_branch_num INTEGER COMMENT '다음에 붙일 브랜치 번호',
    PRIMARY KEY (stream_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS branches (
    branch_num INTEGER NOT NULL COMMENT '스트림의 각 브랜치에 붙는 번호. 0부터 시작해 순차적으로 쌓인다.',
    stream_id BIGINT NOT NULL,
    path VARCHAR(500) NOT NULL COMMENT '자기 자신까지 오기 위한 branch_num의 경로. 구분자는 ","',
    PRIMARY KEY (branch_num, stream_id),
    CONSTRAINT FK_branch_to_stream FOREIGN KEY (stream_id) REFERENCES streams (stream_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS points (
    point_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NOW() NOT NULL,
    branch_num INTEGER COMMENT '소속 브랜치의 branchNum',
    child_branch_nums VARCHAR(256) COMMENT '이 포인트를 베이스로 하는 branch_num 목록(쉼표로 구분)',
    depth INTEGER NOT NULL COMMENT '0부터 시작하는 stream 내에서의 depth',
    item_id VARCHAR(255) COMMENT '저장할 아이템의 ID, root의 경우 null',
    stream_id BIGINT NOT NULL,
    PRIMARY KEY (point_id),
    CONSTRAINT FK_point_to_branch FOREIGN KEY (branch_num, stream_id) REFERENCES branches (branch_num, stream_id),
    CONSTRAINT FK_point_to_stream FOREIGN KEY (stream_id) REFERENCES streams (stream_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

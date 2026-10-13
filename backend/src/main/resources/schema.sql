-- 电影评论智能情感分析系统 —— 生产建表脚本
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS，每次启动执行安全
-- 兼容 MySQL 8+ / 26.x 与 H2（MODE=MySQL）

-- ========== 用户与权限 ==========

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP
);

-- ========== 电影与评论 ==========

CREATE TABLE IF NOT EXISTS movie (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    director VARCHAR(200),
    actors VARCHAR(500),
    release_year INT,
    poster_url VARCHAR(500),
    synopsis TEXT,
    comment_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id VARCHAR(50) NOT NULL,
    text TEXT NOT NULL,
    author VARCHAR(100),
    publish_time VARCHAR(50),
    like_count INT DEFAULT 0,
    rating INT,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comment_movie ON comment (movie_id);

-- ========== 分析结果 ==========

CREATE TABLE IF NOT EXISTS comment_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    polarity VARCHAR(20) NOT NULL,
    confidence DOUBLE,
    trigger_words TEXT,
    analyzer_name VARCHAR(50),
    raw_json TEXT,
    elapsed_ms BIGINT,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_comment ON comment_analysis (comment_id);

-- 方面级结果用纵表：维度集合可能调整，纵表零迁移成本
CREATE TABLE IF NOT EXISTS comment_aspect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    aspect_name VARCHAR(50) NOT NULL,
    mentioned BOOLEAN NOT NULL DEFAULT FALSE,
    polarity VARCHAR(20),
    confidence DOUBLE,
    sample_count INT DEFAULT 0,
    trigger_words TEXT,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aspect_comment ON comment_aspect (comment_id);

-- ========== 爬虫任务 ==========

CREATE TABLE IF NOT EXISTS crawler_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    progress INT DEFAULT 0,
    message VARCHAR(500),
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

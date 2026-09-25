-- 站长账号（全站仅 1 行）
CREATE TABLE IF NOT EXISTS user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 统一内容表：帖子/图片/评论
CREATE TABLE IF NOT EXISTS content (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(20) NOT NULL,                    -- POST / IMAGE / COMMENT
  source VARCHAR(20) NOT NULL,                  -- ADMIN / SUBMIT
  title VARCHAR(120),
  body TEXT,                                    -- Markdown 正文（评论存文本）
  media_url VARCHAR(255),                       -- 图片路径
  thumbnail_url VARCHAR(255),                   -- 缩略图（审核预览用）
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',-- PENDING/APPROVED/REJECTED/DELETED
  like_count INT NOT NULL DEFAULT 0,
  view_count INT NOT NULL DEFAULT 0,
  ip VARCHAR(45),                               -- 留痕
  ua VARCHAR(255),                              -- 留痕
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_by BIGINT,                           -- 审核人
  reviewed_at DATETIME,                         -- 审核时间
  parent_id BIGINT DEFAULT NULL,
  author_label VARCHAR(50) DEFAULT NULL,
  sort_weight INT NOT NULL DEFAULT 0 COMMENT '置顶权重，1=置顶',
  KEY idx_status_type (status, type, created_at),
  KEY idx_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报
CREATE TABLE IF NOT EXISTS report (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content_id BIGINT NOT NULL,
  reporter_ip VARCHAR(45),
  reason VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',   -- OPEN / HANDLED / IGNORED
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at DATETIME,
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 敏感词库
CREATE TABLE IF NOT EXISTS sensitive_word (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  word VARCHAR(50) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 作品集
CREATE TABLE IF NOT EXISTS portfolio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  summary VARCHAR(500),
  tech_stack VARCHAR(255),
  link VARCHAR(255),
  cover_url VARCHAR(255),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content_image (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content_id BIGINT NOT NULL,          -- 属于哪篇话题
  url VARCHAR(255) NOT NULL,           -- 原图
  thumbnail_url VARCHAR(255),          -- 缩略图（审核预览）
  sort_order INT NOT NULL DEFAULT 0,   -- 排序
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_content (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ══ 种子数据（幂等）══

INSERT INTO user (username, password_hash, role) VALUES ('azv', 'PENDING_SET', 'ADMIN')
  ON DUPLICATE KEY UPDATE username = username;
-- 密码 hash 阶段 2 登录时用 BCrypt 生成后 UPDATE

INSERT INTO sensitive_word (word) VALUES
  ('加V'), ('免费领取'), ('低价出课'), ('赌博'), ('博彩'),
  ('刷单'), ('代开发票'), ('办证'), ('裸聊'), ('黄色小视频')
ON DUPLICATE KEY UPDATE word = word;

INSERT INTO content (type, source, title, body, status, ip)
SELECT 'POST','ADMIN','HCS 医院预约挂号系统 — 项目复盘',
       '# 项目背景\nSpring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT + Vue3 全栈项目。',
       'APPROVED','127.0.0.1'
WHERE NOT EXISTS (
  SELECT 1 FROM content
  WHERE type = 'POST' AND source = 'ADMIN' AND title = 'HCS 医院预约挂号系统 — 项目复盘'
);

INSERT INTO content (type, source, title, body, status, ip)
SELECT 'POST','ADMIN','MySQL 索引与慢查询优化笔记',
       '# 索引原理\nB+ 树为什么适合磁盘索引？联合索引最左前缀、覆盖索引、回表。',
       'APPROVED','127.0.0.1'
WHERE NOT EXISTS (
  SELECT 1 FROM content
  WHERE type = 'POST' AND source = 'ADMIN' AND title = 'MySQL 索引与慢查询优化笔记'
);

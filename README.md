# AzV 个人网站

一个前后端分离的个人站点，包含文章、图片墙、作品集、访客投稿与评论，以及站长审核、发布、举报处理和在线简历管理。

## 技术架构

```text
浏览器
  └─ Nginx
      ├─ /                → frontend/dist（Vue 单页应用）
      ├─ /api/**          → Spring Boot :8080
      └─ /uploads/**      → 图片持久化目录

Spring Boot
  ├─ MySQL               → 用户、内容、评论、举报、作品集
  ├─ Redis               → 登录限流、点赞去重、JWT 注销状态
  ├─ UPLOAD_DIR          → 投稿及发布图片
  └─ RESUME_STORAGE_DIR  → 当前公开简历 resume.pdf
```

- 前端：Vue 3、Vue Router、Pinia、Axios、Vite、markdown-it
- 后端：Java 21、Spring Boot 3.3、MyBatis-Plus、MySQL、Redis、JWT
- 认证：JWT 存入 `HttpOnly`、`SameSite=Lax` Cookie；生产环境启用 `Secure`
- 文件：图片存本地文件系统；简历通过受控 API 预览、下载和替换

## 功能

- 首页文章流、关键字搜索、分页加载和 Markdown 详情
- 图片墙、访客投稿、评论、点赞与举报
- 作品集、联系方式、PDF 简历在线预览与下载
- 站长登录、内容审核、发布、置顶、隐藏、删除和举报处理
- 后台直接上传或替换 PDF 简历，公开页立即使用新版本
- 公开内容使用最小化 DTO，不返回 IP、User-Agent、审核人等内部字段

## 运行环境

- JDK 21
- Maven 3.9+
- Node.js `^20.19.0` 或 `>=22.12.0`（由当前 Vite 版本要求）
- npm（随 Node.js 安装）
- MySQL，建议 8.x
- Redis，建议 6.x 或 7.x
- 生产环境建议使用 Nginx 和 HTTPS

## 配置

配置模板见 [`.env.example`](./.env.example)。应用会按顺序尝试读取当前工作目录和上一级目录中的 `.env`，因此从项目根目录或 `backend` 目录启动都可以读取根目录 `.env`；操作系统环境变量具有更高优先级。生产环境仍建议通过 systemd `EnvironmentFile`、容器编排或密钥管理服务注入。不要提交真实的 `.env`、数据库密码或 JWT 密钥。

| 变量 | 用途 | 默认值或要求 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring Profile | 生产使用 `prod` |
| `DB_URL` | MySQL JDBC 地址 | 开发配置默认端口为 `3307`；生产示例为 `3306` |
| `DB_USERNAME` | 数据库账号 | 默认 `azv_app` |
| `DB_PASSWORD` | 数据库密码 | 生产必填 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 | `127.0.0.1` / `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 无默认生产密码要求，按实例配置 |
| `REDIS_DATABASE` | Redis DB | `0` |
| `REDIS_TIMEOUT` | Redis 超时 | `2s` |
| `JWT_SECRET` | JWT HMAC 密钥 | 必填；至少 32 个随机字节，并妥善轮换 |
| `JWT_EXPIRE_HOURS` | 登录有效期 | `24` |
| `SERVER_PORT` | 后端端口 | `8080` |
| `UPLOAD_DIR` | 图片持久化目录 | 开发 `./uploads`，生产 `/opt/azv/uploads` |
| `RESUME_STORAGE_DIR` | 简历持久化目录 | 开发 `./data/resume`，生产 `/opt/azv/data/resume` |
| `AUTH_COOKIE_SECURE` | Cookie 仅通过 HTTPS 发送 | 开发默认 `false`，生产默认 `true` |
| `TRUST_FORWARDED_HEADERS` | 是否信任代理提供的客户端 IP | 仅在应用只能由可信代理访问时设为 `true` |
| `TRUSTED_PROXY_REGEX` | Tomcat 可信内部代理表达式 | 生产默认仅本机回环地址 |

生产 Profile 会把后端绑定到 `127.0.0.1`。如果 Nginx 不在同一台主机，需要改造网络边界和可信代理配置，不能简单把服务暴露到公网后继续信任转发头。

## 初始化数据库

1. 创建 `azv` 数据库和权限受限的应用账号。
2. 执行 [`backend/sql/schema.sql`](./backend/sql/schema.sql)。脚本中的建表与种子数据可重复执行，但 `CREATE TABLE IF NOT EXISTS` 不会为旧表自动补列。
3. 将默认管理员 `azv` 的 `password_hash` 从 `PENDING_SET` 更新为离线生成的 BCrypt 哈希；不要把明文密码写入 SQL 或配置文件。

```sql
UPDATE user
SET password_hash = '<BCrypt 哈希>', status = 1
WHERE username = 'azv';
```

从旧版本升级时，先用 `SHOW COLUMNS` 检查并补齐以下字段，再部署新代码：

```sql
ALTER TABLE user ADD COLUMN status TINYINT NOT NULL DEFAULT 1;
ALTER TABLE content ADD COLUMN sort_weight INT NOT NULL DEFAULT 0;
```

仅执行数据库中缺失的语句。生产迁移前必须备份；后续建议用 Flyway 或 Liquibase 取代手工迁移。

## 本地开发

先启动 MySQL 和 Redis，并在根目录 `.env` 或终端/IDE 中至少设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`。本地不要启用 `prod` Profile，也不要把 `AUTH_COOKIE_SECURE` 设为 `true`，否则 HTTP 页面无法正常携带登录 Cookie。

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm ci
npm run dev
```

开发地址为 `http://localhost:5173`。Vite 会把 `/api` 和 `/uploads` 代理到 `http://localhost:8080`。

## 测试与构建

```bash
cd backend
mvn test
mvn clean package

cd ../frontend
npm ci
npm run build
```

产物：

- 后端：`backend/target/azv-backend-0.0.1-SNAPSHOT.jar`
- 前端：`frontend/dist/`

生产启动示例：

```bash
java -jar backend/target/azv-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Nginx 关键配置

下面只展示与本项目有关的核心项；TLS 证书、日志、压缩、缓存策略和其他安全响应头请按实际域名补齐。

```nginx
server {
    listen 80;
    server_name example.com;

    root /opt/azv/frontend/dist;
    index index.html;

    # 与 Spring 的 40MB 总请求上限保持一致；应用仍单独限制 PDF 20MB、单图 10MB。
    client_max_body_size 40m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 覆盖而不是透传客户端传入的 X-Forwarded-For。
        proxy_set_header X-Forwarded-For $remote_addr;
    }

    # 必须与 UPLOAD_DIR 指向同一目录；不要让它落入 SPA fallback。
    location ^~ /uploads/ {
        alias /opt/azv/uploads/;
        autoindex off;
        add_header X-Content-Type-Options nosniff always;
    }

    # Vue Router history 模式的回退。
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

生产环境应启用 HTTPS，并保留 `AUTH_COOKIE_SECURE=true`。只有在后端无法被外部直接访问、且 Nginx 会覆盖转发头时，才使用 `TRUST_FORWARDED_HEADERS=true`。修改后先执行 `nginx -t`，再平滑重载。

## 在页面内替换 PDF 简历

1. 访问 `/login` 登录站长账号。
2. 进入 `/admin/resume`。
3. 选择不超过 20 MB 的 PDF，确认上传或替换。
4. 页面会立即显示新文件预览；公开 `/about` 页面同步提供预览和下载，无需重新构建前端。

相关接口：

| 接口 | 说明 |
| --- | --- |
| `GET /api/resume` | 公开的当前简历元数据 |
| `GET /api/resume/file` | 浏览器内嵌预览 |
| `GET /api/resume/file?download=true` | 附件下载 |
| `POST /api/admin/resume` | 管理员上传，`multipart/form-data` 字段名为 `file` |

后端先把文件写到 `RESUME_STORAGE_DIR` 内的临时文件，检查大小和 `%PDF-` 文件头，再在同一目录中原子替换 `resume.pdf`。校验或写入失败时保留旧版本；底层文件系统不支持原子移动时会拒绝替换。

部署时务必做到：

- 预先创建 `UPLOAD_DIR` 和 `RESUME_STORAGE_DIR`，授予后端进程读写权限。
- 把两个目录挂载到持久化磁盘或容器 Volume；发布时不要随应用目录清空。
- Nginx 只需直接读取 `UPLOAD_DIR`。简历仍应经 `/api/resume/file` 返回，以保留缓存校验、下载头和防嗅探响应头。
- 定期备份 `resume.pdf`。应用只维护当前版本，不提供历史版本回滚。

## 部署前检查

- 轮换数据库密码和 `JWT_SECRET`，确认仓库、构建产物和日志中没有旧凭据。
- 备份 MySQL、图片目录和简历目录，并完成旧库字段迁移。
- 检查管理员 BCrypt 密码和 `user.status=1`。
- 确认 Redis 可用；登录限流、点赞去重和注销 JWT 都依赖 Redis。
- 运行后端测试和前端生产构建。
- 校验 Nginx SPA fallback、`/api` 代理、`/uploads` 映射和 40 MB 总请求上限。
- 发布后重新登录；旧版无 `jti` 的 Cookie 或使用旧密钥签发的 Cookie 会失效。

本轮优化的详细记录、兼容性说明和后续建议见 [`docs/OPTIMIZATION_LOG.md`](./docs/OPTIMIZATION_LOG.md)。

想逐步理解每个功能的实现，可阅读面向初学者的 [`功能实现教程`](./docs/FUNCTION_IMPLEMENTATION_GUIDE.md)。

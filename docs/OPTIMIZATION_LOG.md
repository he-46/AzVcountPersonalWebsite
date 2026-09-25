# 优化记录

日期：2026-09-24

## 范围与目标

本轮优化覆盖认证与配置安全、公开数据边界、内容计数与媒体一致性、前端加载体验、SEO 基础能力，以及“无需重新部署即可在后台替换 PDF 简历”。

记录只区分三类状态：已落地、已验证、后续建议。没有完成端到端验证的项目不会写成已通过。

## 已落地

### 1. 配置与认证安全

- 数据库、Redis、JWT、端口和文件目录改为环境变量配置；仓库提供脱敏的 `.env.example`。
- Spring Boot 会从当前工作目录或上一级目录自动导入未提交的 `.env`，兼容从项目根和 `backend` 目录启动；操作系统环境变量仍优先。
- `.gitignore` 覆盖 `.env`、密钥、构建产物、依赖目录和运行时上传数据。
- 删除遗留 Cookie 文件和带旧配置的生成目录文件，不再把凭据保留在工作区产物中。
- 生产 Profile 默认只监听 `127.0.0.1`，由同机 Nginx 反向代理。
- 登录接口改为 JSON 请求体 `{ username, password }`，不再通过 URL 查询参数传密码，也不接受客户端自报 IP。
- 新增统一 `ClientIpResolver`；默认忽略 `X-Forwarded-For`，只有显式开启可信代理模式时才使用代理头。
- 登录失败同时按服务端解析出的 IP 和账号限流，并检查账号 `status=1`。
- JWT 新增唯一 `jti`。注销时按剩余有效期写入 Redis 撤销键并清除 Cookie；后台拦截器会拒绝已撤销、无 `jti`、过期或非管理员令牌。
- 认证 Cookie 使用 `HttpOnly`、`SameSite=Lax`；生产默认启用 `Secure`。
- 移除会暴露用户记录及密码哈希的公开用户列表入口，移除公开敏感词测试入口。

### 2. 公开内容与数据一致性

- 公开列表、详情和评论改用专用 DTO，仅返回页面需要的字段，不再暴露 IP、User-Agent、审核状态、来源和审核人。
- 公开内容列表始终排除 `COMMENT`；详情拒绝评论 ID；评论列表同时约束父内容、`COMMENT` 类型和 `APPROVED` 状态。
- 列表返回最长 180 字的纯文本 `excerpt`，减少首页传输和渲染完整正文。
- 列表新增 `withMedia=true` 数据库筛选，图片墙可分页读取真正含媒体的内容，而不是先取固定数量再在前端过滤。
- 浏览量改为数据库原子自增，并重新读取后返回包含本次访问的计数。
- 点赞改为 Redis Set 原子判重和数据库原子自增；数据库写入失败时撤销本次 Redis 标记，允许重试。
- 投稿统一保存为 `POST`，后台待审投稿统计同步按 `POST` 计算；兼容字段名 `pendingImage` 暂时保留。
- 新增统一媒体清理服务：收集封面、缩略图和多图记录，先更新数据库，事务提交成功后再删除物理文件。审核驳回、内容删除和举报下架复用同一逻辑。
- `schema.sql` 把 `sort_weight` 纳入建表定义，管理员与内容种子数据改为可重复执行，避免每次初始化重复插入。

### 3. 前端体验与基础 SEO

- 登录调用与后端 JSON 契约一致，移除客户端 IP 参数。
- 后台文章发布正确提交 `FormData` 请求体，修复把表单误放入请求参数配置的问题。
- 文章详情同时加载正文与评论，增加加载、失败、404 和重试状态；图片启用懒加载和异步解码。
- 首页使用摘要，支持“加载更多”；搜索和列表错误改为页面内提示，不再依赖阻塞式弹窗。
- 图片墙使用服务端 `withMedia=true` 分页，支持加载更多和失败重试。
- 作品集项目链接、邮箱、电话和 GitHub 改为可操作链接，并校验项目链接协议。
- 路由统一维护页面标题、description、Open Graph 摘要和 robots；登录、后台、404 标记为 `noindex, nofollow`。
- 新增 404 页面，并补充移动端、状态提示和无障碍相关样式。

### 4. PDF 简历在线替换

- 新增公开元数据、内嵌预览和下载接口。
- 新增受后台认证保护的 PDF 上传接口及 `/admin/resume` 管理页面。
- `/about` 会读取当前简历元数据，在页面内嵌预览，并提供新窗口查看和下载。
- 前后端统一限制为 20 MB；后端同时检查实际写入字节数和 `%PDF-` 文件头，不信任扩展名或浏览器 Content-Type。
- 文件先写到目标目录内的临时文件，再用原子移动替换固定的 `resume.pdf`，避免访客读到半写入文件；失败时清理临时文件并保留旧简历。
- 文件响应提供 ETag、Last-Modified、`nosniff`、同源 frame 限制以及 inline/attachment 两种 Content-Disposition。
- 简历存储目录由 `RESUME_STORAGE_DIR` 配置，与应用包分离，适合挂载持久化磁盘。

## 接口与兼容性

| 项目 | 新行为 | 升级影响 |
| --- | --- | --- |
| `POST /api/auth/login` | JSON `{username,password}` | 旧的 query/form 调用方需要更新；当前前端已同步 |
| 管理员 JWT | 必须含 `jti`，注销后可立即失效 | 升级或轮换密钥后要求重新登录 |
| `GET /api/content/list` | 返回公开摘要 DTO，正文改为 `excerpt`；排除评论 | 依赖完整实体或 `body` 的外部调用方需要调整 |
| `GET /api/content/{id}` | 保持 `{content, images}` 外层结构，内部内容为公开 DTO | 读取内部审核/留痕字段的调用方不再可用 |
| `GET /api/content/{id}/comments` | 只返回已审核评论的最小字段 | 不再返回内部内容实体字段 |
| 内容列表参数 | 新增 `withMedia`；`type=COMMENT` 被拒绝 | 图片类客户端建议使用 `withMedia=true` |
| 后台审核统计 | `pendingImage` 字段仍存在，但统计口径改为待审 `POST` | 旧后台不崩溃；后续版本可改名为 `pendingPost` |
| `POST /api/admin/publish/post` | 正确接收 multipart `FormData` | 当前后台已同步，无需人工改请求头 |
| 简历接口 | 新增 `/api/resume`、`/api/resume/file`、`/api/admin/resume` | Nginx 必须允许上传请求体并代理 `/api`；建议总请求上限 40 MB，应用单独限制 PDF 20 MB |

数据库兼容注意：新的 `schema.sql` 可安全初始化空库，但 `CREATE TABLE IF NOT EXISTS` 不会修改已存在的表。旧库需要先检查并按需增加 `user.status`、`content.sort_weight`，同时确认 `content_image` 表存在。不要在未备份的生产库盲目执行 `ALTER TABLE`。

## 已验证

本轮已执行并通过：

- 后端使用 Maven 离线 `clean package`（fork compiler）成功，生成可执行 JAR；共 8 项测试，0 failures，0 errors。
  - `ResumeServiceTest`：合法 PDF 原子替换；非法文件不破坏旧版本且清理临时文件；超限文件拒绝。
  - `ClientIpResolverTest`：默认忽略转发头；可信模式选择规范 IP；非法转发值回退到远端地址。
  - `JwtUtilTest`：拒绝短于 HS256 下限的密钥；签发的令牌包含唯一 `jti` 并可正确解析。
- 已检查可执行 JAR 内的 `application.yml`，数据库密码和 JWT 密钥均为环境变量占位，没有字面量凭据。
- 前端使用 Vite 8.2.2 完成生产构建，共处理 120 个模块并生成 `frontend/dist`；发布目录包含禁止抓取 `/admin` 与 `/login` 的 `robots.txt`。

上述验证不等于完整上线验收。目前没有在本文档编写环境中连接真实 MySQL、Redis 和 Nginx 进行浏览器端到端测试，也没有覆盖所有控制器的集成测试。部署后仍需执行下方冒烟检查。

## 部署清单

1. 备份 MySQL、`UPLOAD_DIR` 和 `RESUME_STORAGE_DIR`。
2. 轮换数据库密码与 `JWT_SECRET`；检查源文件、旧构建目录、日志和发布包中不存在历史凭据。
3. 对照 `backend/sql/schema.sql` 检查旧库结构，补齐缺失列和表；确认管理员密码为 BCrypt 哈希且 `status=1`。
4. 创建持久化目录，例如 `/opt/azv/uploads`、`/opt/azv/data/resume`，授予后端进程读写权限；若 Nginx 直接服务图片，还需授予其只读和目录遍历权限。
5. 注入 `.env.example` 中列出的生产变量。不要依赖当前工作目录，也不要在 systemd unit 中写入可被非授权用户读取的密钥。
6. 执行 `mvn test`、`mvn clean package` 和 `npm ci && npm run build`，部署新的 JAR 与 `frontend/dist`。
7. 配置 Nginx：SPA fallback、`/api` 反代、`/uploads` 静态映射、`client_max_body_size 40m`；覆盖 `X-Forwarded-For`，不要直接透传客户端值。
8. 保持生产后端绑定回环地址并启用 HTTPS；确认 `AUTH_COOKIE_SECURE=true`，只有可信代理拓扑才启用 `TRUST_FORWARDED_HEADERS=true`。
9. `nginx -t` 后平滑重载，重启后端。所有管理员重新登录。
10. 冒烟检查：公开列表与详情、评论审核链路、投稿图片、点赞去重、举报下架、后台发布、注销后后台拒绝访问、PDF 替换与公开预览下载。
11. 检查浏览器 Network：认证 Cookie 为 HttpOnly/Secure/SameSite=Lax；公开内容响应中没有 `ip`、`ua`、`reviewedBy`、`passwordHash`。

## 剩余建议

按优先级建议继续推进：

1. 引入 Flyway 或 Liquibase，把旧库补列、索引和种子调整变成版本化迁移，并在测试库演练升级与回滚。
2. 修正当前工作区的版本库根目录：业务代码根目录目前没有 `.git`，仅嵌套占位目录带有 Git 元数据。应先确认正确远端与历史，再在项目根建立或恢复版本管理，避免直接移动未知 `.git`。
3. 增加连接 MySQL、Redis 的集成测试，以及登录—审核—发布—下架—注销的浏览器端到端测试；接入 CI 做构建、测试和敏感信息扫描。
4. 为 Cookie 认证增加明确的 CSRF 防护策略，并在 Nginx 补齐 CSP、HSTS、Referrer-Policy、Permissions-Policy 等响应头。当前 `SameSite=Lax` 是基础防线，不应视为完整 CSRF 方案。
5. 对上传增加更完整的 PDF/图片解析、恶意内容扫描和图像像素上限；当前 PDF 只验证签名，图片解码仍需防范畸形文件和解压炸弹。
6. 为“文件已写入但数据库事务失败”的投稿/发布路径增加补偿删除或定期孤儿文件清理任务。
7. 给简历和图片增加版本化备份或对象存储。当前简历只保留最新文件，应用内没有历史回滚。
8. 增加结构化日志、健康检查、指标、告警、备份恢复演练和 Redis 故障策略。
9. 若需要更强的搜索引擎收录与社交分享预览，增加 sitemap、canonical，并考虑静态预渲染或 SSR；客户端动态 meta 对部分爬虫仍有限制。

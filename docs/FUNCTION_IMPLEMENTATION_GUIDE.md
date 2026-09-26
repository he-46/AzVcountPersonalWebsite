# AzV 个人网站：从零读懂功能实现

> 面向第一次接触 Vue、Spring Boot 和前后端分离项目的读者。本文以 **2026-09-26 的仓库代码**为准，讲的是“代码现在怎样工作”，不是理想化的设计稿。建议先读第 1～4 章，再按自己感兴趣的功能跳读。

## 目录

1. [先建立一张全局地图](#1-先建立一张全局地图)
2. [必备概念：一次请求经过了什么](#2-必备概念一次请求经过了什么)
3. [从浏览器打开首页开始](#3-从浏览器打开首页开始)
4. [数据模型：所有功能共用的地基](#4-数据模型所有功能共用的地基)
5. [前端的公共骨架](#5-前端的公共骨架)
6. [文章列表、搜索与图片墙](#6-文章列表搜索与图片墙)
7. [文章详情、Markdown、浏览与点赞](#7-文章详情markdown浏览与点赞)
8. [评论：提交、审核和分页展示](#8-评论提交审核和分页展示)
9. [投稿与图片文件](#9-投稿与图片文件)
10. [举报和内容下架](#10-举报和内容下架)
11. [站长登录与后台权限](#11-站长登录与后台权限)
12. [后台各功能怎样工作](#12-后台各功能怎样工作)
13. [作品集与 PDF 简历](#13-作品集与-pdf-简历)
14. [四套视觉风格的实现](#14-四套视觉风格的实现)
15. [错误处理、配置与部署](#15-错误处理配置与部署)
16. [如何测试与调试](#16-如何测试与调试)
17. [接口速查表](#17-接口速查表)
18. [已知边界和进阶练习](#18-已知边界和进阶练习)

## 1. 先建立一张全局地图

这个网站分成三个主要部分：浏览器里的 Vue 前端、处理业务的 Spring Boot 后端、保存数据的 MySQL/Redis/文件系统。前端负责显示和交互；后端负责校验、权限和数据规则。不能因为页面上隐藏了某个按钮，就认为接口安全——后端必须独立检查每一次请求。

```text
访客浏览器
  ├─ 请求网页、执行 Vue、点击按钮
  └─ HTTP /api/**
       ↓
开发环境：Vite 代理           生产环境：Nginx 反向代理
       ↓
Spring Boot Controller  ← AdminInterceptor 保护 /api/admin/**
       ├─ MyBatis-Plus Mapper → MySQL：文章、评论、用户、举报等
       ├─ StringRedisTemplate → Redis：限流、点赞判重、JWT 撤销
       ├─ FileStorageService → 图片目录
       └─ ResumeService → 当前的 resume.pdf
```

阅读源码时可先记住下面的“目录词典”：

| 目录或文件 | 它的职责 | 新手可先看 |
| --- | --- | --- |
| [`frontend/src/pages`](../frontend/src/pages) | 每个公开页面和后台页面的 Vue 组件 | `Home.vue`、`PostDetail.vue` |
| [`frontend/src/api/index.js`](../frontend/src/api/index.js) | 前端所有接口调用的集中入口 | `listContent`、`postComment` |
| [`frontend/src/router/index.js`](../frontend/src/router/index.js) | URL 到页面的映射及前端路由守卫 | `/post/:id`、`/admin` |
| [`backend/src/main/java/com/azv/controller`](../backend/src/main/java/com/azv/controller) | 接收 HTTP 请求并执行用例 | `ContentController`、`CommentController` |
| [`backend/src/main/java/com/azv/service`](../backend/src/main/java/com/azv/service) | 可复用业务逻辑 | `ResumeService`、`ContentMediaService` |
| [`backend/src/main/java/com/azv/mapper`](../backend/src/main/java/com/azv/mapper) | MyBatis-Plus 数据访问 | `ContentMapper` |
| [`backend/sql/schema.sql`](../backend/sql/schema.sql) | 新数据库的表结构及种子数据 | `content` 表 |
| [`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml) | 开发配置与环境变量占位符 | 数据库、Redis、JWT |

项目的入口分别是 [`frontend/src/main.js`](../frontend/src/main.js) 和 [`AzvApplication.java`](../backend/src/main/java/com/azv/AzvApplication.java)。运行条件、Nginx 示例和常见命令见根目录 [`README.md`](../README.md)。

## 2. 必备概念：一次请求经过了什么

假设访客打开 `/post/7`：

1. Vue Router 根据 `/post/:id` 找到 `PostDetail.vue`；这里的 `7` 是路径参数。
2. 页面调用 `api.getContent(7)` 和 `api.listComments(7, {page:1,size:20})`。
3. `api/index.js` 把调用交给 Axios，Axios 使用统一的 `/api` 前缀，于是发出 `GET /api/content/7` 和 `GET /api/content/7/comments?page=1&size=20`。
4. Spring MVC 把路径映射到 `ContentController` 的方法。Controller 调用 Mapper 查询 MySQL，检查内容已审核、不是评论，最后返回 DTO。
5. 后端统一响应形状为 `R<T>`；前端响应拦截器取出其中的 `data`，页面给响应式变量赋值，Vue 自动更新界面。

一个成功响应的形状大致是：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "content": { "id": 7, "title": "示例文章" },
    "images": []
  }
}
```

`R<T>` 的 `T` 表示 `data` 的类型可以变化：列表用分页对象、详情用详情对象、成功但无需返回内容时用 `null`。当前大多数业务错误仍以非零 `code` 放在响应体里；前端 [`http.js`](../frontend/src/api/http.js) 把它转换成 JavaScript `Error`，供页面的 `catch` 显示。未登录的后台请求由拦截器直接返回 HTTP 401/403，上传总体积超限可返回 413。不要只看 HTTP 状态码，也要看 `code`。

三个常见名词：**Controller** 是 HTTP 入口，**Service** 是可复用的业务规则，**Mapper** 是访问数据库的对象。MyBatis-Plus 的 `BaseMapper` 已提供 `selectById`、`insert`、`updateById` 等方法，本项目也在 `ContentMapper` 中写了少量原子更新 SQL。

## 3. 从浏览器打开首页开始

前端启动时，[`main.js`](../frontend/src/main.js) 创建 Vue 应用，安装 Pinia（状态容器）和 Vue Router，导入基础 CSS 与四风格 CSS，然后初始化当前风格。根组件 [`App.vue`](../frontend/src/App.vue) 根据路由元数据选用 `FrontLayout` 或 `AdminLayout`，其中 `<router-view />` 显示实际页面。公开页有公共导航和页脚，后台页有管理导航。

[`router/index.js`](../frontend/src/router/index.js) 使用 history 路由，因此网址是 `/about`，不是 `/#/about`。生产 Nginx 必须把非 API 的未知路径回退到 `index.html`，否则直接刷新 `/about` 会出现服务器 404。路由还设置页面标题、description 和 robots。后台路由带 `requiresAuth`；其前端守卫只检查 `localStorage` 中的显示标记，**真正的权限判定始终在后端**。

开发时 Vite 监听 `5173`，并把 `/api`、`/uploads` 转发到本机 `8080`，配置位于 [`frontend/vite.config.js`](../frontend/vite.config.js)。因此浏览器只访问前端开发地址，也能调用后端，不必在每个组件里写完整的后端主机名。

## 4. 数据模型：所有功能共用的地基

建表语句见 [`backend/sql/schema.sql`](../backend/sql/schema.sql)。理解表关系后，后面的代码会容易很多：

```text
user                  站长账号
content               文章 / 图片类内容 / 评论（由 type 区分）
  ├─ content_image     一篇内容关联的多张图片，content_id → content.id
  └─ content.parent_id 评论所属文章，parent_id → 原帖 content.id
report                举报，content_id → 被举报 content.id
sensitive_word         敏感词库
portfolio              作品集项目
```

`content` 是核心表，而不是“文章表 + 评论表”两张表。`type` 可为 `POST`、`IMAGE`、`COMMENT`；当前访客投稿和站长发布均写成 `POST`，图片作为文章附件保存，旧的 `IMAGE` 类型仍在枚举与部分后台筛选项中。`source` 区分 `ADMIN` 与 `SUBMIT`。`status` 的含义如下：

| 状态 | 含义 | 公开页面能看到吗 |
| --- | --- | --- |
| `PENDING` | 待站长审核 | 不能 |
| `APPROVED` | 已审核发布 | 能，仍须符合具体接口的类型条件 |
| `REJECTED` | 审核驳回 | 不能 |
| `DELETED` | 隐藏或删除 | 不能 |

评论的 `parent_id` 当前**只指向文章 ID**。后端禁止把评论作为被评论目标，前端也只是平铺显示，所以现在没有“评论树”或楼中楼。想做回复，需要重新设计“评论回复谁”、查询树和 UI；仅有 `parent_id` 字段不代表功能已经完成。

`content_image` 记录原图 URL、缩略图 URL 和排序。`content.media_url`、`thumbnail_url` 保存第一张图，兼容封面显示；详情页再读取全部图片。图片实体文件不在 MySQL 中，而是在 `UPLOAD_DIR`，数据库存访问路径。`idx_comment_feed` 索引帮助按文章读取已审核评论；**已有库必须单独执行** [`20260926_comment_index.sql`](../backend/sql/migrations/20260926_comment_index.sql)，`CREATE TABLE IF NOT EXISTS` 不会自动为旧表加索引。

## 5. 前端的公共骨架

页面通常用 Vue 的 `ref` 表示可变化的状态。例如 `loading = ref(true)`，请求完成后改为 `false`，模板中的 `v-if="loading"` 就从加载提示切换为内容。`computed` 用于从现有状态推导值，例如总页数或简历预览 URL。`onMounted` 表示组件进入页面后执行一次；`watch` 可在文章 ID 改变时重新加载文章。`v-model` 把表单输入与变量连接。

所有 HTTP 调用经过 [`api/index.js`](../frontend/src/api/index.js)，它再使用 [`api/http.js`](../frontend/src/api/http.js) 的 Axios 实例。这样页面不需要重复写 `/api`、超时、错误解包和登录过期处理。当前普通请求默认超时 10 秒，PDF 上传单独放宽到 120 秒。

组件内一般将状态分为 `loading`、`error`、`data` 三组：开始时显示加载，失败时显示错误和重试按钮，成功时显示真实数据。比如 [`Gallery.vue`](../frontend/src/pages/Gallery.vue) 用 `items`、`loading`、`error`、`page`、`hasMore` 管理图库。**客户端错误提示只是体验层；服务端仍必须校验输入。**

## 6. 文章列表、搜索与图片墙

### 6.1 首页文章流

[`Home.vue`](../frontend/src/pages/Home.vue) 首次加载时请求 `GET /api/content/list?page=1&size=8&type=POST`。后端 [`ContentController.list`](../backend/src/main/java/com/azv/controller/ContentController.java) 必须满足 `status=APPROVED`、`type≠COMMENT`，再根据请求的 `type` 筛选。结果按 `sort_weight` 和创建时间倒序；所以置顶内容会排在普通内容前。

MyBatis-Plus 分页对象包含 `records`（当前页）、`total`（总数）、`current`（页号）和 `pages`（总页数）。点击“加载更多”时，前端请求下一页，按 ID 去重后追加；这比一次加载全部文章省流量。列表使用 [`PublicContentSummaryDto`](../backend/src/main/java/com/azv/dto/PublicContentSummaryDto.java)，只提供 180 字左右的纯文本摘要，不传完整 Markdown，也不暴露 IP、审核人等内部字段。

搜索仍走同一个列表接口，只多一个 `keyword`。后端对标题、正文、作者标签做 `LIKE` 匹配，首页只展示第一页的 20 条结果。它不是全文索引搜索：数据量变大时需要另外考虑全文索引或搜索服务。

### 6.2 图片墙

[`Gallery.vue`](../frontend/src/pages/Gallery.vue) 请求同一列表接口，但增加 `withMedia=true`。后端据封面、缩略图或 `content_image` 记录筛选有图的内容；前端再取可用的图片 URL，使用 CSS 网格展示。每页取 12 条，支持继续加载。首页右侧的图库预览也借用这一接口，最多展示四张图。

要注意：`withMedia=true` 是“这篇内容有媒体”的数据库筛选，不是独立的“图片对象”类型，也不会绕开审核状态。无图内容不会进入图库，有图但未通过审核的内容也不会公开。

## 7. 文章详情、Markdown、浏览与点赞

访问 `/post/:id` 时，[`PostDetail.vue`](../frontend/src/pages/PostDetail.vue) 并行请求详情和第一页评论。后端详情方法先确认记录存在、已审核且不是评论，再用 `ContentMapper.incrementViewCount` 执行数据库原子 `UPDATE`。这里“原子”表示多个访客同时打开文章时，不会因为先读后写互相覆盖浏览数。更新条件里仍检查状态，避免刚下架的内容继续计数或公开。随后读取图片表，返回 `{content, images}`。

文章正文存的是 Markdown 文本。前端 [`MarkdownView.vue`](../frontend/src/components/MarkdownView.vue) 使用 markdown-it 生成 HTML，并设置 `html:false`，让用户文本中的原始 HTML 不被直接执行。Vue 的 `v-html` 仍应谨慎使用；当前安全性建立在渲染器禁用原始 HTML、内容先审核等约束上。

点赞走 `POST /api/like/{contentId}`。后端先检查目标是已审核的非评论内容，再用 Redis Set 记录这个 IP 是否给该内容点过赞：第一次加入集合返回成功，重复加入返回“已经点过赞”。随后数据库用原子 `UPDATE` 给 `like_count` 加一。如果数据库更新失败，代码会尝试从 Redis Set 移除本次 IP 标记，允许重试。**这不是账号级别点赞**：同一公网 IP 的多人可能互相影响，IP 变化也可能重复点赞；Redis 判重记录当前没有自动过期设置。

## 8. 评论：提交、审核和分页展示

评论的最重要规则是**先审后发**：投稿者按下提交，不等于全网可见。

```text
文章页输入评论（最多 500 字）
  → POST /api/comment，JSON 请求体
  → 服务端校验长度、按 IP 限流、检查敏感词和目标文章
  → content 新增一行：type=COMMENT、parent_id=文章 ID、status=PENDING
  → 后台审核通过：status=APPROVED
  → GET /api/content/{id}/comments 才能查到它
```

前端调用见 [`PostDetail.vue`](../frontend/src/pages/PostDetail.vue)，后端提交见 [`CommentController`](../backend/src/main/java/com/azv/controller/CommentController.java)。Redis 的 `comment:limit:<ip>` 计数同一 IP 每小时最多 10 条。敏感词来自 [`SensitiveWordService`](../backend/src/main/java/com/azv/service/SensitiveWordService.java)：启动时从表中加载，后台增删词后刷新内存列表。当前匹配算法只是 `text.contains(word)`，不是 AI 审核，也不能识别所有变体或语境。即使命中过滤器以外的风险内容，人工审核仍是最后关口。

公开评论接口只查询 `parent_id=文章 ID`、`type=COMMENT`、`status=APPROVED`，按创建时间和 ID 升序，默认每页 20 条、最多 50 条，并转换成 [`PublicCommentDto`](../backend/src/main/java/com/azv/dto/PublicCommentDto.java)。页面上的“加载更多评论”取下一页。审核队列位于 [`AuditController`](../backend/src/main/java/com/azv/controller/admin/AuditController.java)：通过后记录审核人和时间，驳回则变为 `REJECTED`。

## 9. 投稿与图片文件

[`Submit.vue`](../frontend/src/pages/Submit.vue) 把标题、正文、昵称和最多三张图片放入 `FormData`；这是一种 `multipart/form-data` 请求，适合文字与文件一起上传。前端检查类型、大小并用浏览器生成临时预览 URL；这些检查只是提前提示，后端 [`SubmitController`](../backend/src/main/java/com/azv/controller/SubmitController.java) 仍独立校验。

后端流程按顺序是：按 IP 每小时最多五次；正文不能为空且最多 60000 字、标题最多 120 字；检查敏感词；逐张交给 [`LocalFileStorageImpl`](../backend/src/main/java/com/azv/storage/LocalFileStorageImpl.java)；创建 `PENDING` 的 `POST`；再插入 `content_image`。图片原图以按年月分类的随机文件名保存，不直接使用上传者提供的文件名，避免文件名冲突和路径穿越。可解码格式在写盘前检查字节数和像素尺寸，生成宽度最多 300 像素的 JPEG 缩略图；每张最多 10 MB。WebP 在服务器没有解码器时可保存原图，但不会生成缩略图，也无法执行解码后的像素检查。

这里有两个不同的“事务”：MySQL 事务可撤销数据库写入，但**不能自动撤销磁盘文件**。因此上传代码在异常或数据库事务回滚时补偿删除本次写入的图片；若进程在关键时刻突然崩溃，仍可能留下孤儿文件，生产上需要定期对账清理。与此相反，审核驳回或后台删除时，[`ContentMediaService`](../backend/src/main/java/com/azv/service/ContentMediaService.java) 先清理数据库引用，等事务真正提交成功后再物理删除图片，避免“数据库回滚了，但图片已经删了”。

公开图片 URL 使用 `/uploads/**`。开发环境由 Spring 静态资源映射提供，生产示例由 Nginx 直接读取图片目录。这个目录需要持久化；重新部署 JAR 或前端 `dist` 不应该清空它。

## 10. 举报和内容下架

文章页填写举报原因后发送 JSON 到 `POST /api/report`。后端 [`ReportController`](../backend/src/main/java/com/azv/controller/ReportController.java) 检查原因非空、最多 255 字，按 IP 每小时最多 10 次，并确认被举报内容仍是已审核的非评论内容。随后在 `report` 表创建 `OPEN` 记录。

后台 [`ReportAdminController`](../backend/src/main/java/com/azv/controller/admin/ReportAdminController.java) 读取待处理举报。站长可以选择“忽略”，将状态变为 `IGNORED`；也可以“下架”，清理内容媒体、将内容状态改成 `DELETED`，再把举报状态改成 `HANDLED`。这些是站长操作，系统目前**没有**“被举报若干次自动下架”的规则；举报也不覆盖评论内容。

## 11. 站长登录与后台权限

登录过程由 [`Login.vue`](../frontend/src/pages/Login.vue)、[`AuthController`](../backend/src/main/java/com/azv/controller/AuthController.java) 和 [`JwtUtil`](../backend/src/main/java/com/azv/security/JwtUtil.java) 合作完成：

1. 浏览器把用户名、密码放在 JSON 请求体，发送到 `/api/auth/login`；不要把密码放在 URL。
2. 后端按解析出的 IP 和用户名分别查 Redis 失败计数，达到五次会暂时锁定 15 分钟。
3. 后端查 `user` 表，要求账号启用，并用 BCrypt 校验输入密码与 `password_hash`；数据库不保存明文密码。
4. 成功后 JWT 包含用户 ID、角色、签发和过期时间及唯一 `jti`，服务器将它设置成 `azv_token` Cookie。Cookie 使用 `HttpOnly`、`SameSite=Lax`；生产配置启用 `Secure`。
5. 浏览器以后调用 `/api/admin/**` 会自动携带 Cookie。`AdminInterceptor` 验证签名、过期时间、`ADMIN` 角色和 Redis 撤销标记，才放行后台接口。
6. 登出时后端按令牌剩余寿命在 Redis 写 `jwt:revoked:<jti>`，并清除 Cookie。旧令牌即使未自然过期，也会被后台拦截。

[`auth.js`](../frontend/src/stores/auth.js) 的 `azv_authed` **只是界面标记**，不是凭据；手动修改它不能通过后台拦截器。`JWT_SECRET` 必须由环境变量提供，至少 32 字节；更换密钥后旧令牌无法验证，站长需要重新登录。项目还没有完整的 CSRF 令牌方案，`SameSite=Lax` 只是基础防线；公网部署要结合反向代理与同源策略评估。

## 12. 后台各功能怎样工作

后台所有接口都以 `/api/admin/` 开头，因此受 `WebMvcConfig` 注册的 `AdminInterceptor` 保护。页面本身也有前端守卫，但安全依赖后端。主要页面与接口的关系如下：

| 页面 | 后端功能 | 关键规则 |
| --- | --- | --- |
| [`AdminHome.vue`](../frontend/src/pages/admin/AdminHome.vue) | `/api/admin/audit/stats` | 待审评论、待审投稿、待处理举报、内容总数 |
| [`Audit.vue`](../frontend/src/pages/admin/Audit.vue) | `/api/admin/audit/**` | 只看 `PENDING`；通过为 `APPROVED`，驳回为 `REJECTED` |
| [`Content.vue`](../frontend/src/pages/admin/Content.vue) | `/api/admin/content/**` | 按类型、状态和关键字查；置顶、隐藏、删除 |
| [`Reports.vue`](../frontend/src/pages/admin/Reports.vue) | `/api/admin/reports/**` | 处理 `OPEN` 举报：下架或忽略 |
| [`Publish.vue`](../frontend/src/pages/admin/Publish.vue) | `/api/admin/publish/post` | 站长发帖直接 `APPROVED`，可附最多三张图片 |
| [`Resume.vue`](../frontend/src/pages/admin/Resume.vue) | `/api/admin/resume` | 上传或替换当前 PDF |

“隐藏”只是把内容设为 `DELETED`，保留数据库记录和媒体；“删除”还会清理图片文件和引用。置顶把 `sort_weight` 设成 1，公开列表会优先显示。待审统计的 `pendingImage` 是为兼容旧前端保留的字段名，实际统计的是待审 `POST` 投稿。

后端另外有 [`WordAdminController`](../backend/src/main/java/com/azv/controller/admin/WordAdminController.java) 用于敏感词增删、[`PortfolioAdminController`](../backend/src/main/java/com/azv/controller/admin/PortfolioAdminController.java) 用于作品集管理。项目经历管理现已集成在“简历管理”页；敏感词管理仍没有专门页面。前端 API 文件中还保留 `adminPublishImage` 方法，但本轮代码未发现与它对应的 `/api/admin/publish/image` 后端接口，不要把它当作可用功能。站长图片发布应使用已有的 `/api/admin/publish/post` 并附带图片。

## 13. 作品集与 PDF 简历

公开 `/about` 页通过 `GET /api/portfolio` 读取状态为 `APPROVED` 的项目，按 `sort_order` 升序显示。项目的标题、简介、技术栈、链接等来自 `portfolio` 表；页面会检查项目链接协议后再显示为可点击链接。站长在 `/admin/resume` 的“项目经历管理”中可以新增、编辑、隐藏和删除项目。表单参数由前端 API 传给 `PortfolioAdminController`，成功后重新获取管理列表；只有 `APPROVED` 的项目出现在公开页。删除是数据库硬删除，操作前会二次确认。

简历不是放在前端构建包内的固定 PDF，而是单独保存在 `RESUME_STORAGE_DIR/resume.pdf`。公开接口：`GET /api/resume` 返回是否可用、大小、更新时间与预览/下载 URL；`GET /api/resume/file` 内嵌预览；加 `?download=true` 以附件下载。`About.vue` 用 `<iframe>` 在页面中预览，提供下载和新窗口查看。服务端响应设置 PDF 类型、下载方式、ETag、Last-Modified、`nosniff` 等头。

站长在 `/admin/resume` 选文件后，前端先用浏览器本地临时 URL 展示待上传预览，再发送 `multipart/form-data` 到 `POST /api/admin/resume`。[`ResumeService`](../backend/src/main/java/com/azv/service/ResumeService.java) 限制最大 20 MB、检查 `%PDF-` 文件头，把新文件写到同一目录的临时文件，最后原子移动替换 `resume.pdf`。校验或写入失败保留旧版本。替换成功后公开页重新请求元数据，就能使用新版，无需重新打包前端。当前只保留**最新一版**，没有应用内历史回滚；重要简历应另做备份。

## 14. 四套视觉风格的实现

风格切换不是请求后端。[`theme.js`](../frontend/src/stores/theme.js) 保存 `editorial`（书页）、`studio`（工作室）、`zine`（杂志）、`terminal`（终端）的名称；[`ThemeSwitcher.vue`](../frontend/src/components/ThemeSwitcher.vue) 提供选择菜单。选中后把 `data-style` 设置到 `<html>`，并存入浏览器 `localStorage` 的 `azv-style`。再次打开网站时恢复选择；旧的 `azv-theme` 值也会映射到新风格。

[`main.css`](../frontend/src/styles/main.css) 是通用组件和基础样式，[`style-systems.css`](../frontend/src/styles/style-systems.css) 用 `:root[data-style="..."]` 覆盖颜色、字体、圆角、版式。书页是安静的文章网格，工作室使用柔和的模块卡片，杂志使用粗边框与海报排版，终端在宽屏采用侧边导航。移动端通过媒体查询降为单列或顶部导航。风格只影响视觉，不改变数据库中的文章和审核状态；后台界面共享部分全局视觉变量，但终端侧栏只作用于公开页面的 `front-shell`。

## 15. 错误处理、配置与部署

[`GlobalExceptionHandler`](../backend/src/main/java/com/azv/common/GlobalExceptionHandler.java) 把 `BizException` 转成可读的业务提示；其他异常记入服务端日志，对访客只返回通用信息，避免泄露数据库或文件路径。Spring 启动时会构建 Bean：敏感词服务的 `@PostConstruct` 立即读取 MySQL，因此数据库不可连接时，应用会在启动阶段失败，而不是等第一个访客请求才失败。

[`application.yml`](../backend/src/main/resources/application.yml) 从环境变量读取数据库、Redis、JWT、目录和端口，并尝试导入当前目录或上一级的 `.env`。开发默认数据库端口是 `3307`；[`application-prod.yml`](../backend/src/main/resources/application-prod.yml) 的生产示例端口是 `3306`。**端口不是凭据，密码与 JWT 密钥不要写进仓库**。生产 Profile 把后端绑定到 `127.0.0.1`，由同机 Nginx 对外提供 HTTPS 与反向代理。`TRUST_FORWARDED_HEADERS=true` 只适合可信代理覆盖转发头且后端无法被公网直连的拓扑，否则 IP 限流可能被伪造。

Windows 根目录的 [`start.bat`](../start.bat) 会分别启动后端与前端开发服务器；它只是开发便利脚本，不会自动启动 MySQL/Redis，也不是生产部署脚本。部署步骤与 Nginx 示例请以 [`README.md`](../README.md) 为准。`backend/target`、`frontend/dist`、运行时上传目录、`.env` 都被 `.gitignore` 排除；提交代码前仍应检查暂存区是否意外包含秘密。

## 16. 如何测试与调试

推荐按“最小一环”逐层排查，而不是看到前端报错就立刻改页面：

```text
浏览器 Network：URL、方法、请求体、HTTP 状态、R.code/message
    ↓
后端日志：是否进入 Controller？是否被 AdminInterceptor 拒绝？
    ↓
数据库：记录的 type/status/parent_id 是否正确？
    ↓
Redis：限流键、点赞集合、JWT 撤销键是否按预期变化？
    ↓
文件目录：图片或 resume.pdf 是否真实存在且权限正确？
```

例如“评论提交成功却看不到”，第一步不是怀疑 Vue：检查该行 `status` 是否为 `PENDING`。如果是，就说明先审后发在正常工作。又如“网站启动时 `SensitiveWordService` 报错”，应沿着数据库 URL、端口、用户名、密码、服务状态和权限查，而不是怀疑评论页面。若访问 `/about` 能读项目却没有 PDF，先请求 `/api/resume` 看 `available` 和 URL，再查 `RESUME_STORAGE_DIR`。

后端可在 `backend` 执行 `mvn test`；已有的测试覆盖 JWT、IP 解析、PDF 原子替换、公开 JSON 接口、评论分页与图片上传回滚等。前端在 `frontend` 执行 `npm run build`；这能检查 Vue 模板与生产打包，但不等于真实浏览器端到端测试。改数据库之前先备份；尤其新评论索引需要单独迁移已有库，见 [`2026-09-26-code-fixes.md`](./2026-09-26-code-fixes.md)。

## 17. 接口速查表

下表列的是当前主要可用接口；除 PDF 文件响应外，业务接口一般包在 `R<T>` 中。`/api/admin/**` 全部需要管理员 Cookie。

| 方法和路径 | 用途 | 请求/返回要点 |
| --- | --- | --- |
| `GET /api/content/list` | 首页、搜索、图库 | `page,size,type,keyword,withMedia`；返回分页摘要 |
| `GET /api/content/{id}` | 详情与浏览数 | 返回 `{content,images}` |
| `GET /api/content/{id}/comments` | 已审核评论 | `page,size`；返回分页公开 DTO |
| `POST /api/comment` | 访客评论 | JSON `{contentId,text,nickname}`；写入 `PENDING` |
| `POST /api/submit` | 访客投稿 | multipart：`title,body,nickname,files`；写入 `PENDING` |
| `POST /api/like/{id}` | 点赞 | Redis 按 IP 判重，返回计数 |
| `POST /api/report` | 举报 | JSON `{contentId,reason}`；写入 `OPEN` |
| `GET /api/portfolio` | 公开作品集 | 只读已发布项目 |
| `GET /api/resume`、`/file` | 简历元数据/预览 | `/file?download=true` 下载 |
| `POST /api/auth/login`、`/logout` | 登录/注销 | 登录 JSON；JWT Cookie |
| `GET /api/admin/audit/list` | 待审队列 | 按类型分页 |
| `POST /api/admin/audit/approve/{id}`、`reject/{id}` | 审核 | 写状态与审核记录 |
| `GET /api/admin/content/list` | 内容管理列表 | 类型、状态、关键字筛选 |
| `POST /api/admin/content/{id}/pin`、`hide`、`delete` | 内容操作 | 置顶、隐藏、清理媒体后删除 |
| `GET /api/admin/reports/list`、`POST .../{id}/take-down`、`ignore` | 举报处理 | 下架或忽略 |
| `POST /api/admin/publish/post` | 站长发布 | multipart，直接 `APPROVED` |
| `POST /api/admin/resume` | 替换 PDF | multipart 字段 `file` |

## 18. 已知边界和进阶练习

学习项目时要区分“现在能做什么”和“下一步值得做什么”：

1. **没有 AI 审核，也没有自动放行评论。** 现有敏感词为简单子串匹配，正常访客评论都进入待审。可以先设计风险分级，但 AI 故障时必须保持待审。
2. **评论不是树。** 当前只有“文章 → 单层评论列表”，没有回复按钮、评论作为父节点的校验或递归渲染。
3. **已有后台接口未必有页面。** 敏感词管理还没有专门 UI；作品集已纳入简历管理页；`adminPublishImage` 前端方法没有对应的后端路径。
4. **图片与数据库不是真正的单一事务。** 补偿清理能覆盖普通异常与回滚，进程崩溃仍可能留下孤儿文件；站长发布图片路径也值得统一复用同样的补偿策略。
5. **安全仍有进阶空间。** Cookie 方案可增加明确的 CSRF 防护；对 WebP 做完整解码和尺寸验证；给管理员列表限制最大分页大小；增加审核通知和举报应急开关。
6. **真实上线验收不能被单元测试替代。** 建议在测试库和浏览器中完整走一遍“投稿 → 审核 → 公开 → 举报 → 下架”，并检查图片、Redis 与日志。

可以按下面顺序动手练习，每一题都能在现有代码里找到入口：

- 练习 A：从首页点击一篇文章，用浏览器开发工具写出从 Vue 方法到 Controller、Mapper、MySQL 的完整调用链。
- 练习 B：在测试环境投稿一篇带两张图的内容，分别观察 `content`、`content_image`、磁盘文件和审核队列。不要用生产数据练习。
- 练习 C：画出评论从 `PENDING` 到 `APPROVED` 的状态图，思考为什么“提交成功”不代表“公开成功”。
- 练习 D：模拟错误的数据库密码与关闭的 Redis，比较启动阶段和请求阶段的错误表现；不要把真实密码写入测试代码或截图。
- 练习 E：为后台列表加分页大小上限、为敏感词管理补前端页面，并添加测试与文档，再检查与既有接口契约是否一致。

读完后，最重要的不是记住每个文件，而是能回答三个问题：**数据从哪里来、在哪一层被校验、什么时候才算真正公开。** 这三问能帮你读懂本项目，也能迁移到大多数前后端分离应用。

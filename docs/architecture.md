# PersonalBlog 项目架构文档

## 项目概览

一个前后端分离的个人博客系统，前端 Vue 3 + 后端 Spring Boot，支持文章管理、评论互动、AI 智能问答、DOCX 文档导入等功能。

---

## 前端技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue 3 | ^3.4.0 | Composition API + `<script setup>` |
| 路由 | Vue Router | ^4.3.0 | 前端路由管理 + SEO afterEach 钩子 |
| UI 组件库 | Element Plus | ^2.5.0 | 含 @element-plus/icons-vue 图标库 |
| HTTP 客户端 | Axios | ^1.6.0 | 封装统一请求/响应拦截器 |
| Markdown 渲染 | Marked | ^12.0.0 | 文章正文渲染 + 编辑器预览 |
| Markdown 高亮 | marked-highlight | ^2.2.4 | 编辑器预览区代码语法高亮 |
| 代码高亮 | highlight.js | ^11.11.1 | 文章详情代码块语法高亮 |
| 图片查看器 | viewerjs | ^1.11.7 | 文章图片点击放大、缩放、拖拽 |
| 构建工具 | Vite | ^5.1.0 | 开发热更新 + 生产构建 |

### 前端项目结构

```
frontend/
├── src/
│   ├── main.js                    # 应用入口
│   ├── App.vue                    # 根组件（含 BackToTop + AiChat）
│   ├── router/
│   │   └── index.js               # 路由配置（前台 + 后台路由守卫 + SEO 钩子）
│   ├── store/                     # 状态管理（响应式 store，非 Vuex/Pinia）
│   │   ├── user.js                # 用户登录状态 + 管理员权限判断
│   │   ├── theme.js               # 深色/浅色主题切换（localStorage 持久化）
│   │   └── site.js                # 站点级状态（站长信息、favicon、登录背景）
│   ├── utils/
│   │   ├── request.js             # Axios 实例（Bearer Token 拦截器 + 统一错误处理）
│   │   ├── api.js                 # 全部 API 封装（30+ 函数，含 DOCX 上传）
│   │   └── seo.js                 # SEO 管理器（动态 title/meta/OG/Twitter Card）
│   ├── composables/
│   │   └── useRevealOnScroll.js   # 滚动触发动画 composable（复用于多个页面）
│   ├── styles/
│   │   └── global.css             # 全局样式 + 主题变量 + 暗黑模式 + 共享组件样式
│   ├── components/                # 公共组件
│   │   ├── NavBar.vue             # 顶部导航栏（毛玻璃效果 + 响应式 + 深色模式）
│   │   ├── AiChat.vue             # AI 助手悬浮对话框（SSE 流式输出 + 会话管理）
│   │   ├── BackToTop.vue          # 回到顶部按钮
│   │   ├── ImageViewer.vue        # 图片查看器（viewerjs 封装，支持缩放/旋转/拖拽）
│   │   └── TableOfContents.vue    # 文章目录导航（TOC）
│   └── views/                     # 页面视图
│       ├── Home.vue               # 首页（文章列表 + 标签筛选 + 搜索 + 分页）
│       ├── ArticleDetail.vue      # 文章详情（Markdown 渲染 + 代码高亮 + 评论 + 点赞 + 图片放大）
│       ├── Archive.vue            # 归档页（时间线展示）
│       ├── About.vue              # 关于页（从后端动态加载）
│       ├── UserProfile.vue        # 个人资料设置
│       └── admin/                 # 后台管理页面
│           ├── Login.vue          # 管理员登录（自定义背景图 + 毛玻璃卡片）
│           ├── Dashboard.vue      # 后台布局壳（侧边栏 + 主内容区）
│           ├── ArticleList.vue    # 文章列表管理
│           ├── ArticleEditor.vue  # 文章编辑器（Markdown + 实时预览 + DOCX 导入 + 字数限制）
│           ├── CommentManage.vue  # 评论管理
│           ├── UserManage.vue     # 用户管理
│           └── AboutSettings.vue  # 关于页内容编辑（头像/favicon/登录背景）
├── index.html                     # HTML 入口（含 SEO 默认 meta 标签）
├── vite.config.js                 # Vite 配置（代理 /api → 后端 8080）
├── nginx.conf                     # Nginx 部署配置
└── package.json
```

---

## 后端技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 2.7.18 | Web + AOP + Validation |
| ORM | MyBatis-Plus 3.5.5 | 简化 CRUD、分页插件 |
| 数据库 | MySQL 8.0 | 主数据存储 |
| 缓存 | Redis 7 | 接口缓存（CacheConfig，TTL 30min） |
| 文件存储 | 阿里云 OSS | 图片上传（OssService） |
| 认证 | JWT 自定义 Token | JwtFilter 请求拦截 |
| AI 接入 | DashScope（阿里云百炼） | 通义千问流式对话 |
| 向量存储 | Pinecone | 文章向量检索（RAG） |
| AI 框架 | LangChain4j | Embedding + 向量检索封装 |
| 会话存储 | MongoDB | AI 聊天会话和消息记录 |
| 邮件 | Spring Mail | 评论回复通知 |
| DOCX 解析 | Apache POI 5.2.5 | Word 文档转 Markdown（DocxToMdUtil） |
| Markdown 解析 | CommonMark 0.21.0 | 服务端 Markdown → HTML |

### 后端项目结构

```
backend/src/main/java/com/blog/
├── BlogApplication.java                  # 启动类
├── common/                               # 通用组件
│   ├── R.java                            # 统一响应封装 {code, message, data}
│   ├── GlobalExceptionHandler.java       # 全局异常处理
│   └── MarkdownUtil.java                 # Markdown → HTML（CommonMark）
├── config/                               # Spring 配置
│   ├── SecurityConfig.java               # Spring Security 配置（URL 权限规则）
│   ├── JwtFilter.java                    # JWT 认证过滤器
│   ├── CorsConfig.java                   # 跨域配置
│   ├── WebMvcConfig.java                 # MVC 配置
│   ├── MyBatisPlusConfig.java            # MyBatis-Plus 分页插件
│   ├── MyMetaObjectHandler.java          # 自动填充 createTime/updateTime
│   ├── RedisConfig.java                  # Redis 序列化配置
│   └── CacheConfig.java                  # Redis 缓存管理器（TTL 30min）
├── controller/                           # 接口层
│   ├── ArticleController.java            # 文章列表、详情、归档、搜索（公开）
│   ├── CommentController.java            # 评论列表、发表评论（公开）
│   ├── LikeController.java               # 点赞/取消点赞（Redis Set）
│   ├── TagController.java                # 标签列表（公开）
│   ├── UploadController.java             # 文件上传（OSS，公开）
│   ├── AuthController.java               # 用户注册、登录、发送验证码
│   ├── AdminController.java              # 管理员：评论/用户/关于页 CRUD + 文件上传
│   ├── AdminArticleController.java       # 管理员：文章 CRUD + DOCX 上传解析
│   └── AdminTagController.java           # 管理员：标签创建（幂等）
├── service/                              # 业务逻辑层
│   ├── BlogArticleService.java           # 文章服务接口
│   ├── BlogCommentService.java           # 评论服务接口
│   ├── BlogTagService.java               # 标签服务接口（含 createTag 幂等方法）
│   ├── SysUserService.java               # 用户服务接口
│   ├── LikeService.java                  # 点赞服务（Redis Set）
│   ├── MailService.java                  # 邮件通知服务
│   ├── OssService.java                   # OSS 文件上传（MultipartFile + InputStream）
│   └── impl/
│       ├── BlogArticleServiceImpl.java   # 文章服务实现（含 Markdown → HTML 转换）
│       ├── BlogCommentServiceImpl.java   # 评论服务实现
│       ├── BlogTagServiceImpl.java       # 标签服务实现（@Cacheable + @CacheEvict）
│       └── SysUserServiceImpl.java       # 用户服务实现
├── mapper/                               # MyBatis-Plus Mapper 接口
│   ├── BlogArticleMapper.java
│   ├── BlogCommentMapper.java
│   ├── BlogTagMapper.java                # 含 selectTagsWithCount 自定义查询
│   ├── ArticleTagMapper.java             # 文章-标签关联表
│   ├── BlogAboutMapper.java              # 关于页内容
│   ├── BlogLikeMapper.java
│   └── SysUserMapper.java
├── entity/                               # 数据库实体
│   ├── BlogArticle.java                  # 文章表（content_md + content_html 双存储）
│   ├── BlogComment.java                  # 评论表（支持多级回复）
│   ├── BlogTag.java                      # 标签表
│   ├── ArticleTag.java                   # 文章-标签关联表
│   ├── BlogAbout.java                    # 关于页内容表
│   ├── BlogLike.java                     # 点赞表
│   └── SysUser.java                      # 用户表
├── dto/                                  # 数据传输对象
│   ├── ArticleListVo.java                # 文章列表响应（含 contentMd/contentHtml）
│   ├── ArticleDto.java                   # 文章创建/编辑请求
│   ├── CommentVo.java                    # 评论视图（含嵌套子评论）
│   ├── TagVo.java                        # 标签视图（含文章数）
│   ├── ArchiveVo.java                    # 归档视图（年/月分组）
│   ├── LoginDto.java                     # 登录请求
│   └── RegisterDto.java                  # 注册请求
├── util/
│   ├── JwtUtil.java                      # JWT 生成与解析
│   └── DocxToMdUtil.java                 # DOCX → Markdown 转换（Apache POI，558 行）
│                                         #   三级标题识别（样式名/大纲级别/字号+加粗+居中）
│                                         #   代码块检测（16 种等宽字体 + 14 种语言关键词匹配）
│                                         #   图片提取上传（复用 OssService）
└── ai/                                   # AI 模块（独立包）
    ├── config/
    │   ├── AiConfig.java                 # AI Bean 配置
    │   ├── DashScopeModel.java           # DashScope API 封装（Embedding + 流式对话）
    │   └── PineconeRestStore.java        # Pinecone 向量检索 REST 封装
    ├── controller/
    │   ├── ChatController.java           # AI 对话接口（SSE 流式输出 + RAG）
    │   └── ChatSessionController.java    # 会话管理接口（CRUD）
    ├── document/
    │   ├── ChatSessionDoc.java           # MongoDB 会话文档
    │   └── ChatMessageDoc.java           # MongoDB 消息文档
    ├── repository/
    │   ├── ChatSessionRepository.java    # 会话 Repository
    │   └── ChatMessageRepository.java    # 消息 Repository
    ├── loader/
    │   └── ArticleDataLoader.java        # 文章向量化加载器
    └── task/
        └── ChatCleanupTask.java          # 过期会话定时清理

backend/src/main/resources/
├── application.yml                       # 主配置文件
├── application-local.yml                 # 本地配置（含敏感信息，不提交）
├── application-local.yml.example         # 本地配置模板
├── init.sql                              # 数据库初始化（Docker MySQL 自动执行）
└── SystemMessage.txt                     # AI 助手系统提示词
```

---

## 前端核心模块详解

### 路由与 SEO

路由配置在 `router/index.js`，使用 `createWebHistory`。每个公开路由携带 `meta.seo` 对象（title、description），`afterEach` 钩子自动调用 `seo.js` 的 `updateSEO()` 更新页面标题、meta 描述、Open Graph、Twitter Card。管理员路由自动添加 `noindex, nofollow`。文章详情页使用 `dynamic: true` 标记，由组件自行设置 SEO 内容。

### 主题系统

`store/theme.js` 读取 `localStorage('theme')`，在 `<html>` 上同时设置 `class="dark"` 和 `data-theme="dark"`。`global.css` 通过 CSS 自定义属性（`--gray-50` ~ `--gray-900`、`--indigo-*`）统一管理颜色，`html.dark` 块覆盖暗色值。所有页面通过 `var(--gray-*)` 引用，切换主题时自动生效。

### 文章渲染流程

1. 后端存储 `content_md`（原始 Markdown）和 `content_html`（CommonMark 转换的 HTML）
2. 前端 `ArticleDetail.vue` 通过 `v-html` 渲染 `contentHtml`
3. `enhanceContent()` 在 DOM 渲染后执行：用 `hljs.highlightElement()` 高亮代码块、添加行号和复制按钮、构建 TOC
4. `ImageViewer.vue`（viewerjs）绑定到 `.article-content` 容器，支持图片点击放大

### 编辑器预览

`ArticleEditor.vue` 使用 `marked` + `marked-highlight` + `highlight.js` 实现实时预览。`previewHtml` 是 `computed`，每次输入触发 Markdown 解析 + 语法高亮。支持 DOCX 文件上传（调用后端 `uploadDocx` 接口，由 `DocxToMdUtil` 解析后回填 Markdown）。

---

## 后端核心模块详解

### 文章双存储

文章同时存储 `content_md`（Markdown 原文）和 `content_html`（HTML 渲染结果）。创建/更新时通过 `MarkdownUtil.toHtml()` 转换。前端编辑器使用 `content_md`，详情页使用 `content_html`。

### DOCX 解析（DocxToMdUtil）

基于 Apache POI 的 `XWPFDocument` 解析 Word 文档，输出标准 Markdown。标题识别三级策略：
1. **内置标题样式**：匹配 `Heading1`~`Heading6` / `标题1`~`标题6`（样式 ID + 样式名双重匹配）
2. **大纲级别**：读取 XML 属性 `outlineLvl`（0~5）
3. **兜底**：字号 ≥ 16pt + 全部加粗 + 居中对齐 → 映射为 `##`

代码块检测：16 种等宽字体匹配 + 14 种编程语言关键词自动识别。图片提取后通过 `OssService.upload(InputStream)` 上传到 OSS，替换为 Markdown 图片语法。

### 标签服务

`BlogTagService.createTag(name)` 是幂等操作：先查同名标签是否存在，存在则直接返回，不存在才插入。使用 `@CacheEvict("tags")` 清除缓存。

### 点赞服务

`LikeService` 基于 Redis Set 实现。`SISMEMBER` 判断是否已赞，`SADD`/`SREM` 切换点赞状态，`SCARD` 统计点赞数。支持文章和评论两种目标类型。

### AI 模块

独立 `ai` 包，基于 LangChain4j + DashScope + Pinecone 实现 RAG：
- `ArticleDataLoader` 将文章内容向量化存入 Pinecone
- `ChatController` 对话时自动检索相关文章片段作为上下文
- 会话和消息存储在 MongoDB，支持多轮对话
- `ChatCleanupTask` 定时清理过期会话

---

## 数据库设计

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | username, password(BCrypt), nickname, role, avatar, email |
| `blog_article` | 文章表 | title, summary, cover_image, content_md, content_html, status, view_count |
| `blog_tag` | 标签表 | name (UNIQUE) |
| `article_tag` | 文章-标签关联 | article_id, tag_id (联合主键) |
| `blog_comment` | 评论表 | article_id, user_id, parent_id, reply_to_user_id, content |
| `blog_like` | 点赞表 | user_id, target_type, target_id (联合唯一) |
| `blog_about` | 关于页配置 | bio, tech_stack, github, twitter, email, favicon, login_bg |

---

## 部署架构

```
┌─────────────────────────────────────────────────────┐
│                   Docker Compose                    │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │  MySQL   │  │  Redis   │  │     MongoDB       │ │
│  │  3306    │  │  6380    │  │     27018         │ │
│  └────┬─────┘  └────┬─────┘  └────────┬──────────┘ │
│       │              │                 │            │
│  ┌────┴──────────────┴─────────────────┴──────────┐ │
│  │           Spring Boot Backend (8080)           │ │
│  └────────────────────┬───────────────────────────┘ │
│                       │                             │
│  ┌────────────────────┴───────────────────────────┐ │
│  │          Nginx + Vue SPA Frontend (80)         │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
         │                    │                    │
    ┌────┴─────┐        ┌────┴─────┐        ┌────┴─────┐
    │ 阿里云 OSS │        │ Pinecone │        │ DashScope │
    │ 文件存储   │        │ 向量数据库 │        │ AI 模型   │
    └──────────┘        └──────────┘        └──────────┘
```

---

## API 接口概览

### 公开接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles` | 文章列表（分页、标签筛选） |
| GET | `/api/articles/search` | 文章搜索 |
| GET | `/api/articles/archive` | 文章归档 |
| GET | `/api/articles/{id}` | 文章详情 |
| GET | `/api/tags` | 标签列表（含文章数） |
| GET | `/api/articles/{id}/comments` | 评论列表 |
| POST | `/api/articles/{id}/comments` | 发表评论 |
| POST | `/api/articles/{id}/like` | 点赞/取消 |
| POST | `/api/comments/{id}/like` | 评论点赞 |
| POST | `/api/login` | 用户登录 |
| POST | `/api/register` | 用户注册 |
| POST | `/api/send-code` | 发送邮箱验证码 |
| GET | `/api/about` | 关于页数据 |
| POST | `/api/upload/image` | 图片上传 |

### 管理员接口（需 JWT + ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/articles` | 文章管理列表 |
| POST | `/api/admin/articles` | 创建文章 |
| PUT | `/api/admin/articles/{id}` | 更新文章 |
| DELETE | `/api/admin/articles/{id}` | 删除文章 |
| POST | `/api/admin/articles/upload-docx` | DOCX 上传转 Markdown |
| POST | `/api/admin/tags` | 创建标签（幂等） |
| GET | `/api/admin/comments` | 评论管理列表 |
| DELETE | `/api/admin/comments/{id}` | 删除评论 |
| GET | `/api/admin/users` | 用户管理列表 |
| POST | `/api/admin/users` | 创建用户 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| GET | `/api/admin/about` | 获取关于页设置 |
| PUT | `/api/admin/about` | 更新关于页设置 |
| POST | `/api/admin/upload/avatar` | 上传头像 |
| POST | `/api/admin/upload/favicon` | 上传网站图标 |
| POST | `/api/admin/upload/login-bg` | 上传登录背景图 |

# PersonalBlog

## 项目概览

| 层 | 技术栈 | 版本 |
|---|---|---|
| 前端 | Vue 3 (Composition API) + Vite + Element Plus + Vue Router + Axios + Highlight.js | Vue 3.x, Vite 5 |
| 后端 | Spring Boot + MyBatis-Plus + Spring Security (JWT) + Redis | Java 17, Spring Boot 3.x |
| 数据库 | MySQL | 8.0 (utf8mb4) |
| 存储 | Aliyun OSS (头像/图标/背景图上传) | — |

## 项目结构

```
PersonalBlog/
├── frontend/                  # Vue 3 前端
│   ├── index.html             # 入口 HTML（含动态 favicon 占位）
│   ├── vite.config.js         # Vite 配置，@ 别名指向 src，proxy /api → :8080
│   └── src/
│       ├── main.js            # 引导：Vue + ElementPlus + Highlight.js
│       ├── App.vue            # 根组件：<router-view> + BackToTop + 动态 favicon 监听
│       ├── router/index.js    # 路由配置 + 导航守卫（token + admin 角色校验）
│       ├── store/             # 模块级响应式状态（非 Pinia/Vuex）
│       │   ├── user.js        # currentUser + setUser()，localStorage 持久化
│       │   ├── site.js        # siteOwner（昵称/头像/图标/背景等站点配置）
│       │   └── theme.js       # 暗色/亮色主题切换
│       ├── utils/
│       │   ├── api.js         # 所有后端 API 函数
│       │   └── request.js     # Axios 封装（baseURL、JWT 拦截器、401 跳转）
│       ├── components/        # NavBar、BackToTop、TableOfContents
│       ├── styles/global.css  # CSS 变量（--bg-primary, --text-primary 等）
│       └── views/
│           ├── Home.vue               # 首页：打字机 Hero + 标签云 + 文章列表 + 分页
│           ├── ArticleDetail.vue      # 文章详情 + 目录 + 评论区 + 点赞
│           ├── About.vue              # 关于页（动态展示站长信息/技术栈）
│           ├── Archive.vue            # 文章归档（年/月/日分组）
│           ├── UserProfile.vue        # 用户中心
│           └── admin/
│               ├── Login.vue          # 登录/注册（支持动态背景图）
│               ├── Dashboard.vue      # 后台布局（侧边栏 + router-view）
│               ├── ArticleList.vue    # 文章管理列表
│               ├── ArticleEditor.vue  # 文章新建/编辑（Markdown 编辑器）
│               ├── CommentManage.vue  # 评论审核管理
│               ├── UserManage.vue     # 用户管理（admin 专用）
│               └── AboutSettings.vue  # 站点设置（头像/昵称/个人简介/技术栈/联系方式/网站图标/登录背景）
├── backend/                   # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/blog/
│       │   ├── BlogApplication.java
│       │   ├── common/        # R.java (统一响应), MarkdownUtil.java, GlobalExceptionHandler
│       │   ├── config/        # SecurityConfig, JwtFilter, CorsConfig, RedisConfig, etc.
│       │   ├── controller/    # 9 个 Controller（下面详述）
│       │   ├── dto/           # LoginDto, RegisterDto, ArticleDto, ArticleListVo, etc.
│       │   ├── entity/        # SysUser, BlogArticle, BlogTag, BlogComment, BlogLike, BlogAbout
│       │   ├── mapper/        # MyBatis-Plus BaseMapper
│       │   ├── service/       # 服务接口 + impl/
│       │   └── util/          # JwtUtil
│       └── resources/
│           ├── application.yml
│           └── init.sql       # 建表 + 种子数据
```

## API 路由总览

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 无 | 登录，返回 JWT token + user |
| POST | `/api/auth/register` | 无 | 注册，需邮箱验证码 |
| GET | `/api/auth/captcha` | 无 | 获取图形验证码 (Redis) |
| POST | `/api/auth/send-code` | 无 | 发送邮箱验证码 (Redis) |
| GET | `/api/about` | 无 | 公开站点信息（nickname/avatar/bio/techStack/favicon/loginBg 等） |
| GET | `/api/articles` | 无 | 公开文章列表（分页 + 标签筛选） |
| GET | `/api/articles/{id}` | 无 | 文章详情（含上一篇/下一篇、浏览量+1） |
| GET | `/api/articles/{id}/comments` | 无 | 文章评论（树形结构） |
| POST | `/api/articles/{id}/comments` | 是 | 发表评论 |
| POST | `/api/articles/{id}/like` | 是 | 点赞/取消点赞 |
| GET | `/api/tags` | 无 | 标签列表 |
| GET | `/api/archive` | 无 | 文章归档（年→月→文章） |
| POST | `/api/upload/image` | 是 | 通用图片上传（OSS） |
| GET | `/api/user/profile` | 是 | 当前用户信息 |
| PUT | `/api/user/profile` | 是 | 修改个人信息 |
| GET | `/api/admin/about` | ADMIN | 获取站点设置 |
| PUT | `/api/admin/about` | ADMIN | 更新站点设置 |
| POST | `/api/admin/upload/avatar` | ADMIN | 上传头像 |
| POST | `/api/admin/upload/favicon` | ADMIN | 上传网站图标 |
| POST | `/api/admin/upload/login-bg` | ADMIN | 上传登录页背景 |
| GET | `/api/admin/articles` | ADMIN | 管理端文章列表 |
| POST | `/api/admin/articles` | ADMIN | 创建文章 |
| PUT | `/api/admin/articles` | ADMIN | 更新文章 |
| DELETE | `/api/admin/articles/{id}` | ADMIN | 删除文章 |
| GET | `/api/admin/comments` | ADMIN | 评论管理列表 |
| PUT | `/api/admin/comments/{id}` | ADMIN | 审核/操作评论 |
| DELETE | `/api/admin/comments/{id}` | ADMIN | 删除评论 |
| GET | `/api/admin/users` | ADMIN | 用户管理列表 |
| PUT | `/api/admin/users/{id}` | ADMIN | 更新用户（禁用/启用/修改角色） |

## 关键架构约定

### 前端 store 模式（模块级 ref，非 Pinia）
- `store/*.js` 导出 `ref()` 对象 + 操作函数
- 组件通过 `import { refName } from '@/store/xxx'` 直接访问
- `updateXxx()` 方法用于立即更新本地状态，`refreshXxx()` 重建缓存并从服务端拉取
- 同类模式：`user.js`（用户认证）、`site.js`（站点配置）、`theme.js`（主题）

### 后端响应格式
```json
{ "code": 200, "message": "success", "data": {} }
```
- 成功 `R.ok(data)`，失败 `R.fail(code, msg)`
- 前端 `request.js` 拦截器自动取 `res.data`（即 `response.data.data`）

### 权限控制
- JWT token 存 localStorage key `"token"`，请求头 `Authorization: Bearer <token>`
- `SecurityConfig`：`/api/about`、`/api/auth/**`、`/api/articles`（GET）、`/api/tags`、`/api/archive` 公开
- `/api/admin/**` 需 `hasRole('ADMIN')`
- 前端路由守卫 `router.beforeEach`：`requiresAuth` 检查 token，`requiresAdmin` 检查 `currentUser.value.role === 'admin'`

## 常用命令

```bash
# 前端开发（Vite dev server，热更新，proxy → :8080）
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build

# 后端启动（Maven）
cd backend && mvn spring-boot:run

# 后端打包
cd backend && mvn clean package -DskipTests
```

## AI 协作注意事项

- 本项目的 AI 模型偏好为 **DeepSeek v4 Pro**（通过 Claude Code CLI 调用）
- `.claude/` 目录存放 Claude Code 的项目级配置（settings.json、memory、plans 等）
- 修改 `store/site.js` 的默认值时，同步检查 `frontend/index.html` 和所有引用组件
- 前端模板支持 Vue 3 多根元素（fragments），ESLint 规则 `vue/no-multiple-template-root` 为误报
- 后台 "关于设置" 保存后，需同时调用 `updateSiteInfo()`（立即更新本地 store）+ `await refreshSiteInfo()`（拉取完整服务端数据），确保 NavBar、页脚、Home 打字机等所有组件即时响应
- 打字机效果使用 `setInterval`，重新启动前确保 `clearInterval` 旧定时器，避免双重循环导致文字乱码
- 数据库表 `blog_about` 为单行模式（始终 `id=1`），新增站点配置字段都加在这张表

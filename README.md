<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,14,20&height=200&section=header&text=个人博客&fontSize=60&fontAlignY=35&desc=记录技术思考与生活点滴&descAlignY=55&descSize=18" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.x-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/Vite-5.x-646CFF?style=flat-square&logo=vite&logoColor=white" />
  <img src="https://img.shields.io/badge/Element_Plus-2.x-409EFF?style=flat-square&logo=element&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MyBatis_Plus-3.x-FF6C0C?style=flat-square" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/OSS-阿里云-FF6A00?style=flat-square&logo=alibabacloud&logoColor=white" />
</p>

---

## 功能模块

### 前台页面

| 页面 | 功能 |
|---|---|
| 首页 | 打字机效果 Hero、标签云筛选、文章列表分页、搜索 |
| 文章详情 | Markdown 渲染、目录导航、代码高亮、评论（树形）、点赞 |
| 关于页 | 站长信息、技术栈展示、联系方式 |
| 归档页 | 年/月分组文章归档 |
| 登录/注册 | JWT 认证、邮箱验证码注册、自定义背景图 |

### 后台管理

| 页面 | 功能 |
|---|---|
| 文章管理 | 新建/编辑/删除文章、Markdown 编辑器 |
| 评论管理 | 审核、回复、删除评论 |
| 用户管理 | 用户列表、禁用/启用、修改角色 |
| 站点设置 | 头像、昵称、个人简介、技术栈、联系方式、网站图标、登录页背景 |

---

## 项目结构

```
├── frontend/         # Vue 3 + Vite 前端
│   └── src/
│       ├── views/    # 页面组件（首页/详情/关于/归档/后台）
│       ├── store/    # 模块级响应式状态（user/site/theme）
│       ├── utils/    # API 封装 + Axios 拦截器
│       └── components/  # NavBar / BackToTop / TOC
├── backend/          # Spring Boot 3 后端
│   └── src/main/java/com/blog/
│       ├── controller/  # 9 个 Controller（Auth/Article/Admin/Comment/Like/Tag/Upload）
│       ├── service/     # 业务逻辑层
│       ├── config/      # Security/JWT/Redis/CORS/MyBatis-Plus 配置
│       └── entity/      # 6 个实体（User/Article/Tag/Comment/Like/About）
```

---

## 快速开始

### 环境要求

- Java 17 + Maven 3.8+
- Node.js 18+、npm 9+
- MySQL 8.0、Redis 7

### 后端

```bash
cd backend
cp src/main/resources/application-local.yml.example application-local.yml
# 编辑 application-local.yml 填入你的数据库/邮箱/OSS/JWT 配置
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`

---

## 许可

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,14,20&height=100&section=footer" />
</p>

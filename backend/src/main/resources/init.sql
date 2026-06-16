-- ============================================
-- 个人博客 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS personal_blog
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE personal_blog;

-- ============================================
-- 1. 系统用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`       VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`       VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    `nickname`       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`         VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    `phone`          VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `email`          VARCHAR(100) NOT NULL DEFAULT '' COMMENT '邮箱',
    `email_verified` TINYINT      NOT NULL DEFAULT 0 COMMENT '邮箱验证: 0=未, 1=已',
    `role`           VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 初始管理员: admin / admin123
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `email`, `email_verified`, `role`) VALUES
('admin', '$2a$10$XFIfUcparRMvXETvp00ii.M.TLxH3QQKwDqU780.KM3dfHc5Fx6F6', '子墨', 'https://api.dicebear.com/7.x/notionists/svg?seed=blog&backgroundColor=ffdfbf', 'admin@blog.com', 1, 'admin');

-- 示例用户: reader / 123456
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `email`, `email_verified`, `role`) VALUES
('reader', '$2a$10$XFIfUcparRMvXETvp00ii.M.TLxH3QQKwDqU780.KM3dfHc5Fx6F6', '读者小明', 'https://api.dicebear.com/7.x/notionists/svg?seed=reader&backgroundColor=c0e0ff', 'reader@test.com', 0, 'user');

-- ============================================
-- 2. 文章表
-- ============================================
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `author_id`    BIGINT       NOT NULL DEFAULT 1 COMMENT '作者ID',
    `title`        VARCHAR(200) NOT NULL COMMENT '标题',
    `summary`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '摘要',
    `cover_image`  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '封面图URL',
    `content_md`   LONGTEXT     NOT NULL COMMENT 'Markdown原文',
    `content_html` LONGTEXT     NOT NULL COMMENT '转换后的HTML',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=草稿, 1=已发布',
    `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ============================================
-- 3. 标签表
-- ============================================
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
    `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ============================================
-- 4. 文章标签关联表
-- ============================================
DROP TABLE IF EXISTS `article_tag`;
CREATE TABLE `article_tag` (
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `tag_id`     BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`article_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`),
    CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章-标签关联表';

-- ============================================
-- 5. 评论表
-- ============================================
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id`      BIGINT   NOT NULL COMMENT '文章ID',
    `user_id`         BIGINT   NOT NULL COMMENT '评论用户ID',
    `parent_id`       BIGINT            DEFAULT NULL COMMENT '父评论ID，NULL表示顶层评论',
    `reply_to_user_id` BIGINT           DEFAULT NULL COMMENT '被回复用户ID',
    `content`         TEXT     NOT NULL COMMENT '评论内容',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================
-- 6. 点赞表
-- ============================================
DROP TABLE IF EXISTS `blog_like`;
CREATE TABLE `blog_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: article/comment',
    `target_id`   BIGINT   NOT NULL COMMENT '目标ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- ============================================
-- 7. 关于页面配置表
-- ============================================
DROP TABLE IF EXISTS `blog_about`;
CREATE TABLE `blog_about` (
    `id`         INT          NOT NULL DEFAULT 1 COMMENT '主键(仅1行)',
    `bio`        TEXT         COMMENT '关于我介绍',
    `tech_stack` VARCHAR(500) DEFAULT '[]' COMMENT '技术栈JSON数组',
    `github`     VARCHAR(200) DEFAULT '' COMMENT 'GitHub主页',
    `twitter`    VARCHAR(200) DEFAULT '' COMMENT 'Twitter主页',
    `email`      VARCHAR(200) DEFAULT '' COMMENT '公开邮箱',
    `favicon`    VARCHAR(500) DEFAULT '' COMMENT '网站图标URL',
    `login_bg`   VARCHAR(500) DEFAULT '' COMMENT '登录页背景图URL',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关于页面配置表';

INSERT INTO `blog_about` (`id`, `bio`, `tech_stack`, `github`, `twitter`, `email`, `favicon`, `login_bg`) VALUES
(1,
'你好，我是子墨，一名热爱技术的全栈开发者。\n这个博客记录了我在技术学习与工作中的思考、总结和实践。我喜欢探索新技术，追求简洁优雅的代码风格。工作之余，也会分享一些生活和阅读感悟。\n如果你对我的文章有任何想法，欢迎留言交流。',
'["Java","Spring Boot","MyBatis-Plus","MySQL","Redis","Vue 3","JavaScript","HTML/CSS","Element Plus","Docker","Linux","Git","Nginx","Alibaba Cloud"]',
'https://github.com',
'https://twitter.com',
'', '', '');

-- ============================================
-- 8. 示例数据
-- ============================================

-- 标签
INSERT INTO `blog_tag` (`name`) VALUES
('Java'),('Spring Boot'),('Vue.js'),('JavaScript'),
('数据库'),('Docker'),('后端开发'),('前端开发');

-- 文章（内容丰富的测试文章，含多级标题、代码块以便展示目录导航效果）
INSERT INTO `blog_article` (`author_id`, `title`, `summary`, `cover_image`, `content_md`, `content_html`, `status`, `view_count`, `create_time`) VALUES
(1, 'Spring Boot 3 新特性全面解析', '本文详细介绍了 Spring Boot 3 的重要新特性，包括 GraalVM 原生镜像支持、Java 17 基线、Jakarta EE 迁移、Observability 可观测性等。', 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&h=340&fit=crop', '# Spring Boot 3 新特性全面解析\n\nSpring Boot 3.0 于 2022 年 11 月正式发布，这是 Spring Boot 历史上最重要的版本之一。本文将全面解析其核心新特性。\n\n## 一、Java 17 基线\n\nSpring Boot 3 将最低 Java 版本要求提升至 Java 17，这意味着我们可以充分利用 Java 17 带来的诸多新特性。\n\n### 1.1 Records 记录类\n\nRecords 是 Java 14 引入的预览特性，在 Java 16 中正式发布。它极大简化了数据载体类的编写：\n\n```java\npublic record UserDto(Long id, String name, String email) {}\n```\n\n相比传统的 POJO，Records 自动生成构造器、getter、equals、hashCode 和 toString 方法。\n\n### 1.2 文本块 Text Blocks\n\n文本块让多行字符串的编写更加自然：\n\n```java\nString json = \"\"\"\n    {\n        \"name\": \"Spring Boot 3\",\n        \"version\": \"3.0.0\"\n    }\n    \"\"\";\n```\n\n### 1.3 密封类 Sealed Classes\n\n密封类允许你控制哪些类可以继承或实现某个类或接口：\n\n```java\npublic sealed interface Shape\n    permits Circle, Rectangle, Triangle {\n    double area();\n}\n```\n\n## 二、GraalVM 原生镜像支持\n\n这是 Spring Boot 3 最受瞩目的特性。原生镜像将应用提前编译为独立的可执行文件。\n\n### 2.1 性能对比\n\n| 指标 | JVM 模式 | 原生镜像 |\n|------|----------|----------|\n| 启动时间 | 2-3 秒 | 0.05 秒 |\n| 内存占用 | ~300MB | ~50MB |\n| 打包体积 | ~20MB | ~80MB |\n\n### 2.2 构建配置\n\n在 `pom.xml` 中添加 GraalVM 原生插件：\n\n```xml\n<plugin>\n    <groupId>org.graalvm.buildtools</groupId>\n    <artifactId>native-maven-plugin</artifactId>\n    <version>0.9.20</version>\n</plugin>\n```\n\n### 2.3 AOT 编译的局限性\n\n虽然原生镜像性能出色，但也有一些限制需要注意：\n\n- 不支持动态代理的运行时生成\n- 反射需要在编译期配置\n- 某些第三方库可能不兼容\n\n## 三、Jakarta EE 9+ 迁移\n\n从 Spring Boot 2.x 的 `javax.*` 命名空间迁移到 `jakarta.*` 是最重要的 breaking change。\n\n### 3.1 影响范围\n\n几乎所有 Java EE 相关的注解和接口都受到影响：\n\n```java\n// 旧版本 (Spring Boot 2.x)\nimport javax.servlet.http.HttpServletRequest;\nimport javax.persistence.Entity;\n\n// 新版本 (Spring Boot 3.x)\nimport jakarta.servlet.http.HttpServletRequest;\nimport jakarta.persistence.Entity;\n```\n\n### 3.2 迁移工具\n\nSpring 官方提供了 OpenRewrite 迁移工具，可以自动完成大部分重命名工作。\n\n## 四、可观测性增强\n\n### 4.1 Micrometer 集成\n\nSpring Boot 3 深度集成了 Micrometer，开箱即用地提供指标、链路追踪和健康检查。\n\n### 4.2 应用追踪\n\n通过 Micrometer Tracing，可以轻松实现分布式追踪：\n\n```yaml\nmanagement:\n  tracing:\n    sampling:\n      probability: 1.0\n```\n\n## 五、总结与展望\n\nSpring Boot 3 是一个面向未来的重要版本。Native Image 支持让 Java 应用在 Serverless 和容器化场景中更具竞争力；Java 17 基线为未来十年的发展奠定了基础。\n\n### 迁移建议\n\n1. 优先升级到 Spring Boot 2.7.x 最新版\n2. 全面评估 Jakarta EE 迁移的影响\n3. 在新项目中优先选择 Spring Boot 3\n4. 关注 GraalVM 生态的成熟度\n\n> **提示**：迁移前建议先在测试环境充分验证，特别是涉及 JPA、Servlet Filter 等底层组件时。', '<h1>Spring Boot 3 新特性全面解析</h1><p>Spring Boot 3.0 于 2022 年 11 月正式发布。</p>', 1, 256, '2026-05-15 09:30:00'),
(1, 'Vue 3 Composition API 实战技巧', '分享在实际项目中使用 Vue 3 Composition API 的经验和技巧，涵盖 ref/reactive 响应式系统、自定义 Hook 封装、watch 侦听器、组件通信等核心主题。', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&h=340&fit=crop', '# Vue 3 Composition API 实战技巧\n\nComposition API 是 Vue 3 最重要的特性，它彻底改变了我们组织组件逻辑的方式。\n\n## 一、响应式基础：ref vs reactive\n\n理解 ref 和 reactive 的区别是掌握 Composition API 的第一步。\n\n### 1.1 ref 的使用场景\n\nref 适用于基本类型和需要替换整个值的场景：\n\n```javascript\nimport { ref } from "vue"\n\nconst count = ref(0)\nconst message = ref("Hello World")\n\n// 在 setup 中访问需要 .value\nconsole.log(count.value) // 0\n```\n\n### 1.2 reactive 的使用场景\n\nreactive 适用于对象和数组：\n\n```javascript\nimport { reactive } from "vue"\n\nconst state = reactive({\n  user: { name: "子墨", role: "admin" },\n  list: []\n})\n```\n\n### 1.3 选择建议\n\n- 基本类型用 `ref`\n- 表单对象用 `reactive`\n- `.value` 在模板中自动解包是 ref 的优势\n\n## 二、自定义 Hook 封装\n\n这是 Composition API 最强大的能力——将逻辑抽象为可复用的函数。\n\n### 2.1 封装一个 useRequest Hook\n\n```javascript\nimport { ref } from "vue"\n\nexport function useRequest(apiFn) {\n  const loading = ref(false)\n  const data = ref(null)\n  const error = ref(null)\n\n  async function execute(...args) {\n    loading.value = true\n    error.value = null\n    try {\n      const res = await apiFn(...args)\n      data.value = res.data\n      return res\n    } catch (e) {\n      error.value = e\n      throw e\n    } finally {\n      loading.value = false\n    }\n  }\n\n  return { loading, data, error, execute }\n}\n```\n\n### 2.2 封装一个 useTheme Hook\n\n```javascript\nexport function useTheme() {\n  const isDark = ref(localStorage.getItem("theme") === "dark")\n\n  watch(isDark, (val) => {\n    document.documentElement.classList.toggle("dark", val)\n    localStorage.setItem("theme", val ? "dark" : "light")\n  }, { immediate: true })\n\n  return { isDark, toggle: () => isDark.value = !isDark.value }\n}\n```\n\n## 三、watch 与 watchEffect\n\n### 3.1 watch 精确侦听\n\n```javascript\nwatch(() => state.count, (newVal, oldVal) => {\n  console.log(`count 从 ${oldVal} 变为 ${newVal}`)\n})\n\n// 侦听多个源\nwatch([count, message], ([newCount, newMsg], [oldCount, oldMsg]) => {\n  // ...\n})\n```\n\n### 3.2 watchEffect 自动追踪\n\n```javascript\nwatchEffect(() => {\n  console.log(`当前计数: ${count.value}`)\n})\n```\n\n## 四、组件通信模式\n\n### 4.1 Props 与 Emits\n\n```javascript\nconst props = defineProps({\n  title: String,\n  visible: Boolean\n})\n\nconst emit = defineEmits(["update:visible", "submit"])\n```\n\n### 4.2 依赖注入模式\n\n跨层级组件通信使用 provide/inject：\n\n```javascript\n// 祖先组件\nprovide("theme", theme)\n\n// 后代组件\nconst theme = inject("theme")\n```\n\n## 五、实战总结\n\nComposition API 的核心优势在于逻辑复用和更好的 TypeScript 支持。当组件逻辑变得复杂时，Composition API 可以保持代码的清晰和可维护性。', '<h1>Vue 3 Composition API 实战技巧</h1><p>Composition API 是 Vue 3 最重要的特性。</p>', 1, 389, '2026-05-12 14:20:00'),
(1, 'MySQL 8.0 性能优化完全指南', '从索引优化、SQL 查询优化、配置调优、架构设计四个方面，系统性地介绍 MySQL 8.0 的性能优化策略。', 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=600&h=340&fit=crop', '# MySQL 8.0 性能优化完全指南\n\n数据库性能优化是一个系统工程，需要从多个维度系统推进。\n\n## 一、索引优化\n\n索引是数据库优化的第一道防线。正确的索引设计能将查询性能提升数百倍。\n\n### 1.1 B+Tree 索引原理\n\nMySQL InnoDB 使用 B+Tree 作为索引结构。理解其原理有助于我们设计更高效的索引。\n\n### 1.2 覆盖索引\n\n覆盖索引可以避免回表查询，显著提升性能：\n\n```sql\n-- 建立覆盖索引\nCREATE INDEX idx_user_name_email ON users(name, email);\n\n-- 这个查询可以完全使用索引\nSELECT name, email FROM users WHERE name = "子墨";\n```\n\n### 1.3 复合索引与最左前缀\n\n复合索引遵循最左前缀原则：\n\n```sql\n-- 创建复合索引\nCREATE INDEX idx_a_b_c ON orders(user_id, status, create_time);\n\n-- 以下查询可以使用索引\nSELECT * FROM orders WHERE user_id = 1;\nSELECT * FROM orders WHERE user_id = 1 AND status = "paid";\nSELECT * FROM orders WHERE user_id = 1 AND create_time > "2026-01-01";\n\n-- 以下查询不能使用索引\nSELECT * FROM orders WHERE status = "paid";\nSELECT * FROM orders WHERE create_time > "2026-01-01";\n```\n\n### 1.4 索引优化的注意事项\n\n- 避免在索引列上使用函数或运算\n- 注意区分度，低区分度列不适合单独建索引\n- 定期分析慢查询日志\n\n## 二、SQL 查询优化\n\n### 2.1 使用 EXPLAIN 分析执行计划\n\n```sql\nEXPLAIN SELECT * FROM orders WHERE user_id = 1;\n```\n\n重点关注 type、key、rows、Extra 字段。\n\n### 2.2 大表分页优化\n\n传统的 LIMIT offset, size 在大偏移量时性能很差：\n\n```sql\n-- 不推荐：OFFSET 过大会导致扫描大量无用行\nSELECT * FROM articles ORDER BY id LIMIT 100000, 20;\n\n-- 推荐：使用游标分页\nSELECT * FROM articles WHERE id > 100000 ORDER BY id LIMIT 20;\n```\n\n### 2.3 避免 SELECT *\n\n只查询需要的字段，减少数据传输和内存占用。\n\n## 三、MySQL 8.0 配置调优\n\n### 3.1 InnoDB Buffer Pool\n\nBuffer Pool 是 InnoDB 最重要的内存结构：\n\n```ini\n[mysqld]\ninnodb_buffer_pool_size = 8G\ninnodb_buffer_pool_instances = 8\ninnodb_log_file_size = 2G\n```\n\n### 3.2 连接池配置\n\n```ini\nmax_connections = 1000\nthread_cache_size = 100\n```\n\n## 四、架构层面的优化\n\n### 4.1 读写分离\n\n使用 MySQL 主从复制，将读请求分流到从库。\n\n### 4.2 缓存策略\n\n合理使用 Redis 作为缓存层，减轻数据库压力：\n\n- 热点数据缓存\n- 查询结果缓存\n- 计数器缓存\n\n### 4.3 分区表\n\n对于大表，可以考虑使用分区表按时间或 ID 范围分区。\n\n## 五、监控与诊断\n\n使用 Performance Schema 和 sys schema 监控数据库运行状态，及时发现性能瓶颈。\n\n```sql\n-- 查看当前运行的查询\nSELECT * FROM sys.processlist;\n\n-- 查看最耗资源的语句\nSELECT * FROM sys.statements_with_full_table_scans;\n```\n\n> **最佳实践**：优化是一个持续的过程，建议建立性能基线，定期对比分析。', '<h1>MySQL 8.0 性能优化完全指南</h1><p>数据库性能优化是一个系统工程。</p>', 1, 178, '2026-05-08 10:00:00'),
(1, 'Docker 容器化部署最佳实践', '总结使用 Docker 进行应用容器化部署的最佳实践，包括 Dockerfile 优化、多阶段构建、Docker Compose 编排、健康检查、日志管理和安全加固等。', 'https://images.unsplash.com/photo-1605745341112-85968b19335b?w=600&h=340&fit=crop', '# Docker 容器化部署最佳实践\n\nDocker 已成为现代应用部署的标准方式。掌握最佳实践能让你的容器更加高效、安全和易于维护。\n\n## 一、Dockerfile 编写规范\n\n### 1.1 选择合适的基础镜像\n\n优先使用官方镜像的 Alpine 版本以减小体积：\n\n```dockerfile\n# 推荐：Alpine 版本体积更小\nFROM node:18-alpine\n\n# 避免使用 latest 标签\nFROM node:18.15.0-alpine3.17\n```\n\n### 1.2 多阶段构建\n\n多阶段构建可以显著减小最终镜像大小：\n\n```dockerfile\n# 第一阶段：构建\nFROM maven:3.8-openjdk-17 AS builder\nWORKDIR /app\nCOPY pom.xml .\nRUN mvn dependency:go-offline\nCOPY src ./src\nRUN mvn package -DskipTests\n\n# 第二阶段：运行\nFROM openjdk:17-alpine\nCOPY --from=builder /app/target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT ["java", "-jar", "/app.jar"]\n```\n\n### 1.3 层缓存优化\n\n利用 Docker 的层缓存机制，将变化频率低的文件先复制：\n\n```dockerfile\n# 先复制依赖描述文件（变化少）\nCOPY package.json package-lock.json ./\nRUN npm ci\n\n# 再复制源码（变化频繁）\nCOPY . .\n```\n\n## 二、Docker Compose 服务编排\n\n### 2.1 完整的开发环境编排\n\n```yaml\nversion: "3.8"\nservices:\n  app:\n    build: .\n    ports:\n      - "8080:8080"\n    depends_on:\n      mysql:\n        condition: service_healthy\n    environment:\n      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/blog\n\n  mysql:\n    image: mysql:8.0\n    volumes:\n      - mysql_data:/var/lib/mysql\n    healthcheck:\n      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]\n      interval: 10s\n      retries: 5\n\nvolumes:\n  mysql_data:\n```\n\n### 2.2 环境变量管理\n\n使用 env_file 管理不同环境的配置，避免在 compose 文件中硬编码敏感信息。\n\n## 三、容器安全加固\n\n### 3.1 非 root 用户运行\n\n```dockerfile\nRUN addgroup -S appgroup && adduser -S appuser -G appgroup\nUSER appuser\n```\n\n### 3.2 镜像安全扫描\n\n定期使用工具扫描镜像漏洞：\n\n```bash\ndocker scan myapp:latest\ntrivy image myapp:latest\n```\n\n## 四、健康检查与日志管理\n\n### 4.1 健康检查\n\n为容器配置健康检查确保服务可用：\n\n```dockerfile\nHEALTHCHECK --interval=30s --timeout=3s --retries=3 \\\n  CMD curl -f http://localhost:8080/actuator/health || exit 1\n```\n\n### 4.2 日志管理\n\n容器化应用应该将日志输出到 stdout/stderr，而非写入文件。使用日志驱动配置限制日志大小：\n\n```yaml\nservices:\n  app:\n    logging:\n      driver: "json-file"\n      options:\n        max-size: "10m"\n        max-file: "3"\n```\n\n## 五、总结\n\nDocker 最佳实践的核心是：镜像小、启动快、安全、可观测。在生产环境中部署前，务必将这些实践融入 CI/CD 流程中。', '<h1>Docker 容器化部署最佳实践</h1><p>Docker 已成为现代应用部署的标准方式。</p>', 1, 312, '2026-05-05 08:15:00'),
(1, 'JavaScript 异步编程深度解析', '从回调函数到 Promise 再到 async/await，从事件循环机制到微任务与宏任务，全面深度解析 JavaScript 异步编程的核心原理与实战技巧。', 'https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=600&h=340&fit=crop', '# JavaScript 异步编程深度解析\n\nJavaScript 的异步编程经历了从回调地狱到优雅的 async/await 的演进。\n\n## 一、单线程与事件循环\n\nJavaScript 是单线程语言，但浏览器和 Node.js 提供了异步能力。理解事件循环是掌握异步编程的关键。\n\n### 1.1 事件循环机制\n\n事件循环负责协调代码执行、事件处理和异步任务。它包含调用栈、任务队列和微任务队列。\n\n### 1.2 宏任务与微任务\n\n```javascript\nconsole.log("1");  // 同步\n\nsetTimeout(() => {\n  console.log("2");  // 宏任务\n}, 0);\n\nPromise.resolve().then(() => {\n  console.log("3");  // 微任务\n});\n\nconsole.log("4");  // 同步\n\n// 输出顺序: 1 → 4 → 3 → 2\n```\n\n**执行顺序**：同步代码 → 微任务队列（Promise.then, MutationObserver）→ 宏任务队列（setTimeout, setInterval, I/O）。\n\n## 二、回调函数时代\n\n### 2.1 回调地狱\n\n早期的异步处理依赖于回调函数，但嵌套过深会导致代码难以维护：\n\n```javascript\n// 回调地狱示例\nfs.readFile("a.txt", (err, dataA) => {\n  if (err) throw err;\n  fs.readFile("b.txt", (err, dataB) => {\n    if (err) throw err;\n    fs.readFile("c.txt", (err, dataC) => {\n      if (err) throw err;\n      console.log(dataA + dataB + dataC);\n    });\n  });\n});\n```\n\n## 三、Promise 时代\n\n### 3.1 Promise 基础\n\nPromise 有三种状态：pending、fulfilled、rejected。状态一旦改变就不可逆。\n\n```javascript\nfunction fetchUser(id) {\n  return new Promise((resolve, reject) => {\n    fetch(`/api/users/${id}`)\n      .then(res => res.json())\n      .then(resolve)\n      .catch(reject);\n  });\n}\n```\n\n### 3.2 Promise 组合模式\n\n```javascript\n// 并行请求\nconst [user, articles, tags] = await Promise.all([\n  fetchUser(1),\n  fetchArticles(),\n  fetchTags()\n]);\n\n// 竞速\nconst result = await Promise.race([\n  fetch("/api/fast"),\n  new Promise((_, reject) =>\n    setTimeout(() => reject(new Error("Timeout")), 5000)\n  )\n]);\n```\n\n### 3.3 错误处理\n\nPromise 的错误会沿着链传递，直到被 catch 捕获。\n\n## 四、async/await 时代\n\n### 4.1 基本用法\n\nasync/await 是 Promise 的语法糖，让异步代码看起来像同步代码：\n\n```javascript\nasync function loadUserProfile(userId) {\n  try {\n    const user = await fetchUser(userId);\n    const articles = await fetchUserArticles(userId);\n    return { user, articles };\n  } catch (error) {\n    console.error("加载用户信息失败:", error);\n    throw error;\n  }\n}\n```\n\n### 4.2 循环中的异步\n\n```javascript\n// 串行处理\nfor (const item of items) {\n  await processItem(item);\n}\n\n// 并行处理\nawait Promise.all(items.map(item => processItem(item)));\n```\n\n### 4.3 async/await 注意事项\n\n- async 函数始终返回 Promise\n- 不要在 forEach 中使用 await（forEach 不支持异步）\n- 合理使用 try/catch 处理错误\n\n## 五、高级异步模式\n\n### 5.1 并发控制\n\n当需要限制并发数时，可以使用信号量模式：\n\n```javascript\nasync function asyncPool(limit, items, fn) {\n  const ret = [];\n  const executing = new Set();\n  for (const item of items) {\n    const p = Promise.resolve().then(() => fn(item));\n    ret.push(p);\n    executing.add(p);\n    const clean = () => executing.delete(p);\n    p.then(clean, clean);\n    if (executing.size >= limit) {\n      await Promise.race(executing);\n    }\n  }\n  return Promise.all(ret);\n}\n```\n\n### 5.2 取消异步操作\n\n使用 AbortController 可以取消 fetch 请求：\n\n```javascript\nconst controller = new AbortController();\nconst signal = controller.signal;\n\nfetch("/api/data", { signal })\n  .catch(err => {\n    if (err.name === "AbortError") {\n      console.log("请求已取消");\n    }\n  });\n\n// 5 秒后取消\nsetTimeout(() => controller.abort(), 5000);\n```\n\n## 六、总结\n\n从回调到 async/await，JavaScript 异步编程的演进让代码越来越直观。理解底层的事件循环机制是写出高质量异步代码的基础。', '<h1>JavaScript 异步编程深度解析</h1><p>JavaScript 的异步编程经历了从回调地狱到优雅的 async/await 的演进。</p>', 1, 245, '2026-04-28 16:45:00'),
(1, '前后端分离架构设计与实践', '分享在个人博客项目和企业项目中实践前后端分离架构的经验，涵盖 RESTful API 设计、JWT 认证方案、跨域处理、权限控制、部署策略等核心主题。', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&h=340&fit=crop', '# 前后端分离架构设计与实践\n\n前后端分离是现代 Web 开发的主流架构。本文分享我在个人项目和企业项目中实践的经验。\n\n## 一、架构概览\n\n### 1.1 前后端分离的优势\n\n- **开发解耦**：前后端团队可以并行开发\n- **技术独立**：前端可以使用 Vue/React，后端可以使用 Java/Go/Node.js\n- **部署独立**：前后端可以独立部署和扩展\n- **复用性强**：同一套 API 可以服务 Web、Mobile、小程序\n\n### 1.2 整体架构图\n\n```\n┌─────────┐    HTTP/REST    ┌──────────┐    JDBC    ┌────────┐\n│  Vue 3  │ <─────────────> │ Spring   │ <────────> │ MySQL  │\n│  前端   │    JSON 格式     │  Boot    │            │  数据库 │\n└─────────┘                 │  后端    │            └────────┘\n                            │          │\n                            │ ┌──────┐ │   ┌────────┐\n                            │ │ Redis │ │──>│ 阿里云  │\n                            │ └──────┘ │   │  OSS   │\n                            └──────────┘   └────────┘\n```\n\n## 二、RESTful API 设计\n\n### 2.1 URL 设计规范\n\n遵循资源导向的 URL 设计：\n\n```\nGET    /api/articles          # 获取文章列表\nGET    /api/articles/:id      # 获取文章详情\nPOST   /api/articles          # 创建文章\nPUT    /api/articles/:id      # 更新文章\nDELETE /api/articles/:id      # 删除文章\n```\n\n### 2.2 统一响应格式\n\n设计统一的 API 响应结构是良好实践：\n\n```json\n{\n  "code": 200,\n  "message": "success",\n  "data": {\n    "records": [...],\n    "total": 100,\n    "current": 1,\n    "size": 10\n  }\n}\n```\n\n### 2.3 版本管理\n\n在 URL 中加入版本号：/api/v1/articles、/api/v2/articles。\n\n## 三、JWT 认证方案\n\n### 3.1 JWT 工作流程\n\n1. 用户登录，服务器验证凭据\n2. 生成 JWT Token，返回给客户端\n3. 客户端将 Token 存储在 localStorage\n4. 后续请求在 Authorization 头中携带 Token\n5. 服务器解析 Token 获取用户身份\n\n### 3.2 Token 刷新策略\n\n```javascript\n// Axios 拦截器：检测 401 并刷新 Token\naxios.interceptors.response.use(\n  response => response,\n  async error => {\n    if (error.response?.status === 401) {\n      const newToken = await refreshToken();\n      error.config.headers.Authorization = `Bearer ${newToken}`;\n      return axios(error.config);\n    }\n    return Promise.reject(error);\n  }\n);\n```\n\n### 3.3 安全性考量\n\n- Token 设置合理的过期时间（通常 24 小时）\n- 敏感操作需要二次验证\n- HTTPS 保证传输安全\n- 不要在 Token 中存储敏感信息\n\n## 四、跨域处理与权限控制\n\n### 4.1 CORS 配置\n\n后端需要配置 CORS 允许前端跨域访问。Spring Boot 中通过 CorsConfiguration 配置允许的来源、方法和头信息。\n\n### 4.2 RBAC 权限模型\n\n基于角色的访问控制：用户 → 角色 → 权限。使用 @PreAuthorize 注解控制接口访问权限。\n\n### 4.3 前端路由守卫\n\n```javascript\nrouter.beforeEach((to, from, next) => {\n  if (to.meta.requiresAuth && !isLoggedIn.value) {\n    next({ name: "Login", query: { redirect: to.fullPath } });\n    return;\n  }\n  next();\n});\n```\n\n## 五、部署策略\n\n### 5.1 开发环境\n\n- 前端：Vite Dev Server (port 5173)，配置代理到后端\n- 后端：Spring Boot DevTools 热重载 (port 8080)\n\n### 5.2 生产环境\n\n使用 Nginx 作为反向代理，统一入口：\n\n```nginx\nlocation / {\n    root /app/dist;\n    try_files $uri $uri/ /index.html;\n}\n\nlocation /api/ {\n    proxy_pass http://localhost:8080;\n}\n```\n\n## 六、总结\n\n前后端分离架构是当前 Web 开发的主流选择。核心在于清晰的 API 契约、可靠的安全机制和高效的协作流程。', '<h1>前后端分离架构设计与实践</h1><p>前后端分离是现代 Web 开发的主流架构。</p>', 1, 156, '2026-04-20 11:30:00'),
(1, 'Redis 缓存实战：从入门到精通', '全面讲解 Redis 核心数据结构、缓存策略、分布式锁、消息队列、持久化方案，以及在 Spring Boot 中的最佳实践。', 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=340&fit=crop', '# Redis 缓存实战：从入门到精通\n\nRedis 是最流行的内存数据库，广泛应用于缓存、分布式锁、消息队列等场景。\n\n## 一、Redis 核心数据结构\n\n### 1.1 String 字符串\n\n最基础的数据类型，常用于缓存简单值和计数器：\n\n```bash\nSET article:1:views 1000\nINCR article:1:views\nGET article:1:views\n```\n\n### 1.2 Hash 哈希\n\n适合存储对象：\n\n```bash\nHSET user:1 name "子墨" role "admin" email "admin@blog.com"\nHGET user:1 name\nHGETALL user:1\n```\n\n### 1.3 List 列表\n\n可用于消息队列、最新动态等：\n\n```bash\nLPUSH timeline:latest "文章A" "文章B" "文章C"\nLRANGE timeline:latest 0 9\n```\n\n### 1.4 Set 集合\n\n存储不重复元素，支持交并差运算：\n\n```bash\nSADD article:1:tags "Java" "Spring" "Redis"\nSINTER article:1:tags article:2:tags\n```\n\n### 1.5 Sorted Set 有序集合\n\n排行榜的理想选择：\n\n```bash\nZADD article:rank 256 "文章A" 189 "文章B" 312 "文章C"\nZREVRANGE article:rank 0 9 WITHSCORES\n```\n\n## 二、缓存策略\n\n### 2.1 Cache-Aside 模式\n\n最常用的缓存策略：读取时先查缓存，未命中则查数据库并回写缓存。\n\n```java\npublic Article getArticle(Long id) {\n    String key = "article:" + id;\n    Article article = redisTemplate.opsForValue().get(key);\n    if (article != null) return article;\n\n    article = articleMapper.selectById(id);\n    if (article != null) {\n        redisTemplate.opsForValue().set(key, article, 30, TimeUnit.MINUTES);\n    }\n    return article;\n}\n```\n\n### 2.2 缓存更新策略\n\n- **先删缓存后更新数据库**（推荐）\n- **延迟双删**：删缓存 → 更新数据库 → 延时后再删缓存\n\n### 2.3 缓存穿透/击穿/雪崩\n\n| 问题 | 原因 | 解决方案 |\n|------|------|----------|\n| 缓存穿透 | 查询不存在的数据 | 布隆过滤器、空值缓存 |\n| 缓存击穿 | 热点 key 过期 | 互斥锁、永不过期 |\n| 缓存雪崩 | 大量 key 同时过期 | 过期时间加随机值、多级缓存 |\n\n## 三、分布式锁\n\n### 3.1 Redisson 实现\n\n```java\nRLock lock = redissonClient.getLock("lock:order:" + orderId);\ntry {\n    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {\n        // 执行业务逻辑\n    }\n} finally {\n    if (lock.isHeldByCurrentThread()) {\n        lock.unlock();\n    }\n}\n```\n\n### 3.2 注意事项\n\n- 锁必须设置过期时间\n- 释放锁前检查是否自己持有\n- 考虑使用 RedLock 算法提高可靠性\n\n## 四、持久化方案\n\n### 4.1 RDB 快照\n\n适合备份和灾难恢复，但可能在故障时丢失最近的数据。\n\n### 4.2 AOF 日志\n\n更持久化，但文件体积更大，恢复速度较慢。\n\n### 4.3 混合持久化（Redis 4.0+）\n\n结合 RDB 和 AOF 的优点，推荐使用。\n\n## 五、Spring Boot 集成实战\n\n```java\n@Bean\npublic CacheManager cacheManager(RedisConnectionFactory factory) {\n    RedisCacheConfiguration config = RedisCacheConfiguration\n        .defaultCacheConfig()\n        .entryTtl(Duration.ofMinutes(5))\n        .serializeValuesWith(\n            RedisSerializationContext.SerializationPair\n                .fromSerializer(new GenericJackson2JsonRedisSerializer())\n        );\n    return RedisCacheManager.builder(factory)\n        .cacheDefaults(config)\n        .build();\n}\n```\n\n## 六、总结\n\nRedis 是一个功能丰富的瑞士军刀。合理使用 Redis 可以大幅提升系统性能和可扩展性。', '<h1>Redis 缓存实战：从入门到精通</h1><p>Redis 是最流行的内存数据库。</p>', 1, 198, '2026-04-15 09:00:00'),
(1, 'Git 工作流与团队协作规范', '介绍 Git Flow、GitHub Flow、Trunk-Based 三种主流工作流，以及提交规范、代码审查、分支管理等团队协作最佳实践。', 'https://images.unsplash.com/photo-1556075798-4825dfaaf498?w=600&h=340&fit=crop', '# Git 工作流与团队协作规范\n\n规范的 Git 工作流是高效团队协作的基础。\n\n## 一、主流工作流对比\n\n### 1.1 Git Flow\n\n适合有固定发布周期的项目。分支结构复杂但职责清晰：\n\n- **master**：生产环境代码\n- **develop**：开发主线\n- **feature/***：功能分支\n- **release/***：发布分支\n- **hotfix/***：紧急修复分支\n\n### 1.2 GitHub Flow\n\n适合持续部署的项目，分支模型简单：\n\n1. 从 main 创建 feature 分支\n2. 提交代码并创建 PR\n3. 代码审查通过后合并\n4. 合并后立即部署\n\n### 1.3 Trunk-Based Development\n\n主干开发模式，所有开发者直接在主干（或短生命周期分支）上工作，适合 DevOps 成熟度高的团队。\n\n## 二、Commit Message 规范\n\n### 2.1 Conventional Commits\n\n```\n<type>(<scope>): <subject>\n\n<body>\n\n<footer>\n```\n\n常用类型：\n\n| Type | 说明 |\n|------|------|\n| feat | 新功能 |\n| fix | Bug 修复 |\n| docs | 文档更新 |\n| style | 代码格式（不影响逻辑）|\n| refactor | 重构 |\n| perf | 性能优化 |\n| test | 测试相关 |\n| chore | 构建/工具变更 |\n\n### 2.2 示例\n\n```bash\ngit commit -m "feat(article): add full-text search support"\ngit commit -m "fix(auth): resolve JWT token refresh issue"\ngit commit -m "docs(readme): update deployment guide"\n```\n\n## 三、分支管理最佳实践\n\n### 3.1 分支命名规范\n\n```bash\nfeature/article-search     # 功能分支\nfix/login-redirect-loop    # 修复分支\nrelease/v2.1.0             # 发布分支\n```\n\n### 3.2 合并策略\n\n- **Squash Merge**：将功能分支的所有提交压缩为一个\n- **Merge Commit**：保留完整的分支历史\n- **Rebase Merge**：线性历史，无合并提交\n\n### 3.3 保持分支同步\n\n```bash\ngit checkout feature/my-feature\ngit rebase main\n```\n\n## 四、代码审查\n\n### 4.1 PR 描述模板\n\n```markdown\n## 变更说明\n简要描述本次变更的内容和原因。\n\n## 测试计划\n- [ ] 单元测试通过\n- [ ] 集成测试通过\n- [ ] 手动测试验证\n\n## 关联 Issue\nCloses #123\n```\n\n### 4.2 审查要点\n\n- 逻辑正确性\n- 代码风格和命名\n- 安全漏洞（XSS、SQL 注入等）\n- 性能影响\n- 测试覆盖\n\n## 五、总结\n\n选择合适的 Git 工作流并坚持执行是团队效率的保障。规范不是束缚，而是让协作更顺畅的润滑剂。', '<h1>Git 工作流与团队协作规范</h1><p>规范的 Git 工作流是高效团队协作的基础。</p>', 1, 134, '2026-04-10 14:00:00');

-- 文章-标签关联
INSERT INTO `article_tag` (`article_id`, `tag_id`) VALUES
(1,1),(1,2),(1,7),(2,3),(2,4),(2,8),(3,1),(3,5),(3,7),(4,6),(4,7),(5,4),(5,8),(6,3),(6,7),(6,8),
(7,1),(7,2),(7,5),(7,7),(8,3),(8,4),(8,6),(8,7);

-- 示例评论
INSERT INTO `blog_comment` (`article_id`, `user_id`, `content`, `create_time`) VALUES
(1, 2, '写得非常好！Spring Boot 3 的新特性总结得很全面。', '2026-05-12 10:30:00'),
(1, 1, '谢谢支持，后续会继续更新更多 Spring Boot 实践文章。', '2026-05-12 11:00:00'),
(2, 2, 'Composition API 确实比 Options API 好用很多。', '2026-05-10 09:15:00'),
(3, 2, 'MySQL 索引优化的部分讲得很透彻，期待更多文章。', '2026-05-07 14:20:00');

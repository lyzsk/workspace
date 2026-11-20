# Workspace

面向内部业务的统一工作台：RuoYi 风格的后端权限与系统管理能力、Art Design Pro 风格的前端，以及可独立接入的微前端子应用。

## 技术栈

- Java 17, Spring Boot 3.5.16, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0, Spring Security 6, MyBatis-Plus 3.5.18.1, PostgreSQL 17, Redis, Maven 3.9+
- Vue 3, Vite 8, TypeScript, Pinia, Vue Router, Element Plus, SCSS
- qiankun 3.0.0-rc.22, LogicFlow, Excalidraw, Spring WebSocket, Quartz, Velocity
- OpenFeign、Nacos、Spring Cloud Gateway
- Knife4j 4.5.0, springdoc 2.8.9

## 模块

```text
workspace/
├─ workspace-common     通用能力，按包分层：core / security / redis
├─ workspace-auth       认证服务：登录、JWT 签发、会话、刷新、退出
├─ workspace-system     平台业务服务：用户、角色、菜单、应用授权、通知、文件、工具、Quartz、Generator
├─ workspace-tool       流程图、脑图、UML、白板及协作 WebSocket（库，随 system 启动）
├─ workspace-quartz     任务调度与 cron 校验（库，随 system 启动）
├─ workspace-generator  PostgreSQL 元数据、Java/TypeScript/Vue 模板（库，随 system 启动）
├─ workspace-gateway    统一入口：JWT 验签、Redis 会话校验、路由、限流，端口 61115
├─ workspace-ui         Vue 单页基座，开发端口 5173
└─ sql                  初始化说明与独立本地账号 seed
```

依赖方向：`auth → common`；`system → {common, tool, quartz, generator}`；`tool → system`；`gateway` 独立启动，不依赖业务模块。

## 认证与会话

- access token 为 HS256 JWT，只包含 `userId / sessionId / tokenVersion / exp`；
- Redis 保存完整 `LoginUser` 会话，键为 `login:user:<sessionId>`；
- 空闲超时由 Redis TTL 控制（30 分钟），绝对上限 12 小时；
- 同一账号默认只保留一个有效会话，新登录会替换旧会话，适配当前 PC 单端场景；
- 应用授权缓存在 `authz:<userId>:<appCode>`，子应用只读；
- 内部服务通过服务凭证访问用户同步接口，凭证不进入前端。

## 快速启动

### 1. 环境

- JDK 17
- Maven 3.9+
- Node.js 22+
- PostgreSQL 17
- Nacos Server 3.0.3
- Redis

### 2. 初始化数据库

完整结构维护在 `workspace-system/src/main/resources/db/migration/`。已有环境直接启动 `workspace-system`，Flyway 会补齐迁移：

```bash
cd workspace
mvn -DskipTests package
```

需要手工重建时，先创建数据库与 `system` schema，再按版本顺序执行 V1 起的迁移和 `sql/seed_user.sql`。

### 3. 后端（三个进程）

```bash
cd workspace
mvn -DskipTests package

java -jar workspace-system/target/workspace-system-1.0-exec.jar --spring.profiles.active=dev
java -jar workspace-auth/target/workspace-auth-1.0.jar --spring.profiles.active=dev
java -jar workspace-gateway/target/workspace-gateway-1.0.jar --spring.profiles.active=dev
```

- `workspace-system`：61114，注册名 `workspace-system`
- `workspace-auth`：9200，注册名 `workspace-auth`
- `workspace-gateway`：61115

不要重复启动同端口实例。

### 4. 前端

```bash
cd workspace/workspace-ui
npm install
npm run dev
```

访问：

- 应用：`http://localhost:5173`
- Gateway：`http://localhost:61115`

## 配置

主要环境变量：

```bash
JWT_SECRET=<至少 32 字节的随机密钥>
GATEWAY_INTERNAL_TOKEN=<Gateway 下发给 system 的内部凭证>
WORKSPACE_SERVICE_TOKEN=<auth 调 system 的服务凭证>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<按实际环境>
ACCESS_TOKEN_TTL=30m
SESSION_ABSOLUTE_TIMEOUT=12h
DB_URL=jdbc:postgresql://localhost:5432/workspace?currentSchema=system
DB_USERNAME=workspace
DB_PASSWORD=<运行账号密码>
DB_MIGRATION_USERNAME=<迁移账号>
DB_MIGRATION_PASSWORD=<迁移账号密码>
VITE_API_PROXY=http://localhost:61115
```

生产环境必须设置强随机 `JWT_SECRET`、`GATEWAY_INTERNAL_TOKEN`、`WORKSPACE_SERVICE_TOKEN`、数据库和 Redis 凭证，使用 HTTPS，并保持 Generator 关闭。

## 当前能力

- JWT + Redis 统一认证：30 分钟 access token、30 分钟空闲退出、12 小时绝对会话、tokenVersion 撤销、单会话替换。
- 用户、角色、应用菜单、数据权限和按钮权限按应用隔离；同一 userId 可分别配置各应用授权。
- 内置管理员账号和角色保护，前后端双重约束。
- 微前端：qiankun HTML entry、原生 ESM 子应用、深链恢复、宿主与子应用双向路由同步、ready/error 处理、延迟骨架、应用级菜单虚拟分组。
- 工作台：访问统计、近 7 日点击量、PV/UV、TOP 用户和部门分布；分析页支持自动刷新。
- 审计与监控：登录日志、操作日志、在线用户、Redis 缓存监控；日志记录客户端 IP 与 User-Agent。
- 子应用用户同步：按应用范围向已授权子应用同步用户、状态和角色，支持启动全量同步、定时同步和登录时补同步。
- Quartz 任务与日志：Spring Bean Handler 注册、启动全量对账、提交后立即生效。
- Generator：PostgreSQL 单表元数据、后端实体 / mapper / service / controller、form/query/vo、TS API/类型、Art 页面和菜单 SQL。
- 实时通知、附件、WebSocket 数据变更事件、多人协作文档。

## 微前端接入

- 宿主使用 qiankun 3.0.0-rc.22，通过 HTML entry 加载子应用，支持 Vite 原生 ESM 子应用。
- `sys_app.module_entry` 填写子应用入口页 HTML：开发环境如 `http://localhost:3010/`，生产环境如 `/ies/`；不要填 `micro.ts` 或 JS 模块地址。
- `sys_app.style_entry` 仅在子应用单独产出 CSS 文件时填写；HTML 已包含样式时留空，避免重复加载。
- 子应用需暴露 `bootstrap / mount / unmount` 生命周期，并遵守宿主下发的 `basePath`、`authBridge` 和路由事件契约。
- 子应用入口脚本中的动态 `import` 必须使用绝对 URL（如 `window.location.origin + '/eap/micro/ies-micro.js'`），不要使用 `/...` 根路径；qiankun 3 的 classic script 沙箱以 blob 执行，根路径无法解析。

## 日志与 banner

- 启动 banner：`workspace-system/src/main/resources/banner.txt`
- Logback：`workspace-system/src/main/resources/logback-spring.xml`
- 全量日志：`logs/workspace-system/workspace-system.log`
- WARN/ERROR 日志：`logs/workspace-system/workspace-system-error.log`

## 数据库迁移

- `workspace-system/src/main/resources/db/migration/V1__baseline.sql`：唯一完整结构基线。
- `V2` 起为按版本递增的增量迁移，不要跳过版本；以目录内文件为准。
- `sql/seed_user.sql`：独立本地账号初始化。
- `sql/README.md`：初始化与 migrator/运行账号职责说明。

生产环境不要把数据库口令、服务凭证、运行数据或本地环境文件提交到仓库。

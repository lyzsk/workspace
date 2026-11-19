# Workspace

面向内部业务的平台：RuoYi 风格的后端权限与系统管理能力、Art Design Pro 风格的前端，以及可独立接入的微前端子应用。

## 技术栈

- Java 17, Spring Boot 3.5.16, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0, Spring Security 6, MyBatis-Plus 3.5.18.1, PostgreSQL 17, Redis, Maven 3.9+
- Vue 3, Vite 8, TypeScript, Pinia, Vue Router, Element Plus, SCSS
- qiankun 2.10.16, LogicFlow, Excalidraw, Spring WebSocket, Quartz, Velocity
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

认证模型：

- access token 为 HS256 JWT，只包含 `userId / sessionId / tokenVersion / exp`；
- Redis 保存完整 `LoginUser` 会话，`login:user:<sessionId>`；
- 空闲超时由 Redis TTL 控制（30 分钟），绝对上限 12 小时；
- 应用授权缓存在 `authz:<userId>:<appCode>`，子应用只读。

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
cd /c/Users/admin/dev/workspace
mvn -DskipTests package
```

需要手工重建时，由 owner 先创建 schema，再按顺序执行 V1 起的迁移和 `sql/seed_user.sql`。

### 3. 后端（三个进程）

```bash
cd /c/Users/admin/dev/workspace
mvn -DskipTests package

"/c/Program Files/Java/jdk-17.0.18/bin/java.exe" \
  -jar workspace-system/target/workspace-system-1.0-exec.jar \
  --spring.profiles.active=dev

"/c/Program Files/Java/jdk-17.0.18/bin/java.exe" \
  -jar workspace-auth/target/workspace-auth-1.0.jar \
  --spring.profiles.active=dev

"/c/Program Files/Java/jdk-17.0.18/bin/java.exe" \
  -jar workspace-gateway/target/workspace-gateway-1.0.jar \
  --spring.profiles.active=dev
```

- `workspace-system`：61114，注册名 `workspace-system`
- `workspace-auth`：9200，注册名 `workspace-auth`
- `workspace-gateway`：61115

不要重复启动同端口实例。

### 4. 前端

```bash
cd /c/Users/admin/dev/workspace/workspace-ui
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

- JWT + Redis 统一认证：30 分钟 access token、30 分钟空闲退出、12 小时绝对会话、tokenVersion 撤销。
- 用户、角色、应用菜单、数据权限和按钮权限按应用隔离；同一 userId 可分别配置各应用授权。
- 内置 admin、workspace 范围 SUPER_ADMIN、总公司只读保护，前后端双重约束。
- Quartz 任务与日志：Spring Bean Handler 注册、启动全量对账、提交后立即生效。
- Generator：PostgreSQL 单表元数据、后端实体 /mapper/service/controller, form/query/vo, TS API/ 类型、Art 页面和菜单 SQL。
- 微前端：qiankun HTML entry、配置注册、挂载/卸载、统一 AuthBridge 和后端运行权限校验。
- 实时通知、附件、WebSocket 数据变更事件、多人协作文档。

注意：历史 `/eap/src/micro.ts`、`/ws-test/src/micro.ts` 是 single-spa ESM 入口，不能直接作为 qiankun HTML entry；对应子应用和 `sys_app.module_entry` 需要在单独接入任务中迁移。

## 日志与 banner

- 启动 banner：`workspace-system/src/main/resources/banner.txt`
- Logback：`workspace-system/src/main/resources/logback-spring.xml`
- 全量日志：`logs/workspace-system/workspace-system.log`
- WARN/ERROR 日志：`logs/workspace-system/workspace-system-error.log`

## SQL 文件

- `workspace-system/src/main/resources/db/migration/V1__baseline.sql`：唯一完整结构基线。
- `V2__generator_menu.sql`：Generator 菜单权限。
- `V3__internal_user_sync.sql`：内部用户同步索引。
- `V4__app_access_menu_parent.sql`：应用访问按钮父节点修正。
- `V5__opaque_authentication.sql`：历史不透明认证元数据（表保留，当前 JWT 方案不再写入）。
- `V6__app_scoped_authorization.sql`：角色应用归属、用户应用授权、数据范围覆盖及入口授权回填。
- `sql/seed_user.sql`：独立本地账号初始化。
- `sql/README.md`：初始化与 migrator/运行账号职责说明。

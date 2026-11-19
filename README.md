# Workspace

面向内部业务的平台：RuoYi 风格的后端权限与系统管理能力、Art Design Pro 风格的前端，以及可独立接入的微前端子应用。

## 技术栈

- Java 17, Spring Boot 3.5.16, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0, Spring Security 6, MyBatis-Plus 3.5.18.1, PostgreSQL 17, Redis, Maven 3.9+
- Vue 3, Vite 8, TypeScript, Pinia, Vue Router, Element Plus, SCSS
- qiankun 2.10.16, LogicFlow, Excalidraw, Spring WebSocket, Quartz, Velocity
- Knife4j 4.5.0, springdoc 2.8.9

## 模块

```text
workspace/
├─ workspace-common     通用实体、结果、异常、事件、日志注解、持久层基础能力
├─ workspace-system     认证、RBAC、通知、文件、安全配置与数据变更事件
├─ workspace-tool       流程图、脑图、UML、白板及协作 WebSocket
├─ workspace-quartz     sys_job/sys_job_log、Handler 注册、RAMJobStore、cron 生成与校验
├─ workspace-generator  PostgreSQL 元数据、Java/TypeScript/Vue 模板、预览和 ZIP
├─ workspace-gateway    Spring Cloud Gateway、Nacos 路由、introspection、限流，端口 61115
├─ workspace-admin      workspace-service 启动与模块聚合，端口 61114
├─ workspace-ui         Vue 单页基座，开发端口 5173
└─ sql                  初始化说明与独立本地账号 seed
```

依赖方向：`admin → {system, tool, quartz, generator}`；`tool → system`；`quartz/generator → common`。framework 与 system 合并但保留包级边界。`gateway` 独立启动，不依赖 admin/system 的数据库或 Java 实现，只通过 Nacos 与内部认证契约通信。

## 快速启动

### 1. 环境

- JDK 17
- Maven 3.9+
- Node.js 22+
- PostgreSQL 17
- Nacos Server 3.0.3（与当前 Spring Cloud Alibaba 版本配套）
- Redis（认证集群状态与 Gateway 限流）

### 2. 初始化数据库

完整结构只维护在 `workspace-admin/src/main/resources/db/migration/V1__baseline.sql`。需要手工初始化时，由 owner 先创建 schema，再执行 V1 和本地账号 seed：

```bash
cd /c/Users/admin/dev/workspace

"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U postgres -d workspace -c "CREATE SCHEMA system AUTHORIZATION workspace;"

"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U workspace -d workspace -v ON_ERROR_STOP=1 \
  -f workspace-admin/src/main/resources/db/migration/V1__baseline.sql

"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U workspace -d workspace -v ON_ERROR_STOP=1 \
  -f sql/seed_user.sql
```

admin/test 只由本地 seed 创建，口令不以明文记录。已有结构变更使用新的递增迁移，不修改已发布旧版本。

### 3. 后端

```bash
cd /c/Users/admin/dev/workspace
mvn -DskipTests package

"/c/Program Files/Java/jdk-17.0.18/bin/java.exe" \
  -jar workspace-admin/target/workspace-admin-1.0.jar \
  --spring.profiles.active=dev

"/c/Program Files/Java/jdk-17.0.18/bin/java.exe" \
  -jar workspace-gateway/target/workspace-gateway-1.0.jar \
  --spring.profiles.active=dev
```

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
- workspace-service：`http://localhost:61114`（生产环境只允许内部访问）
- 开发接口文档：`http://localhost:5173/api-doc`
- 代码生成：`http://localhost:5173/tools/generator`

## 配置

主要环境变量：

```bash
JWT_SECRET=<旧 JWT 清理前保留；不再用于新登录>
GATEWAY_INTERNAL_TOKEN=<Gateway 与 workspace-service 共用的强随机内部凭证>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<按实际环境>
ACCESS_TOKEN_TTL=30m
SESSION_ABSOLUTE_TIMEOUT=12h
REFRESH_COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost:5173
DB_URL=jdbc:postgresql://localhost:5432/workspace?currentSchema=system
DB_USERNAME=workspace
DB_PASSWORD=<运行账号密码>
DB_MIGRATION_USERNAME=<迁移账号>
DB_MIGRATION_PASSWORD=<迁移账号密码>
VITE_API_PROXY=http://localhost:61115
```

生产环境必须设置强随机 `GATEWAY_INTERNAL_TOKEN`、数据库和 Redis 凭证，使用 HTTPS 并将 `REFRESH_COOKIE_SECURE=true`，保持 Generator 关闭。开发 profile 默认启用 Generator，预览和 ZIP 不会直接覆盖工程源码。

## 当前能力

- 30 分钟不透明 access token、HttpOnly refresh cookie 轮换、12 小时绝对会话、30 分钟空闲超时、集群撤销和 Gateway introspection。
- 用户、角色、应用菜单、数据权限和按钮权限按应用隔离；同一 userId 可分别配置各应用授权。
- 内置 admin、workspace 范围 SUPER_ADMIN、总公司只读保护，前后端双重约束。
- Quartz 任务与日志：Spring Bean Handler 注册、启动全量对账、提交后立即生效、misfirePolicy, cron 生成与未来执行时间预览。
- Generator：PostgreSQL 单表元数据、后端实体 /mapper/service/controller, form/query/vo, TS API/ 类型、Art 页面和菜单 SQL。
- 微前端：qiankun HTML entry、配置注册、挂载/卸载、统一 AuthBridge 和后端运行权限校验。
- 实时通知、附件、WebSocket 数据变更事件、多人协作文档。

协作文档同步按元素 ID 合并，不是 CRDT；操作日志和 revision 持久化后才广播。

注意：历史 `/eap/src/micro.ts`、`/ws-test/src/micro.ts` 是 single-spa ESM 入口，不能直接作为 qiankun HTML entry；对应子应用和 `sys_app.module_entry` 需要在单独接入任务中迁移。

## 日志与 banner

- 启动 banner：`workspace-admin/src/main/resources/banner.txt`，显示项目与 Spring Boot 版本。
- Logback：`workspace-admin/src/main/resources/logback-spring.xml`。
- 全量日志：`logs/workspace-admin/workspace-admin.log`，按天及 100 MB 滚动，保留 30 天，总量上限 3 GB。
- WARN/ERROR 日志：`logs/workspace-admin/workspace-admin-error.log`，保留 60 天，总量上限 1 GB。
- MyBatis SQL 通过 Slf4j 输出，需要排查 SQL 时将 `cn.sichu.quartz.mapper` 调整为 DEBUG，不再直接写控制台。

## SQL 文件

- `workspace-admin/src/main/resources/db/migration/V1__baseline.sql`：唯一完整结构基线。
- `workspace-admin/src/main/resources/db/migration/V2__generator_menu.sql`：Generator 菜单权限。
- `workspace-admin/src/main/resources/db/migration/V4__app_access_menu_parent.sql`：历史应用访问按钮父节点修正。
- `workspace-admin/src/main/resources/db/migration/V5__opaque_authentication.sql`：不透明认证会话与 refresh token 轮换元数据。
- `workspace-admin/src/main/resources/db/migration/V6__app_scoped_authorization.sql`：角色应用归属、用户应用授权、数据范围覆盖及历史入口授权回填。
- `sql/seed_user.sql`：独立本地账号初始化，不影响其他账号。
- `sql/README.md`：初始化和 migrator/ 运行账号职责说明。

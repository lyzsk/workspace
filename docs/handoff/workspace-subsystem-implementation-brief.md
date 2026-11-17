# Workspace 子系统接入实施交接 v1.0

> 日期：2026-11-17
> 读者：k3、1pro
> 目标：按已确定的契约改造 workspace 宿主，并新建 `eap-tool-system` 作为第一个合规子应用。
> 明确不做：制品上传/zip 自动部署（本轮不实现）。

## 0. 先读这三份

1. `docs/specs/workspace-platform-spec.md` — 平台规范
2. `docs/specs/subsystem-integration-spec.md` — 子系统接入规范
3. `docs/cv/2026-11-17-micro-theme-logout.md` — 主题/退出登录补丁

三份文档与本文冲突时，以本文“最终决策”为准；本文未覆盖的细节以两份规范为准。

## 1. 最终决策（不再讨论）

| 项 | 结论 |
|---|---|
| 平台定位 | 薄门户：身份 + 应用注册 + 门户壳 + 平台自身 RBAC |
| 业务权限 | 子应用自己管角色/菜单/字段/数据范围 |
| 应用准入 | `sys_app.access_permission` 仅门户入口可见性，不是 API 鉴权 |
| 菜单 | workspace 管门户菜单；子应用管内部菜单；`sys_menu.app_code` 只用于平台菜单 |
| 用户主数据 | workspace 是唯一源；子应用只读同步 |
| 认证 | workspace HS256 JWT；子应用只验签，不签发 |
| 会话 | 门户模式 workspace 会话唯一权威；默认 12h 绝对上限 + 30m 空闲 |
| 滑动过期 | 宿主监听用户活动，节流调 `POST /api/auth/touch`；子应用 token 短 TTL（≤15m），401 重换 |
| 前端载体 | single-spa；子应用保留自己的 sidebar/header；workspace 应用栏收到 64px |
| 路由 | 子应用 `createWebHistory(basePath)`；宿主同时注册 `basePath` 和 `basePath/**` |
| 主题 | v1 共享 `html.dark`；宿主传入；双向事件同步；不循环 |
| 退出 | 子应用先退自己的会话，发 `app.logout`；宿主退 workspace 并跳 `/login` |
| CSS | 严格隔离：子应用样式 scope 到根容器；禁 `:root/html/body/*`/未限定 `.el-*` |
| 响应体 | `{ code: number, msg, data, bizCode? }`；HTTP status 与 code 一致 |
| 应用注册 | 应用列表前端配置；自动创建 `{code}:access` 权限；动态菜单 + 通配路由 |
| 子应用形态 | 独立后端、独立库/schema、独立前端；新 `eap-tool-system` 为参考实现 |
| 部署 | 本轮只做手工部署；制品上传/自动切换暂不实现 |

## 2. 仓库与路径

| 仓库 | 路径 | 用途 |
|---|---|---|
| workspace | `D:\dev\workspace` | 平台：`workspace-admin/system/common/ui` |
| eap-tool-service | `D:\dev\eap-tool-service` | **历史实现，禁止参考/复制** |
| eap-tool-system | `D:\dev\eap-tool-system` | 本轮新建，第一份合规参考实现 |

仓库约定：

- workspace 有 `.claude/CLAUDE.md`：源码改动默认由用户手动复制。k3/1pro 若未获得用户当次直接改码授权，把补丁写到 `docs/cv/`；获得授权则直接改。
- 服务进程（后端/前端/数据库）由用户手动启动；k3/1pro 只做构建、测试、给启动命令，不代起。
- 不改 `.claude/settings*`；不 git add/commit/push。

## 3. 契约摘要

### 3.1 后端端点

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/auth/workspace-login` | 无 | `{token}` 换子应用 token |
| POST | `/api/auth/login` | 无 | 可选；本地登录 |
| POST | `/api/auth/logout` | 有 | 撤销子应用会话 |
| GET | `/api/user/info` | 有 | 当前用户/角色/按钮 |
| GET | `/api/health` | 无 | 200/UP |
| GET | `/api/authz/effective` | 有 | 可选；权限上下文 |

- 外部路径：`/{code}/api/**`；内部映射 `/api/**`。
- Header：`Authorization: Bearer <token>`；`X-Request-Id` 透传。
- 身份只从 token 来；禁止 `X-User`、`?username=`、请求体 operator。
- 每个业务接口必须本地鉴权；字段级/数据范围服务端校验。

### 3.2 响应体

```json
{ "code": 200, "msg": "ok", "data": {} }
{ "code": 403, "msg": "无权限", "data": null, "bizCode": "TOOL_UPDATE_FORBIDDEN" }
```

- HTTP status 必须与 `code` 一致。
- 成功：200；创建：201；无内容：204。
- 客户端/业务：400、401、403、404、409、422。
- 服务端：500、502、503。
- 不返回堆栈/SQL/内部异常。

### 3.3 微前端 props / 事件

Props：

```ts
{
  domElement: HTMLElement
  basePath: string
  appCode: string
  mode: 'micro'
  getWorkspaceToken: () => string
  theme: 'light' | 'dark'
  locale: string
  user: { username: string; realName?: string }
  navigate: (path: string) => void
  onSessionExpired: () => void
}
```

事件名：`workspace:micro-frontend`；detail 含 `version:1`、`type`、`appCode`、`path`、`time`。

- 宿主 → app：`host.app.entered`、`host.app.left`、`host.app.error`、`host.theme.changed`、`host.locale.changed`、`host.session.expired`、`host.logout`
- app → 宿主：`app.ready`、`app.route.changed`、`app.theme.changed`、`app.logout`、`app.session.expired`、`app.auth.failed`、`app.error`

### 3.4 用户同步 / 密码同步

- workspace 是用户主数据唯一源。
- 子应用 `sys_user` 镜像字段：`workspace_user_id`、`username`、`real_name`、`email`、`mobile`、`gender`、`dept_id`、`status`、`source`、`password_hash`、`synced_at`。
- `source=workspace`：同步用户，只读；`source=local`：break-glass 管理员。
- 同步：SSO 交换 JIT upsert + 定时增量拉取 + 每日对账。
- 密码同步（启用本地登录时）：
  - 平台统一 BCrypt，cost ≥ 12；
  - 仅同步已授予该应用权限的用户；
  - 仅内网、服务凭证、审计；不传明文，哈希不进日志；
  - 改密传播 ≤ 1 分钟。
- 停用/删除传播 ≤ 1 分钟，或下一次 token 刷新拒绝。

### 3.5 路由 / 会话

- 子应用 micro 模式 `createWebHistory(basePath)`；独立模式可用 hash/history。
- 宿主动态路由：`basePath` + `basePath/:pathMatch(.*)*`，meta 带 `appCode`、`basePath`。
- 宿主 `getRuntime` 必须用 `route.meta.basePath`，不能用 `route.path`。
- 会话过期：宿主保存 `pathname+search+hash`，跳 `/login?redirect=...`；登录后回到原 URL。
- 子应用 401：先 `getWorkspaceToken()` 重换；失败发 `app.session.expired`，禁止跳子应用登录页。

## 4. 实施轨道

### Track A：workspace-ui（P0）

| # | 文件 | 改动 | 验收 |
|---|---|---|---|
| A1 | `src/utils/microFrontendEvents.ts` | 事件类型：`host.theme.changed`/`app.theme.changed`/`app.logout` 等；detail 加 `version`、`theme` | 类型编译通过 |
| A2 | `src/utils/singleSpa.ts` | props 增加 `appCode`、`theme`、`getWorkspaceToken`、`mode`、`user`、`navigate`、`onSessionExpired` | 子应用能拿到 |
| A3 | `src/views/apps/detail/MicroFrontendHost.vue` | 用 `route.meta.basePath`；传 props；监听 `app.theme.changed`/`app.logout`；退出跳 `/login` | 主题/退出验收 |
| A4 | `src/store/modules/permission.ts` | 为 `MicroFrontendHost` 菜单同时注册精确路由和 `/:pathMatch(.*)*`；meta 带 appCode/basePath | 深链刷新不 404 |
| A5 | `src/store/modules/app.ts` | `applyTheme(silent)`；广播 `host.theme.changed` | 宿主/子应用主题同步 |
| A6 | `src/composables/useSessionHeartbeat.ts`（新建） | document capture 监听活动，60s 节流调 `POST /auth/touch`；401 走 `handleSessionExpired` | 子应用内活动保持会话 |
| A7 | `src/views/index/index.vue` | 启动 heartbeat；主题按钮保持 | 会话/主题验收 |
| A8 | `src/utils/dataChange.ts` | `PERMISSION_CHANGED_PREFIXES` 加 `/api/system/apps` | 新增应用后菜单自动刷新 |
| A9 | `src/types/api.ts` | `ApiResult.code: number` | 编译通过 |
| A10 | `src/utils/http/index.ts` | 判断 `body.code !== 200`；401/403 分开处理 | 登录/权限错误正确 |
| A11 | `src/api/auth.ts` | 增加 `touch()` | heartbeat 可用 |
| A12 | `src/views/apps/index.vue` | 保存/删除后触发权限刷新 | 应用列表改完菜单生效 |

参考补丁：`docs/cv/2026-11-17-micro-theme-logout.md`（A1/A2/A3/A5 已给出代码）。

### Track B：workspace 后端（P0）

| # | 文件 | 改动 | 验收 |
|---|---|---|---|
| B1 | `workspace-common/.../result/Result.java`、`ResultCode.java`、`IResultCode.java` | `code` 改 `int`；成功 200；通用 400/401/403/404/500 | 全量编译/测试 |
| B2 | `workspace-system/.../code/SystemCode.java` | 35 个业务码映射 HTTP 状态；删 `APP_TYPE_INVALID` | 无 A1028 |
| B3 | `workspace-common/.../exception/GlobalExceptionHandler.java` | 返回对应 HTTP status；业务异常 warn、系统异常 error | 响应 status=code |
| B4 | `.../security/handler/RestAuthenticationEntryPoint.java`、`RestAccessDeniedHandler.java` | 401/403 + 数字 body | 未登录/无权限 |
| B5 | `.../security/token/JwtTokenManager.java` | TTL 改 12h（配置） | token exp 12h |
| B6 | `.../security/service/JwtAuthenticationService.java` | 按 `jti` 查 `sys_user_session`；校验 revoked/expires/last_seen + 30m idle | 空闲过期生效 |
| B7 | `.../auth/controller/AuthController.java` | 增加 `POST /api/auth/touch` | 200 |
| B8 | `.../app/service/impl/SysAppServiceImpl.java` | 创建/修改应用时自动建/改隐藏 BUTTON 菜单（`{code}:access`）；删除时清理 | 新应用权限可分配 |
| B9 | `.../config/SecurityConfig.java` | 放行 `/actuator/health`；`/api/auth/touch` 需认证 | health 无 token 200 |
| B10 | `pom.xml` | 加 actuator | health 可用 |
| B11 | 新建 `RequestIdFilter` | 读取/生成 `X-Request-Id`，MDC + 响应头 | 错误响应带 id |
| B12 | 新建内部用户同步 API | `GET /api/internal/users/sync?updatedAfter=&page=&size=`；服务凭证；仅返回已授予该应用权限的用户；含 BCrypt hash | 子应用能同步 |
| B13 | `V3__*.sql`（如需） | 仅加用户同步所需索引/字段；不做制品表 | Flyway 通过 |

注意：B12 的“已授予该应用权限”需要按 `sys_app.access_permission` 关联 `sys_role_menu/sys_menu/sys_user_role` 查询；不要返回全量用户。

### Track C：eap-tool-system 后端（P0 骨架）

| # | 内容 | 验收 |
|---|---|---|
| C1 | 新建 Maven 项目：JDK17、Spring Boot 3.5.x、Security、MyBatis-Plus、PostgreSQL、Flyway、Actuator | `mvn package` 通过 |
| C2 | 包结构：`auth/user/role/menu/authz/tool/codingrule/fieldconfig/log/common/config` | 无循环依赖 |
| C3 | `WorkspaceTokenVerifier`：HS256、issuer、token_type、exp | 非法 token 401 |
| C4 | `JwtAuthenticationFilter` + `@CurrentUser`；子应用 token 短 TTL | 无 token 401 |
| C5 | `POST /api/auth/workspace-login`、`/api/auth/login`（可选）、`/api/auth/logout`、`GET /api/user/info`、`GET /api/health` | 契约测试通过 |
| C6 | 用户镜像表 + 同步客户端（JIT + 定时拉取） | 停用用户被拒 |
| C7 | 本地角色/菜单/按钮/字段/数据范围表与服务 | 无权限 403 |
| C8 | 统一响应体/异常处理/数字 code | status=code |
| C9 | 审计、登录锁定、request id | 日志可查 |
| C10 | 示例业务：工具台账 CRUD + 字段配置 + 编码规则 | 页面/接口可用 |
| C11 | 契约测试：无 token、伪造 X-User、SSO、停用、越权、health、code | 全部通过 |

禁止：照抄 `eap-tool-service` 的 `X-User`、内存 `TokenStore`、Map controller、PBKDF2。平台标准是 BCrypt。

### Track D：eap-tool-system 前端（P0 骨架）

| # | 内容 | 验收 |
|---|---|---|
| D1 | Vue3 + Vite + TS + Element Plus + Pinia + Router；独立 `main.ts` + `micro.ts` | dev/build 通过 |
| D2 | `micro.ts` 导出 `bootstrap/mount/unmount`；`mount` 用 `props.domElement` | single-spa 可加载 |
| D3 | `microBridge.ts`：micro 标志、主题、事件、logout | 主题/退出验收 |
| D4 | 路由：micro `createWebHistory(basePath)`；独立 hash/history；深链恢复 | 刷新回原页 |
| D5 | Auth store：SSO 交换、短 token、401 重换、`app.session.expired` | 会话过期回登录 |
| D6 | micro 模式隐藏登录/logo/用户菜单；保留自己的 sidebar/header | 无双登录 |
| D7 | 主题/语言双向同步；退出发 `app.logout` | 验收通过 |
| D8 | CSS：根容器 `.app-root`；Tailwind 关 preflight；Element Plus 同版本；禁全局选择器 | 挂载前后宿主样式不变 |
| D9 | 构建：`build:micro` 产出 ESM + CSS；相对 base | 入口/chunk 可加载 |
| D10 | 页面：登录（独立模式）、工具台账、字段配置、编码规则、授权、日志 | 基本可用 |

## 5. 实施顺序

1. **Phase 0**：应用 `docs/cv/2026-11-17-micro-theme-logout.md`，构建通过。
2. **Phase 1**：Track A 的 A1-A5（路由/token/主题/退出），先让单子应用跑通。
3. **Phase 2**：Track A 的 A6-A11（心跳、数字码、应用刷新）。
4. **Phase 3**：Track B 的 B1-B7、B9-B11（数字码、会话、health、request id）。
5. **Phase 4**：Track B 的 B8、B12（应用权限、用户同步）。
6. **Phase 5**：Track C/D 的骨架 + 示例业务。
7. **Phase 6**：契约测试 + 深链/主题/退出/会话验收。

Track A 和 Track B 可并行；Track C/D 依赖 Track B 的用户同步 API 和 Track A 的 props/事件契约。

## 6. 验收测试（必须跑）

1. 无 token 调业务接口 → 401。
2. 伪造 `X-User` → 身份仍取 token。
3. 有效 workspace token 交换 → 200 + 子应用 token。
4. 过期/非法 workspace token → 401。
5. 停用用户交换 → 403。
6. 无本地权限 → 403。
7. `/{code}/api/health` 无 token → 200。
8. 响应 `code` 与 HTTP status 一致。
9. 深链 `/apps/{code}/x/y` 刷新 → 同一页面。
10. 应用 A → B → A → 无残留样式/监听/内存。
11. 子应用切主题 ↔ 平台切主题 → 双向同步、无循环。
12. 子应用退出 → 回到平台 `/login`，不出现子应用登录页。
13. 空闲 30 分钟 → 下一次请求 401，跳登录并带 redirect。
14. 挂载前后宿主 computed style 无变化。
15. 独立运行子应用 → 本地登录/主题/路由正常。

## 7. 禁止事项

- 禁止参考/复制 `eap-tool-service` 的鉴权、TokenStore、Map controller、PBKDF2。
- 禁止信任 `X-User`、`?username=`、请求体 operator。
- 禁止子应用签发 workspace token。
- 禁止在 micro 模式跳子应用登录页。
- 禁止把 workspace token 放 URL/localStorage/日志。
- 禁止全局 CSS（`:root`/`html`/`body`/`*`/未限定 `.el-*`）。
- 禁止读写平台 `system` schema。
- 禁止把平台工具当共享服务。
- 禁止本轮实现制品上传/zip 自动部署。
- 禁止代起服务；服务由用户手动启动。

## 8. 待确认默认值

如果用户没有新的指示，按以下默认执行：

1. 会话绝对上限 12h，空闲 30m。
2. 密码同步范围：仅已授予该应用权限的用户。
3. 本地登录：可选；启用时凭证同步；break-glass 管理员独立密码。
4. `bizCode`：可选，不阻塞 P0。
5. 子系统前端仓库形态：本轮 eap-tool-system 单仓库即可；暂不做 pnpm monorepo。

## 9. 交付报告格式

每个 Phase 完成后报告：

```text
Phase:
改动文件:
构建/测试命令与结果:
验收用例结果:
未完成/风险:
下一步:
```

不要只报“已完成”；必须附构建/测试输出摘要。

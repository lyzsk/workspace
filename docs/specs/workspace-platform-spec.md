# Workspace 平台规范 v1.0

> 状态：草案（待评审）
> 适用范围：workspace 平台自身，以及所有通过门户接入的子系统。
> 关键词：**MUST**（必须）、**MUST NOT**（禁止）、**SHOULD**（建议）、**MAY**（可选）。

## 0. 目的

本规范定义 workspace 作为统一门户时，平台与子系统之间的唯一契约。任何子系统只要满足本规范，即可在不修改 workspace 代码的前提下接入；不满足的子系统不得接入生产。

本规范不描述任何具体子系统的业务实现。现有 `eap-tool-service` 视为历史实现，不作为规范依据。

## 1. 术语

- **平台 / 宿主**：workspace，包含 `workspace-admin`、`workspace-ui`、`sys_app`、JWT、门户壳。
- **子系统 / 子应用**：独立部署的业务系统，拥有自己的后端、数据库、前端。
- **契约面**：跨边界的五件事：身份、注册、传输、前端生命周期、可观测性。
- **门户模式**：子应用通过 workspace 进入，嵌套在门户壳内。
- **独立模式**：子应用不经过 workspace，直接访问自己的地址。

## 2. 职责边界

| 能力 | 平台 | 子系统 |
|---|---|---|
| 用户主数据（账号、姓名、状态、部门） | 唯一源 | 同步副本，只读 |
| 登录 / SSO | IdP，签发 workspace token | 验签 + 换本地会话 |
| 应用注册 / 入口 | `sys_app` + `access_permission` | 无 |
| 门户菜单 | `sys_menu`（平台菜单 + `app:<code>` 入口） | 无 |
| 子应用内部菜单 | 不碰 | 自己管 |
| 子应用角色/字段/数据权限 | 不碰 | 自己管 |
| 业务数据 | 不碰 | 自己的库/schema |
| 平台工具（notice/file/dict/quartz/generator/flow） | 平台自用 | 默认自建 |

硬规则：

- 平台 **MUST NOT** 读取或写入子系统的业务库。
- 子系统 **MUST NOT** 读取或写入平台的 `system` schema。
- 平台工具 **MUST NOT** 被视为共享服务；需要复用 **MUST** 另立平台服务 API，明确鉴权、数据归属、版本，不得共享表或 import 宿主组件。

## 3. 身份与认证

- 平台是唯一 IdP。
- Access token 为 HS256 JWT，claims 必须包含：`iss=workspace`、`sub=<userId>`、`username`、`tenant_id`、`token_version`、`token_type=access`、`jti`、`iat`、`exp`。
- 平台 **MUST** 在登录时签发 access token；**MUST NOT** 在 token 中放入业务权限列表。
- 子系统 **MUST** 只验签，**MUST NOT** 签发 workspace token。
- 共享密钥 **MUST** 通过环境变量或密钥管理分发；**MUST NOT** 进入前端、仓库、日志。
- 身份 **MUST** 来自 token；**MUST NOT** 信任 `X-User`、`?username=`、请求体中的 operator。
- `sys_app.access_permission` **仅**用于门户入口可见性；**MUST NOT** 作为 API 鉴权依据。子系统本地用户/角色才是 API 鉴权边界。
- 迁移触发器：子系统超过 2 个、任一子系统不在可信内网、或密钥泄露，**MUST** 切换到 RS256 + JWKS。

## 4. 会话与过期

- 门户模式下，**平台会话是唯一权威**。
- JWT `exp` 为绝对上限，默认 12 小时，可配置。
- `sys_user_session` 记录 `last_seen_at`、`expires_at`、`revoked`；每次认证请求更新 `last_seen_at`，更新频率 60 秒节流。
- 会话有效条件：未撤销 && `now < expires_at` && `now - last_seen_at < idle_timeout`（默认 30 分钟）。
- 宿主 **MUST** 在用户活动时调用 `POST /api/auth/touch`，60 秒节流；监听范围覆盖 workspace 页面和子应用内活动。
- 子应用 token **MUST** 使用短 TTL（≤ 15 分钟）；401 时 **MUST** 用 `getWorkspaceToken()` 重换；重换失败 **MUST** 发 `app.session.expired`，**MUST NOT** 跳子应用自己的登录页。
- 独立模式：子应用可维护自己的会话，但 **MUST** 同时有 idle timeout 和绝对上限。
- 登出：平台 `POST /api/auth/logout` 撤销会话；宿主清理状态并跳 `/login`；子应用 **MUST** 撤销自己的会话，并发出 `app.logout` 或响应 `host.logout`。

## 5. 应用注册

- `sys_app` 字段：`code`、`name`、`icon`、`base_path`、`module_entry`、`style_entry`、`access_permission`、`sort_no`、`status`、`remark`。
- `code` **MUST** 唯一、稳定、短（建议 ≤ 20 字符），同时作为路径前缀和权限前缀。
- `base_path` 默认 `/apps/{code}`，**MUST** 以 `/` 开头、无尾斜杠、唯一。
- `module_entry` **MUST** 指向可加载的 ESM 入口。
- `style_entry` MAY 为空；不为空时 **MUST** 满足第 10 节 CSS 隔离规范。
- `access_permission` **MUST** 为 `{code}:access`；平台 **MUST** 在创建应用时自动创建隐藏 BUTTON 菜单节点，便于分配角色。
- 平台 **MUST** 在应用 CRUD 后刷新当前用户菜单。
- 平台 **MUST** 生成 `app:<code>` 菜单项和 `MicroFrontendHost` 路由；路由 **MUST** 同时匹配 `base_path` 和 `base_path/**`。
- 应用停用 **MUST** 使 `getRuntime` 拒绝并隐藏入口。
- 应用编码在有菜单引用时 **MUST** 不可修改；`base_path` 和入口可修改，下次菜单刷新生效。

## 6. 门户壳层

- 门户模式 **MUST** 保留窄应用栏（目标 64px），**MUST** 隐藏平台 header/worktabs；子应用拥有自己的内部壳。
- 应用切换 **MUST** 不刷新页面、不影响会话；切换时 **MUST** 卸载旧应用、清理样式和监听。
- 会话过期 **MUST** 保存当前 URL（path + search + hash），跳 `/login?redirect=...`；登录后 **MUST** 回到原 URL。
- 主题和语言由平台作为单一权威；子应用 **MUST** 通过 props/events 同步。
- 子应用退出登录 **MUST** 退出到平台 `/login`。

## 7. 微前端宿主契约

- 技术：single-spa。
- 子应用 entry **MUST** 导出 `bootstrap`、`mount`、`unmount`，MAY 导出 `update`。
- 宿主 props：

| Prop | 类型 | 说明 |
|---|---|---|
| `domElement` | `HTMLElement` | 挂载容器 |
| `basePath` | `string` | 应用路由前缀 |
| `appCode` | `string` | 应用编码 |
| `mode` | `'micro'` | 运行模式 |
| `getWorkspaceToken` | `() => string` | 取当前 workspace token，用于 401 重换 |
| `theme` | `'light' \| 'dark'` | 当前主题 |
| `locale` | `string` | 当前语言 |
| `user` | `{ username, realName? }` | 当前用户 |
| `navigate` | `(path: string) => void` | 宿主导航 |
| `onSessionExpired` | `() => void` | 会话失效回调 |

- 宿主 **MUST** 在 mount 前加载 `style_entry`，在 unmount 后移除。
- 宿主 **MUST** 用 `activeWhen` 匹配 `basePath` 及其子路径。
- 宿主 **MUST** 捕获 import/mount 错误，显示重试，并发出 `host.app.error`。
- 宿主 **MUST NOT** 假设子应用内部路由；子应用自己管理。
- 子应用 **MUST** 在 `mount` 里初始化自己的 router/store，在 `unmount` 里清理。

## 8. 事件契约

- 事件名：`workspace:micro-frontend`（`window` 上的 `CustomEvent`）。
- `detail` 结构：`{ version: 1, type, appCode, path, time, ...payload }`。
- 事件目录：

| type | 方向 | payload | 说明 |
|---|---|---|---|
| `host.app.entered` | 宿主 → app | — | 应用已进入 |
| `host.app.left` | 宿主 → app | — | 应用已离开 |
| `host.app.error` | 宿主 → app | `{ message }` | 加载/挂载失败 |
| `host.theme.changed` | 宿主 → app | `{ theme }` | 主题变化 |
| `host.locale.changed` | 宿主 → app | `{ locale }` | 语言变化 |
| `host.session.expired` | 宿主 → app | — | 会话失效 |
| `host.logout` | 宿主 → app | — | 平台登出 |
| `app.ready` | app → 宿主 | — | 应用初始化完成 |
| `app.route.changed` | app → 宿主 | `{ path, title, titles }` | 子应用路由变化，携带当前页面标题及完整层级标题 |
| `app.theme.changed` | app → 宿主 | `{ theme }` | 子应用切换主题 |
| `app.logout` | app → 宿主 | — | 子应用请求退出 |
| `app.session.expired` | app → 宿主 | — | 子应用会话失效 |
| `app.auth.failed` | app → 宿主 | `{ message }` | 子应用登录交换失败 |
| `app.error` | app → 宿主 | `{ message }` | 子应用运行错误 |

规则：

- 只有当前活动应用的 `appCode` 事件会被处理。
- 主题变更的接收方 **MUST** 应用主题，**MUST NOT** 回发同一事件，避免循环。
- 事件 payload **MUST NOT** 包含 token、密码、完整权限列表等敏感信息。
- 未知事件 **MUST** 被忽略。

## 9. 主题与语言

- 平台主题是单一权威。
- Mount 前通过 props 传入主题；子应用 **MUST** 在首次渲染前应用，避免闪烁。
- v1 主题使用共享的 `html.dark` class，由平台维护；活动子应用在用户点击主题按钮时可以切换该 class，但 **MUST** 立即发出 `app.theme.changed`；平台 **MUST** 只更新 store，不重复切换 class；平台自己的主题按钮 **MUST** 切换 class 并发出 `host.theme.changed`；子应用 **MUST NOT** 再回发。
- v2 改为容器级主题：子应用 **MUST NOT** 直接操作 `html`/`body`。
- 语言同步同理：`host.locale.changed` / `app.locale.changed`（可选）。

## 10. CSS 隔离

- 子应用所有样式 **MUST** scope 到自己的根容器（如 `.app-root` 或 `[data-app="<code>"]`）。
- 禁止选择器：`:root`、`html`、`body`、`*`、未限定的 `.el-*`、`::-webkit-scrollbar`。
- Tailwind **MUST** 关闭 preflight；工具类 **MUST** 加前缀或 scope。
- Element Plus **MUST** 与宿主同 major/minor；主题覆盖 **MUST** scope；teleport 弹层 **MUST** 挂到根容器或使用命名空间。
- `style_entry` 由宿主全局加载，因此子应用 **MUST** 保证无全局选择器；构建 **MUST** 有 lint 拦截。
- 验收：挂载前后，宿主关键元素的 computed style **MUST** 无变化。

## 11. API 传输与响应

- 生产同源；开发通过 Vite 代理。
- 路径：`/{code}/api/**` → 子系统 `/api/**`；静态资源 `/{code}/micro/**`。
- `/api/**` 保留给平台。
- CORS **MUST NOT** 使用 `*`；跨域时 **MUST** 显式 origin。
- Header：`Authorization: Bearer <token>`；`X-Request-Id` **MUST** 透传。
- 响应体：`{ code: int, msg: string, data: T, bizCode?: string }`。
- HTTP status **MUST** 与 `code` 一致。
- `code` 取值：
  - 2xx：200 成功、201 创建、204 无内容；
  - 4xx：400 参数/业务错误、401 未登录、403 无权限、404 不存在、409 冲突、422 业务校验失败；
  - 5xx：500 服务异常、502 上游异常、503 不可用；
  - 3xx 仅用于真实重定向接口，不作为业务错误。
- 时间、编码、ID：`Asia/Shanghai`、UTF-8、Long 序列化为字符串。
- 健康检查：`GET /{code}/api/health` 无鉴权，返回 200/UP。
- API 文档仅 dev 开启。

## 12. 子应用身份交换与用户同步

- SSO 端点：`POST /api/auth/workspace-login`，请求体 `{ token }`。
- 校验：签名、`iss=workspace`、`token_type=access`、`exp`；按 `username` 映射本地用户；用户不存在或停用则拒绝。
- 子应用会话 token **MUST** 在门户模式下使用短 TTL（≤ 15 分钟）。
- 用户同步：
  - 平台是用户主数据唯一源。
  - 子应用维护只读副本：`workspace_user_id`、`username`、`real_name`、`email`、`mobile`、`gender`、`dept_id`、`status`、`updated_at`、`synced_at`。
  - 同步方式：SSO 交换时 JIT upsert 兜底；增量拉取 `GET /api/internal/users/sync?updatedAfter=...`；启动/每日全量对账。
  - 停用/删除传播 **MUST** ≤ 1 分钟，或在下一次 token 刷新时拒绝。
- 密码同步（仅当子应用启用本地登录）：
  - 平台统一使用 BCrypt，cost ≥ 12。
  - 仅同步已授予该应用权限的用户，默认不全量同步。
  - 同步接口 **MUST** 仅内网、服务凭证、审计；**MUST NOT** 暴露给浏览器。
  - **MUST NOT** 同步明文；哈希 **MUST NOT** 进入日志。
  - 改密传播 **MUST** ≤ 1 分钟。
- 本地登录：
  - 可选；两种模式：凭证同步（账号密码与 workspace 一致）或本地 break-glass 管理员（独立密码）。
  - 同步用户没有哈希时，本地登录 **MUST** 拒绝；SSO 仍可用。
- 本地鉴权：每个业务接口 **MUST** 执行本地权限校验；前端隐藏不是授权。

## 13. 部署

- 子应用静态资源：构建 `dist/micro`，部署到 nginx 上 `module_entry` 指向的地址；平台 v1 **不**构建、不上传制品。
- 子应用后端 **MUST** 在 `/{code}/api/**` 可达。
- 健康检查、环境变量、Flyway、自有 schema 为接入前置条件。
- 启用/停用通过 `sys_app.status` 控制；停用后入口隐藏、`getRuntime` 拒绝。

## 14. 可观测性与安全

- 健康检查无鉴权。
- `X-Request-Id` **MUST** 接受并透传，错误响应和日志 **MUST** 包含。
- 结构化日志；**MUST NOT** 记录密码、token、完整身份证号等敏感信息。
- 写操作 **MUST** 审计：who/what/when/before/after。
- 登录 **MUST** 有失败锁定或限流。
- 密钥 **MUST** 来自环境变量；**MUST NOT** 进入仓库。
- 数据库变更 **MUST** 用 Flyway；每个子系统自有 schema。
- single-spa 同 realm：只允许可信的一方可执行 JS；**MUST NOT** 接入第三方不可信代码。
- 共享 HS256 风险：任何持有密钥的子系统都能伪造 workspace token；护栏见第 3 节。
- 密码哈希同步风险：任何一个持有哈希的子系统被攻破，都可能离线爆破账号；护栏见第 12 节。

## 15. 版本与合规

- 本规范版本：v1.0。
- 破坏性变更 **MUST** 升 major 版本；子系统 **MUST** 声明支持的契约版本。
- 子系统“合规”的定义：通过第 16 节的契约测试。
- 契约测试至少覆盖：无 token 401、伪造 `X-User` 无效、SSO 交换成功/失败、停用用户拒绝、本地鉴权 403、health 无鉴权 200、路径前缀、响应体数字码与 HTTP 状态一致、CSS 无全局污染、mount/unmount 无泄漏、深链刷新、主题双向同步、退出回到平台登录。

## 16. 非目标

- 平台不构建、不上传、不托管子应用制品（v1）。
- 平台不共享 notice/file/dict/quartz/generator/flow 等内部工具。
- 平台不管理子应用的业务权限。
- v1 不支持 iframe 子应用；Knife4j 的 iframe 是平台工具页，不是 `sys_app` 条目。
- 平台不自动把租户数据下沉到子应用；租户策略由子系统自行决定。

## 附录 A：契约测试清单

| # | 用例 | 期望 |
|---|---|---|
| 1 | 无 token 调业务接口 | 401 |
| 2 | 伪造 `X-User` 调业务接口 | 身份仍取 token，`X-User` 无效 |
| 3 | 有效 workspace token 换子应用 token | 200，返回子应用 token |
| 4 | 过期/非法 workspace token 交换 | 401 |
| 5 | 停用用户交换 | 403 |
| 6 | 无本地权限调业务接口 | 403 |
| 7 | `GET /{code}/api/health` 无 token | 200/UP |
| 8 | 响应体 `code` 与 HTTP status | 一致 |
| 9 | 应用入口无 `access_permission` | 门户不显示；直接调 API 仍由本地权限决定 |
| 10 | 子应用挂载/卸载 5 次 | 无重复监听、无残留样式、无内存增长 |
| 11 | 深链刷新 `/apps/{code}/x/y` | 回到子应用同一页面 |
| 12 | 子应用切主题 | 平台同步；平台切主题，子应用同步；无循环 |
| 13 | 子应用退出 | 回到平台 `/login`，不出现子应用登录页 |
| 14 | 挂载前后宿主 computed style | 无变化 |
| 15 | 会话空闲 30 分钟 | 下一次请求 401，跳登录并带 redirect |

## 附录 B：待确认项

以下为本规范采用的默认值，评审时可调整：

1. 会话绝对上限默认 12 小时。
2. 空闲超时默认 30 分钟。
3. 密码同步范围默认“仅已授予该应用权限的用户”。
4. 本地登录模式默认“可选”；启用时优先凭证同步，break-glass 管理员独立密码。
5. `bizCode` 为可选字段。
6. 子系统前端仓库形态不在本规范约束范围内；建议 pnpm workspace 或共享 `frontend-kit`。

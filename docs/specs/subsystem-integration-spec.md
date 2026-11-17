# 子系统接入规范 v1.0

> 状态：草案（待评审）
> 配套文档：`workspace-platform-spec.md`
> 关键词：**MUST**（必须）、**MUST NOT**（禁止）、**SHOULD**（建议）、**MAY**（可选）。

## 0. 适用范围

本规范面向所有要接入 workspace 门户的子系统。满足本规范即可在不修改 workspace 代码的前提下接入；不满足不得上生产。

现有 `eap-tool-service` 视为历史实现，不作为参考。未来按本规范实现的 `eap-tool-system` 作为参考实现。

## 1. 接入前决策清单

接入方必须先确定：

| 项 | 说明 |
|---|---|
| `code` | 短、稳定、唯一，建议 ≤ 20 字符，同时作为路径前缀和权限前缀 |
| `basePath` | 默认 `/apps/{code}` |
| `moduleEntry` | 微前端 ESM 入口地址，如 `/{code}/micro/app.js` |
| `styleEntry` | 微前端 CSS 地址，如 `/{code}/micro/app.css`，可为空 |
| `accessPermission` | 固定 `{code}:access` |
| 登录模式 | SSO only / SSO + 本地登录 |
| 本地登录范围 | 仅管理员 break-glass / 凭证同步用户 |
| 会话上限 | 默认 12 小时，空闲 30 分钟 |
| 部署地址 | 前端静态资源、后端 API、健康检查 |

## 2. 后端接入清单

### 2.1 技术栈

- 推荐 Java 17 + Spring Boot 3 + Spring Security + MyBatis-Plus + PostgreSQL/Flyway。
- 允许其他语言/框架，但 **MUST** 满足本规范的协议、响应体、路径、鉴权和健康检查。
- 版本只在子应用自己的根 `pom.xml` / `package.json` 管理，不依赖平台内部模块。

### 2.2 安全过滤器（P0）

- **MUST** 从 `Authorization: Bearer <token>` 解析身份。
- **MUST** 验签 workspace token：签名、`iss=workspace`、`token_type=access`、`exp`。
- **MUST NOT** 信任 `X-User`、`?username=`、请求体中的 operator。
- **MUST** 把身份放入请求上下文，提供 `@CurrentUser` 或等价能力。
- **MUST** 在无 token 时返回 401；token 非法/过期返回 401；用户停用返回 403。
- **MUST** 对每个业务接口执行本地权限校验；前端隐藏不是授权。
- **MUST NOT** 接受前端传入的 `operator` 字段作为审计主体。

### 2.3 必须端点

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/auth/workspace-login` | 无 | 请求 `{token}`，验证 workspace token，返回子应用 token |
| POST | `/api/auth/login` | 无 | 可选；本地登录，启用双模式时提供 |
| POST | `/api/auth/logout` | 有 | 撤销当前子应用会话 |
| GET | `/api/user/info` | 有 | 返回当前用户、角色、按钮权限 |
| GET | `/api/health` | 无 | 返回 200/UP |
| GET | `/api/authz/effective` | 有 | 可选；返回当前用户权限上下文 |

`workspace-login` 规则：

- 校验 workspace token 的签名、issuer、token_type、exp。
- 按 `username` 查找本地用户；不存在或停用 **MUST** 拒绝。
- 签发子应用 token；门户模式下 TTL ≤ 15 分钟。
- 返回至少 `{ token }`，MAY 返回 `refreshToken`。

### 2.4 用户同步

- 平台是用户主数据唯一源；子应用维护只读副本。
- 子应用用户表 **MUST** 至少包含：`workspace_user_id`、`username`、`real_name`、`email`、`mobile`、`gender`、`dept_id`、`status`、`updated_at`、`synced_at`、`source`。
- `source` 取值：`workspace`（同步用户）、`local`（本地 break-glass 管理员）。
- 同步方式：
  1. SSO 交换时 JIT upsert 兜底；
  2. 定时增量拉取 `GET /api/internal/users/sync?updatedAfter=...`；
  3. 启动和每日全量对账。
- 停用/删除 **MUST** ≤ 1 分钟生效，或在下一次 token 刷新时拒绝。
- 子应用 **MUST NOT** 允许前端修改同步用户的姓名、状态、部门等主数据。

### 2.5 本地登录与密码同步（可选）

仅在启用双模式时需要。

- 若要求“账号密码与 workspace 一致”，**MUST** 使用凭证同步：
  - 平台统一 BCrypt，cost ≥ 12；
  - 仅同步已授予该应用权限的用户；
  - 同步接口仅内网、服务凭证、审计；
  - **MUST NOT** 同步明文；哈希 **MUST NOT** 进入日志；
  - 改密传播 ≤ 1 分钟。
- 若只做 break-glass：
  - `source=local` 管理员独立密码；
  - **MUST** 明确标注、限制数量、审计；
  - 密码策略、失败锁定、登录日志 **MUST** 与平台同等级。
- 同步用户没有密码哈希时，本地登录 **MUST** 拒绝，提示从门户进入。
- 本地登录 **MUST NOT** 绕过本地角色/权限校验。

### 2.6 本地权限模型

- 子应用 **MUST** 自己维护角色、菜单、按钮、字段级、数据范围权限。
- 权限码命名 **MUST** 带子应用前缀，如 `{code}:tool:create`，避免跨系统冲突。
- 每个写接口 **MUST** 校验权限；敏感字段 **MUST** 服务端校验，不只靠前端置灰。
- 数据范围 **MUST** 在服务端过滤，不得只在前端筛选。
- 平台 `access_permission` 仅控制门户入口；子应用 **MUST NOT** 依赖它做 API 鉴权。

### 2.7 响应体与错误码

- 响应体：`{ code: int, msg: string, data: T, bizCode?: string }`。
- HTTP status **MUST** 与 `code` 一致。
- 成功：200；创建：201；无内容：204。
- 客户端/业务错误：400、401、403、404、409、422。
- 服务端错误：500、502、503。
- 业务细分 **SHOULD** 使用 `bizCode`，如 `{code: 404, msg: "用户不存在", bizCode: "USER_NOT_FOUND"}`。
- **MUST NOT** 返回堆栈、SQL、内部异常消息。
- 全局异常处理 **MUST** 统一出口；业务异常与系统异常分开记录。

### 2.8 路径与 CORS

- 对外 API **MUST** 在 `/{code}/api/**` 下；内部映射到子应用 `/api/**`。
- 静态资源 **MUST** 在 `/{code}/micro/**` 下。
- 生产 **MUST** 同源；**MUST NOT** 使用 `Access-Control-Allow-Origin: *`。
- 开发跨域 **MUST** 只允许明确的本地 origin。
- `/api/**` 根路径保留给平台，子应用 **MUST NOT** 占用。

### 2.9 数据库与迁移

- 子应用 **MUST** 使用自己的库或 schema。
- **MUST NOT** 读写平台 `system` schema，**MUST NOT** 跨库 join。
- 结构变更 **MUST** 用 Flyway（或等价迁移工具），**MUST NOT** 手工改生产库。
- 主键、审计字段、逻辑删除、时间字段 **SHOULD** 与平台一致。
- Long 型 ID **MUST** 序列化为字符串，避免 JS 精度丢失。

### 2.10 配置与密钥

- 数据库口令、JWT 密钥、服务凭证 **MUST** 来自环境变量或密钥管理。
- **MUST NOT** 提交明文密钥、默认生产口令。
- 启动时 **MUST** 校验生产 profile 的关键密钥非默认值。
- 配置前缀 **SHOULD** 使用应用全名，如 `{code}.jwt.*`。

### 2.11 审计、日志与可观测

- 写操作 **MUST** 审计：操作人、时间、对象、动作、变更前后。
- 日志 **MUST** 结构化；**MUST NOT** 记录密码、token、完整敏感信息。
- **MUST** 接受并透传 `X-Request-Id`；错误响应 **MUST** 返回该 id。
- **MUST** 提供 `GET /api/health`，无鉴权，返回 200/UP。
- **SHOULD** 提供启动、关闭、依赖状态的日志。

### 2.12 契约测试（P0）

后端 **MUST** 提供并跑通以下用例：

1. 无 token 调业务接口 → 401。
2. 伪造 `X-User` → 身份仍取 token。
3. 有效 workspace token 交换 → 200 + 子应用 token。
4. 非法/过期 workspace token → 401。
5. 停用用户交换 → 403。
6. 无本地权限 → 403。
7. 健康检查无 token → 200。
8. `code` 与 HTTP status 一致。
9. 越权字段写入 → 403。
10. 请求体/响应体 Long ID 为字符串。

## 3. 前端接入清单

### 3.1 构建形态

- **MUST** 产出 ESM 微前端入口，导出 `bootstrap`、`mount`、`unmount`。
- **MUST** 产出独立 CSS 文件；入口文件名稳定，chunk 带 hash。
- **SHOULD** 使用 Vite library mode 或等价方案；公共路径为相对路径。
- **MUST** 同时支持独立运行形态（本地登录）和 micro 形态。

### 3.2 生命周期

```ts
export async function bootstrap(): Promise<void> {}
export async function mount(props: MicroProps): Promise<void> {}
export async function unmount(): Promise<void> {}
```

- `mount` **MUST**：创建 app、初始化 store/router、应用主题/locale、交换 token、挂载到 `props.domElement`。
- `unmount` **MUST**：卸载 app、销毁 router、移除事件监听/定时器/teleport 节点、清理容器。
- **MUST NOT** 在 `mount` 外操作宿主 DOM。

### 3.3 Props

- **MUST** 使用 `props.domElement` 作为唯一挂载容器。
- **MUST** 使用 `props.basePath` 作为路由 base。
- **MUST** 使用 `props.getWorkspaceToken()` 获取当前 token；**MUST NOT** 把 token 放进 URL query。
- **MUST** 使用 `props.theme` / `props.locale` 初始化。
- **MUST** 使用 `props.onSessionExpired()` 或发 `app.session.expired` 通知宿主。

### 3.4 路由

- micro 模式 **MUST** 使用 `createWebHistory(basePath)`，**MUST NOT** 使用 memory history。
- **MUST** 支持 `/{basePath}/**` 深链；刷新后 **MUST** 恢复到同一页面。
- **MUST** 在 mount 时根据当前 URL 初始化路由；没有子路径时跳应用首页。
- **MUST NOT** 在 micro 模式修改宿主路由；需要导航时用 `props.navigate()`。
- 独立模式 MAY 使用 hash 或 history。

### 3.5 认证

- micro 模式 **MUST** 用 `props.getWorkspaceToken()` 调 `POST /api/auth/workspace-login` 换取子应用 token。
- **MUST** 把子应用 token 放在内存或 `sessionStorage`，键名带应用前缀；**MUST NOT** 放 URL。
- 401 时 **MUST** 先重换 token；重换失败 **MUST** 发 `app.session.expired`，**MUST NOT** 跳子应用登录页。
- micro 模式 **MUST NOT** 展示本地登录页、注册页、忘记密码页。
- 独立模式 MAY 展示本地登录页。
- **MUST NOT** 把 workspace token 持久化到 `localStorage`。

### 3.6 micro 模式 UI

- 保留自己的 sidebar/header 作为内部导航。
- **MUST** 隐藏属于平台的元素：应用 logo、平台用户菜单、平台退出、平台语言/主题开关（如果平台已提供）。
- **MUST** 保留子应用自己的业务菜单、页面标签、业务工具栏。
- 退出按钮 **MUST** 触发 `app.logout`，由宿主退出到平台登录页。

### 3.7 主题与语言

- mount 前 **MUST** 应用 `props.theme`，避免首屏闪烁。
- 子应用切换主题 **MUST** 发 `app.theme.changed`；**MUST** 响应 `host.theme.changed`；**MUST NOT** 回发同一事件。
- v1 允许活动子应用切换共享的 `html.dark`；v2 改为容器级。
- 语言同步可选；若支持，发 `app.locale.changed` 并响应 `host.locale.changed`。
- **MUST NOT** 在 micro 模式写死 `#fff` 等颜色；使用 CSS 变量。

### 3.8 CSS 隔离

- 所有样式 **MUST** scope 到根容器，如 `.app-root`。
- **MUST NOT** 出现 `:root`、`html`、`body`、`*`、未限定的 `.el-*`、`::-webkit-scrollbar`。
- Tailwind **MUST** 关闭 preflight；工具类 **SHOULD** 加前缀。
- Element Plus **MUST** 与宿主同 major/minor；主题覆盖 **MUST** scope；弹层 **MUST** 挂到根容器或使用命名空间。
- 构建 **MUST** 有 stylelint 或自定义 lint 拦截全局选择器。
- **MUST** 通过挂载前后宿主 computed style 无变化验收。

### 3.9 事件

- 事件名 **MUST** 为 `workspace:micro-frontend`；detail **MUST** 含 `version: 1`、`type`、`appCode`、`path`、`time`。
- 发出：`app.ready`、`app.route.changed`、`app.theme.changed`、`app.logout`、`app.session.expired`、`app.auth.failed`、`app.error`。
- 监听：`host.theme.changed`、`host.locale.changed`、`host.session.expired`、`host.logout`。
- **MUST NOT** 在事件中携带 token、密码、完整权限列表。

### 3.10 清理

`unmount` **MUST** 清理：

- Vue app、router、Pinia 订阅；
- `window`/`document` 事件监听；
- 定时器、轮询、WebSocket；
- Element Plus teleport 弹层；
- 子应用根 class、内联样式；
- sessionStorage/localStorage 中本次会话的临时键。

### 3.11 独立模式

- 独立模式 **MUST** 保持可用：本地登录、自己的主题、自己的路由。
- micro 模式与独立模式 **MUST** 共用同一套业务代码，**MUST NOT** 分叉两套页面。
- 独立模式 **MAY** 使用较长的会话，但 **MUST** 有 idle timeout 和绝对上限。

### 3.12 版本对齐

- Vue、Element Plus、Vue Router、Pinia **SHOULD** 与宿主同 major/minor。
- 若版本不一致，**MUST** 证明 CSS 和运行时隔离；否则 **MUST** 升级对齐。
- 子系统 **MUST** 声明支持的平台契约版本。

## 4. 应用注册清单

平台侧注册时 **MUST** 确认：

- `code` 唯一、稳定、短。
- `basePath` 唯一、以 `/` 开头、无尾斜杠。
- `moduleEntry` 可访问，返回 ESM。
- `styleEntry` 可访问，且通过 CSS 隔离验收。
- `accessPermission` 已创建并分配给相应角色。
- 应用菜单出现在“应用管理”下，普通用户按权限可见。
- 子应用后端 `/{code}/api/health` 可访问。
- 子应用静态资源已部署；平台不负责构建/上传。

## 5. 安全清单

- [ ] 无 token 401；伪造 `X-User` 无效。
- [ ] workspace token 只验签，不签发。
- [ ] 每个业务接口本地鉴权。
- [ ] 字段级/数据范围在服务端校验。
- [ ] 子应用 token 短 TTL，401 重换。
- [ ] 密码同步仅内网、服务凭证、审计；无明文。
- [ ] 密钥来自环境变量；仓库无明文。
- [ ] CORS 无 `*`。
- [ ] 日志无 token/密码。
- [ ] 写操作审计。
- [ ] 登录锁定/限流。

## 6. 验收测试

除第 2.12 节后端用例外，前端 **MUST** 通过：

| # | 用例 | 期望 |
|---|---|---|
| 1 | 从门户进入应用 | 主题/语言/用户正确，无二次登录 |
| 2 | 应用内深链刷新 | 回到同一页面 |
| 3 | 浏览器前进/后退 | 子应用路由正确 |
| 4 | 子应用切主题 | 平台同步，无循环 |
| 5 | 平台切主题 | 子应用同步 |
| 6 | 子应用退出 | 回到平台 `/login`，无子应用登录页 |
| 7 | 会话空闲过期 | 下一次操作跳平台登录并带 redirect |
| 8 | 应用 A → 应用 B → 应用 A | 无残留样式/监听/内存 |
| 9 | 挂载前后宿主 computed style | 无变化 |
| 10 | 独立运行应用 | 本地登录、主题、路由正常 |

## 7. 禁止事项

- **MUST NOT** 信任 `X-User`、query `username`、请求体 operator。
- **MUST NOT** 写全局 CSS（`:root`/`html`/`body`/`*`）。
- **MUST NOT** 在 micro 模式直接操作宿主 `html`/`body`（v1 主题协议除外）。
- **MUST NOT** 把 workspace token 放 URL、localStorage 或日志。
- **MUST NOT** 在 micro 模式跳子应用登录页。
- **MUST NOT** 读写平台 `system` schema。
- **MUST NOT** 签发 workspace token。
- **MUST NOT** 在门户模式使用长 TTL 子应用 token。
- **MUST NOT** 依赖平台 `access_permission` 做 API 鉴权。
- **MUST NOT** 把平台工具当共享服务直接接库。

## 8. 参考实现与模板

- 参考实现：`eap-tool-system`（按本规范新建，作为后续子应用的模板）。
- 模板 **SHOULD** 包含：
  - 后端：安全过滤器、SSO 交换、用户同步消费者、本地权限、统一响应体、健康检查、契约测试；
  - 前端：micro 入口、props/事件桥、history 路由、token 重换、主题/语言同步、退出、CSS 隔离 lint；
  - 部署：nginx 片段、sys_app 注册 SQL、环境变量清单。

## 附录 A：最小接口清单

```
POST /api/auth/workspace-login
POST /api/auth/login              # 可选
POST /api/auth/logout
GET  /api/user/info
GET  /api/health
GET  /api/authz/effective         # 可选
```

## 附录 B：最小配置项

```
{code}.jwt.secret                 # 仅验签 workspace token
{code}.jwt.issuer=workspace
{code}.session.idle-timeout=30m
{code}.session.absolute-timeout=12h
{code}.user-sync.enabled=true
{code}.user-sync.interval=60s
{code}.local-login.enabled=true|false
{code}.local-login.mode=credential-sync|break-glass
```

## 附录 C：待确认项

1. 密码同步范围默认“仅已授予该应用权限的用户”。
2. 本地登录默认可选；启用时优先凭证同步，break-glass 管理员独立密码。
3. 会话绝对上限默认 12 小时，空闲 30 分钟。
4. `bizCode` 为可选字段。
5. 子系统前端仓库形态不在本规范约束范围内；建议 pnpm workspace 或共享 `frontend-kit`。

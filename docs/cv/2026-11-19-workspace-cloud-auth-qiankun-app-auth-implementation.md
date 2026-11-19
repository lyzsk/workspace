# Workspace 微前端、云原生认证与应用授权实施记录

> 日期：2026-11-19  
> 范围：Nacos/Gateway → 不透明认证 → qiankun → 应用授权。  
> 状态：代码与本地数据库迁移完成，离线测试通过；真实 Nacos/Redis/浏览器联调未完成。

## 1. 已实施能力

### Nacos / Gateway

- 新增 `workspace-gateway`，默认端口 `61115`。
- `workspace-admin` 注册为 `workspace-service`，默认端口仍为 `61114`。
- 使用 Spring Boot 3.5.16、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0，解析到 Nacos Client 3.0.3。
- 平台 API/WebSocket 使用 `lb://workspace-service`；业务路由示例在 `workspace-gateway/src/main/resources/nacos/`。
- RequestId、统一数字错误响应、Redis 登录 IP 限流和用户限流。
- 受保护请求统一 introspection；清除伪造身份头，写入可信 userId/sessionId/tokenVersion。
- `/api/internal/**` 不允许经 Gateway 从外部调用；认证服务异常返回 503，不伪装成 401。

### 不透明认证

- access token 为 256 bit 随机不透明字符串，Redis 只保存 SHA-256 哈希对应的运行状态。
- access 默认 30 分钟；会话绝对 12 小时、空闲 30 分钟。
- refresh token 仅通过 HttpOnly Cookie 下发，JSON 不返回 refresh token。
- refresh 使用 PostgreSQL 行锁和条件更新轮换；旧 refresh 重用撤销会话。
- 改密、用户停用/删除、强制下线撤销 PostgreSQL 与 Redis 会话。
- workspace-service 的直接 Bearer、可信 Gateway 身份和 WebSocket 共用同一不透明认证服务。
- 主体按 userId 加载，不再按 username 重载。
- login/refresh/logout 校验允许的 Origin；生产 refresh Cookie 必须 Secure。

### qiankun / AuthBridge

- 主应用使用 `qiankun@2.10.16`，业务代码不再直接使用 single-spa。
- 提供 `getToken/getUser/onTokenChange/onAuthChange/requestRefresh/logout/navigate/getAppAuthorization/onSessionExpired`。
- refresh token 不下发子应用；主应用 single-flight 刷新，HTTP 和业务码 401 共用刷新并重试。
- 跨标签页 refresh 使用 Web Locks 串行化，后端仍执行原子轮换与重用检测。
- 启动恢复、菜单、runtime、refresh 使用 sessionEpoch 或路由序号防止旧响应覆盖。
- 所有退出入口统一调用认证 store；手动/空闲退出广播其他标签页，活动时间跨标签页共享，与共享 refresh 会话保持一致。
- 权限指令可响应权限变化。

### 应用授权

- `sys_role.app_id` 表示角色唯一所属应用。
- `sys_user_app` 表示同一 userId 在某应用的授权状态和用户级数据范围覆盖。
- `sys_user_app_dept` 保存用户级指定部门。
- `sys_menu.app_code` 统一管理 workspace 和各子应用的菜单树、按钮与权限资源。
- 角色—菜单、用户—角色写入均校验应用边界。
- 有效授权要求：全局用户启用、应用启用、用户应用授权启用、至少一个该应用的启用角色。
- 无有效角色时不允许进入，不返回应用菜单；不再把“仅开通”当作可正常使用应用。
- 当前用户与内部业务服务授权接口均返回 roles、permissions、menus 和有效数据范围。
- 用户列表按需展开配置应用授权；角色和菜单管理页可切换应用。
- 平台资料修改与平台角色分配使用不同权限；`SUPER_ADMIN` 只旁路 workspace。

## 2. 主要接口

```http
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout

POST /api/internal/auth/introspect
X-Workspace-Gateway-Token: <内部凭证>

GET /api/auth/me/apps/{appCode}/authorization
GET /api/internal/authorization/apps/{appCode}/users/{userId}

GET /api/system/users/{userId}/app-grants
PUT /api/system/users/{userId}/apps/{appCode}/grant

GET /api/system/roles/list?appCode=<code>
GET /api/system/menus/tree?includeButtons=true&appCode=<code>
```

应用授权 PUT 在一个事务中保存 status、roleIds、dataScope 和 deptIds。启用授权时至少分配一个有效角色。

## 3. 数据库实际执行结果

本地 PostgreSQL 已执行并登记：

```text
V4 app access menu parent   success
V5 opaque authentication    success
V6 app scoped authorization success
```

验证结果：

- `sys_refresh_token` 已建立，当前无存量 refresh token；旧 JWT 会话在切换后需重新登录。
- workspace 与两个现有应用各回填 12 条用户应用授权。
- 新增 `sys:user:app-grant`、`sys:user:assign` 平台权限点。
- 两个历史业务应用当前没有 workspace 管理的应用角色；按新规则，在配置应用角色和菜单前不能进入。
- 迁移前备份：`C:\Users\Sichu Huang\AppData\Local\Temp\workspace-pre-cloud-20261119.dump`。

V6 曾先在 psql 中实际执行，之后通过 Flyway 幂等重跑并登记；Flyway 历史现完整到 V6，没有手工伪造历史。

## 4. 验证结果

- 全仓 Maven：8 个模块成功，**102 个后端测试通过**，0 failure/error/skipped；最终 `mvn -DskipTests package` 成功。
- 前端 Vitest：2 个文件、**7 个用例通过**。
- 前端 `npm run build`：通过；仅有项目既有 SCSS `:deep` 压缩警告。
- `git diff --check`：通过；Git 的 LF/CRLF 提示不是代码错误。
- 实际 PostgreSQL：迁移、授权回填、角色应用归属和内部同步时间查询已验证；Flyway `validate` 成功。

## 5. 独立审查后的修正

- workspace 用户管理的数据范围已统一消费 workspace 有效授权，用户级覆盖不再只停留在查询接口。
- 内部同步将锁定用户视为无效，并在锁定/解锁/强制下线时更新可同步的变更时间。
- Gateway 对空 introspection 成功响应返回 503，不允许空 200 冒充已处理请求。
- 修复角色编辑、角色/菜单应用切换、授权面板角色加载的慢响应覆盖。
- 空闲活动与共享 refresh 会话保持一致，退出广播携带原因。
- qiankun 样式按应用与地址共享加载 Promise 并引用计数，旧实例不会删除新实例仍在使用的样式。
- 用户展开行支持多行同时展开；权限指令使用最新权限码。
- 临时恢复故障不再中断首屏导航，但旧会话响应仍会被拒绝。

## 6. 配置与启动边界

统一内部凭证：

```text
GATEWAY_INTERNAL_TOKEN=<Gateway 与 workspace-service 相同的强随机值>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<按环境>
```

前端开发代理默认已改为 `http://localhost:61115`。生产只允许 Gateway 暴露业务入口，workspace-service 和业务服务端口限内部访问。

本次没有启动或停止服务。本机执行时 Nacos 8848、Redis 6379 未监听，因此以下均未验证：

- 真实 Nacos 注册、摘流和配置刷新。
- 真实 Redis Lua、限流和刷新/撤销的跨实例行为。
- 新版登录、refresh、logout 的浏览器端到端流程。
- qiankun 实际挂载、深链和主子应用交互。

## 7. 现有子应用与后续 blocker

- 历史 `/eap/src/micro.ts`、`/ws-test/src/micro.ts` 是 single-spa ESM 入口，不能直接作为 qiankun HTML entry；需在单独接入任务中迁移子应用和 `sys_app.module_entry`。
- 现有应用需要在 workspace 菜单/角色管理中建立该应用的菜单树和角色，再分配给用户；旧 `<appCode>:access` 不再是新链路的授权依据。
- 集群生产上线前仍需完成 Quartz JDBC Cluster、Redis WebSocket 跨实例广播、共享文件存储、唯一雪花 workerId 和独立迁移发布步骤。

## 8. 置信度

- 代码编译、单元测试和本地迁移结果：高置信度。
- 应用隔离与认证并发：已做离线覆盖，真实中间件行为待集成验证。
- 现有子应用兼容性：未验证且未宣称兼容；入口和中央菜单/角色均需迁移配置。

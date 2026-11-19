# Workspace 微前端、统一认证与应用权限重构计划 v2

> 日期：2026-11-19  
> 状态：前四阶段代码已实施；真实中间件与浏览器联调见实施记录，集群补齐尚未实施。  
> 范围：workspace 主应用、平台后端、Gateway/Nacos、认证与应用权限；暂不改造现有已接入子应用。  
> 说明：本文件记录目标架构与规则；实际完成情况见 `2026-11-19-workspace-cloud-auth-qiankun-app-auth-implementation.md`。

## 0. 修正后的硬性目标

以下均为**必须实现**，不再作为可选方案：

1. 前端主应用使用 **qiankun**，负责子应用注册、加载、挂载和卸载。
2. workspace 主应用是唯一前端认证状态源，向子应用提供 `getToken / onTokenChange / logout` 等能力。
3. 后端引入 **Nacos 服务注册与发现**，业务服务和 workspace 服务均注册到 Nacos。
4. 引入 **Spring Cloud Gateway** 作为统一后端入口，按服务名路由，例如 `/api/app-a/** → lb://service-a`。
5. 浏览器使用**不透明 access token**；Gateway 调用认证服务完成 **introspection**，不在 Gateway 本地解 JWT。
6. 认证服务统一签发、刷新、撤销 token；多个实例共享会话状态，支持集群。
7. Gateway 只回答“身份是否有效”，业务服务继续执行功能权限、数据权限和业务规则。
8. 用户管理实现“同一个 userId 在不同应用拥有不同授权”，不创建子账号、不单独登录、不单独发应用 token。
9. 统一处理 401、刷新风暴、退出通知、后端鉴权、开发跨域/资源路径、身份头防伪造。
10. 集群上线前同时处理 Quartz、WebSocket、本地文件和雪花 ID，不能只改 token。

目标调用链固定为：

```text
登录
→ 认证服务签发不透明 access token + refresh 凭证
→ workspace 主应用保存认证状态
→ qiankun 挂载子应用并注入认证能力
→ 子应用携带 Authorization: Bearer <opaque-token>
→ Gateway introspection
→ Nacos 服务发现，lb:// 路由到目标服务
→ 目标服务建立可信用户上下文
→ 目标服务按 userId + appId 执行业务/数据权限
```

---

## 1. 目标总体架构

```text
┌──────────────────────────── 浏览器 ────────────────────────────┐
│ workspace-ui（qiankun 主应用）                                │
│  登录 / 恢复 / 刷新 / 退出 / 用户信息                         │
│  平台菜单 / 应用入口 / 应用授权管理                           │
│  AuthBridge：getToken/onTokenChange/requestRefresh/logout     │
│  qiankun：注册 / 加载 / 挂载 / 卸载 / 错误恢复                │
│       ┌────────────────────┬────────────────────┐             │
│       │ 子应用 A            │ 子应用 B            │             │
│       │ 不维护登录          │ 不维护登录          │             │
│       └────────────────────┴────────────────────┘             │
└───────────────────────────┬───────────────────────────────────┘
                            │ Bearer opaque token / refresh cookie
                            ▼
┌──────────────── Spring Cloud Gateway 集群 ─────────────────────┐
│ RequestId / 限流 / 统一 401、503                               │
│ 清除伪造身份头 → 调认证服务 introspection → 写入可信身份上下文 │
│ /api/auth/**、/api/system/** → lb://workspace-service          │
│ /api/app-a/**                  → lb://service-a                │
│ /api/app-b/**                  → lb://service-b                │
└───────────────────────────┬────────────────────────────────────┘
                            │ 服务发现 / 配置
                ┌───────────▼────────────┐
                │        Nacos           │
                │ Discovery + Config     │
                └───────────┬────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
┌──────────────────┐ ┌───────────────┐ ┌───────────────┐
│ workspace-service │ │ 业务服务 A     │ │ 业务服务 B     │
│ 当前 admin 演进   │ │ 业务/数据权限  │ │ 业务/数据权限  │
│ 认证 + 用户主数据 │ │ 不签发 token   │ │ 不签发 token   │
│ 应用注册 + RBAC   │ └───────┬───────┘ └───────┬───────┘
└────────┬─────────┘         │                  │
         │                   └──── Nacos ───────┘
         ▼
  PostgreSQL + Redis
  持久授权/会话记录   集群 token、会话、撤销、限流、事件
```

边界必须保持：

- qiankun 不管后端服务发现；Nacos 不加载前端资源；Gateway 不管理前端生命周期。
- Gateway 不用菜单、角色或 qiankun props 代替业务鉴权。
- 一个前端应用可以调用多个后端服务，前后端应用不是一一对应关系。
- `sys_app` 管前端注册与授权；Gateway 路由配置管后端路径与服务名，两者通过接入清单对应，不混成一件事。

---

## 2. 代码模块怎么调整

### 2.1 目标模块

```text
workspace/
├─ workspace-gateway       新增：Gateway 启动模块，Reactive，不访问业务数据库
├─ workspace-admin         保留：workspace-service 启动与模块聚合，注册到 Nacos
├─ workspace-system        认证、会话、用户、应用、RBAC、应用授权、内部授权查询
├─ workspace-common        通用基础类型；不放 Gateway 路由或认证状态实现
├─ workspace-tool          现有工具能力
├─ workspace-quartz        改为集群 JobStore
├─ workspace-generator     现有生成器，生产继续关闭
└─ workspace-ui            qiankun 主应用 + 唯一 AuthBridge
```

### 2.2 认证服务第一版的物理边界

推荐采用**逻辑独立、暂不单独拆进程**：

- 当前 `workspace-admin` 注册为 `workspace-service`。
- `workspace-system.auth/security` 中的认证、会话、introspection 是独立领域边界。
- Gateway 只依赖 `/internal/auth/introspect` 契约，不依赖 workspace 的 Java 类或数据库。
- 将来拆出独立 `workspace-auth` 进程时，Gateway 和业务服务不需要改协议。

理由：用户、密码、会话和 tokenVersion 已经在 workspace；第一版立刻拆成独立进程会引入用户主数据归属和跨服务一致性问题。这里的“认证服务”是明确的服务能力，不要求第一天就增加一个仓库。

如果你要求认证服务从第一版就独立部署，则模块和数据库归属需要另拆，见第 11 节。

### 2.3 依赖与版本

- 根 `pom.xml` 增加 Spring Cloud、Spring Cloud Alibaba 的 BOM，统一锁定 Gateway、Nacos、LoadBalancer 版本。
- 新增模块只声明依赖，不写独立版本。
- Spring Boot 3.5.16、Spring Cloud、Spring Cloud Alibaba、Nacos Server 的版本必须按官方兼容矩阵核实后锁定，不在计划中猜版本号。
- Gateway 使用 WebFlux；现有 workspace/business 服务继续保持 Spring MVC，不把业务模块改成 Reactive。
- 增加 Spring Data Redis；不引入 Redisson，除非后续出现现有客户端无法完成的原子操作。

---

## 3. Nacos + Gateway 改造

### 3.1 服务注册与配置

- `workspace-service`、各业务服务启动时注册到 Nacos。
- Gateway 通过 Nacos Discovery 获取健康实例，用 `lb://service-name` 路由。
- Gateway 路由、限流、introspection 超时等共享配置放 Nacos Config。
- 数据库、Redis、Nacos 凭证仍优先来自环境变量或密钥管理，不写进仓库，也不因为用了 Nacos 就把密钥公开到普通配置。
- 每个服务有独立 `spring.application.name`、健康检查和优雅下线；发布时先从 Nacos 摘流，再停实例。

### 3.2 Gateway 过滤器顺序

```text
1. RequestId：接受合法 X-Request-Id 或生成，写日志并透传
2. 基础限流：登录等公开端点按 IP；已认证请求按 userId
3. 清除外部身份头：X-Workspace-User-Id / Session-Id / Tenant-Id 等
4. 白名单判断：login、refresh、health 等按明确规则处理
5. 提取 Bearer opaque token
6. 调 workspace-service /internal/auth/introspect
7. active=false → 401；认证服务不可用/超时 → 503，不伪装成 401
8. 写入可信身份头，移除向下游传递的浏览器 Authorization
9. lb:// 路由到 workspace-service 或业务服务
```

要求：

- introspection 使用服务间凭证和内网地址，不暴露为浏览器接口。
- Gateway 内部调用使用 Nacos `lb://workspace-service`，不经过自身过滤器形成循环。
- 第一版 **每次请求 introspection，不做 Gateway 权限/token 结果缓存**，保证撤销及时；确有性能数据后再评估极短缓存及主动失效。
- 业务服务端口不得对浏览器/公网开放，否则可绕过 Gateway。
- 下游服务只接受来自 Gateway 的可信身份上下文；外部同名头必须在 Gateway 和下游两处防御。
- HTTP/WebSocket 都走同一认证语义；WebSocket 握手鉴权失败返回 401，不把 token 写日志。

### 3.3 路由示例

```text
/api/auth/**、/api/system/**、/ws/** → lb://workspace-service
/api/app-a/**                         → lb://service-a
/api/app-b/**                         → lb://service-b
```

更具体的业务路由必须先于平台 `/api/**` 路由匹配。路径重写、前缀保留/移除、WebSocket 路由都在 Nacos 配置中显式定义，不能依赖隐式默认值。

---

## 4. 不透明 token、introspection 与集群会话

### 4.1 替换当前 JWT 认证

当前代码使用 HS256 JWT，并在每个实例中按 JTI 查数据库会话：

- token 创建：`workspace-system/src/main/java/cn/sichu/system/security/token/JwtTokenManager.java`
- 请求认证：`workspace-system/src/main/java/cn/sichu/system/security/service/JwtAuthenticationService.java`
- 配置：`workspace-admin/src/main/resources/application.yml:9-14`

目标改为：

- access token 是密码学安全随机生成的不透明字符串，至少 256 bit 熵。
- 浏览器只看到原始 token；服务端只保存 token 哈希，不保存可恢复的明文。
- access token 本身不携带可解析的用户、角色或权限 claims。
- introspection 返回：`active、userId、username、tenantId、sessionId、tokenVersion、expiresAt`。
- 角色、应用权限、数据范围不放入 introspection 基础响应，避免 Gateway 变成业务授权中心。
- 旧 JWT 黑名单逻辑由“会话撤销 + access token 撤销 + refresh 轮换/撤销”替代，稳定后删除 JJWT 依赖和死代码。

### 4.2 Redis 与 PostgreSQL 的职责

| 数据 | PostgreSQL | Redis |
|---|---|---|
| 用户、角色、应用授权 | 持久事实源 | 仅可做有失效机制的短期缓存 |
| 登录会话、refresh family、撤销记录 | 持久记录/审计 | 集群运行时状态与快速查询 |
| access token | 哈希元数据/审计（按最终表设计） | 哈希索引、TTL、revoked、session 绑定 |
| 登录失败锁定 | 可保留审计 | 原子计数和过期 |
| 在线状态、lastSeenAt | 定期/关键事件持久化 | 实时集群状态 |
| 授权变更事件 | 审计日志 | Pub/Sub 通知各实例失效缓存 |

- 所有认证实例共享 Redis，不使用本地 ConcurrentHashMap 保存权威 token/session 状态。
- Redis 故障不能返回“用户未登录”：认证服务异常时 Gateway 返回 503，避免把基础设施故障放大成全量退出。
- 第一版以“撤销正确、失败关闭”为优先，再依据压测决定是否增加数据库旁路或缓存。

### 4.3 access token 与 refresh 凭证

推荐默认策略：

- access token：30 分钟。
- 会话绝对上限：12 小时。
- 空闲超时：30 分钟。
- refresh token：仅放在 `HttpOnly + Secure（生产）+ SameSite` Cookie，路径限制到认证端点，不进入 Pinia、sessionStorage 或 qiankun props。
- 每次 refresh 轮换 refresh token，并原子更新会话；检测到 refresh token 重用时撤销整个 token family。
- 多标签页并发刷新必须由后端原子操作保证安全，前端 single-flight 只是减少请求，不能代替后端并发控制。
- 改密、全局停用、强制下线撤销该用户会话；单应用撤权只撤销该应用授权，不登出全局账号。

### 4.4 introspection 接口

概念契约：

```http
POST /internal/auth/introspect
Authorization/服务凭证: <gateway-service-credential>
Content-Type: application/json

{ "token": "<opaque-access-token>" }
```

响应只描述身份状态：

```text
active
userId / username / tenantId
sessionId / tokenVersion
expiresAt
```

认证服务内部检查：token 哈希、TTL、session revoked、绝对/空闲过期、用户状态、tokenVersion。Gateway 不读取数据库，也不信任请求体或外部身份头。

### 4.5 下游可信身份

- Gateway 先删除所有外部伪造的 `X-Workspace-*` 身份头，再写入 introspection 结果。
- 生产网络只允许 Gateway 访问内部业务端口；需要更强保证时启用 mTLS 或内部身份签名。
- 下游用 `userId` 加载用户和权限，并断言会话/用户状态，不再按 username 猜测主体。
- 现有 `JwtAuthenticationFilter` 演进为可信身份过滤器；开发直连与生产 Gateway 模式必须明确隔离，生产不能保留无保护直连。
- 当前 token 校验后按 username 重载用户的逻辑改为按 userId：`JwtAuthenticationService.java:35-51`。

---

## 5. qiankun 与主应用认证桥

### 5.1 从 single-spa 切到 qiankun

当前直接使用 single-spa：`workspace-ui/src/utils/singleSpa.ts`、`workspace-ui/src/views/apps/detail/MicroFrontendHost.vue`。

改造为：

```text
microfrontend/
├─ registry          sys_app → qiankun 注册配置、activeRule、container
├─ lifecycle         加载、挂载、卸载、样式清理、错误重试
├─ hostBridge        唯一主子应用能力契约
└─ events            类型化事件，不承载 token/完整权限清单
```

- 移除业务代码对 single-spa API 的直接使用，统一走 qiankun；不保留两套宿主运行时。
- `MicroFrontendHost` 只负责当前路由对应的容器、加载状态和错误展示。
- 动态应用注册来自后端可见应用列表；`appCode/basePath` 与 runtime 配置必须一致。
- 新契约中 `sys_app.module_entry` 必须是 qiankun 可加载的 HTML 文档地址；现有 `/eap/src/micro.ts`、`/ws-test/src/micro.ts` 等 single-spa ESM 地址不能直接视为兼容入口，需后续迁移子应用并更新入口配置，本阶段不改子应用和既有入口值。
- 加载请求带序号/取消保护，快速切换时旧响应不能覆盖新应用。
- unmount 必须清理子应用监听、定时器、样式和宿主订阅。
- 沙箱和样式隔离是运行约束，不被当成安全边界；第一版只接入可信内部代码。

### 5.2 主应用向子应用提供的能力

```ts
getToken(): string | null
getUser(): HostUserSummary
onTokenChange(callback): () => void
onAuthChange(callback): () => void
requestRefresh(): Promise<void>
logout(reason?): Promise<void>
navigate(path): void
getAppAuthorization(): Promise<AppAuthorization>
onSessionExpired(callback): () => void
```

规则：

- `getToken` 每次读主应用当前 token，不在 mount 时复制一份永久 token。
- `onTokenChange/onAuthChange` 返回取消订阅函数，unmount 时必须调用。
- 子应用收到 401 后调用 `requestRefresh`，由主应用统一刷新；**不向子应用暴露 refresh token**。
- 多个子应用/主应用同时 401 时共享同一个刷新 Promise；刷新成功后统一点发 token 变化。
- 刷新失败才执行全局 expire/logout；403 不刷新、不退出。
- 主应用退出时先广播认证变化/宿主退出，再卸载应用、清理状态并撤销服务端会话。
- props 中的用户和权限只用于展示；业务服务不得信任它们。

### 5.3 主应用自身认证状态收口

保留一个认证 store，并增加：

```text
anonymous / restoring / authenticated / refreshing / expired
sessionEpoch
restorePromise / refreshPromise / logoutPromise
```

统一 `login / restore / refresh / logout / expire` 五个动作。HTTP 401、业务码 401、心跳、用户菜单退出、修改密码、子应用退出都走这些动作，不再各自组合清理逻辑。

- 启动恢复和菜单加载等待同一个会话边界。
- 旧会话的 `getMe`、菜单、runtime、refresh 响应通过 `sessionEpoch` 失效。
- 新导航快照完整获取成功后再替换旧菜单和路由。
- token 可以继续以 access token 形式供主应用和可信子应用使用；refresh 凭证永不下发 JavaScript。

---

## 6. 用户—应用授权与业务鉴权

### 6.1 数据模型

继续采用复用现有 RBAC 的方案，而不是另起一套平行角色 CRUD：

| 表/字段 | 目标 |
|---|---|
| `sys_app` | 应用注册、全局启停、qiankun 前端配置；显式注册受保护的 `workspace` |
| `sys_role` | 新增 `app_id`，角色只属于一个应用 |
| `sys_menu` | 复用 `app_code`，统一管理 workspace 与各子应用的菜单树、按钮和权限资源 |
| `sys_user_role` | 不变，通过 `role.app_id` 推导应用 |
| `sys_role_menu` | 不变，写入时校验角色与权限资源同应用 |
| `sys_user_app` | 新增：userId + appId + 应用内启停 + 可空用户级数据范围覆盖 |
| `sys_user_app_dept` | 用户级“指定部门”覆盖启用时保存部门集合 |
| `sys_tenant_app` | 不复用；它不是用户应用授权 |

目标页面：

```text
▾ 测试用户 TEST  全局状态：启用
  ├─ workspace  平台角色：普通用户  数据权限：仅本人
  ├─ 应用 A     授权：启用  角色：管理员  数据权限：本部门 [配置]
  └─ 应用 B     授权：启用  角色：访客    数据权限：仅本人 [配置]
```

每一行都是同一 `userId` 的授权关系；没有新账号、新密码、新 token。

### 6.2 统一授权规则

```text
可进入应用 = 用户全局启用
           AND 应用全局启用
           AND 用户应用授权存在且启用

应用功能权限 = 该用户在该应用下启用角色的权限并集

应用数据范围 = 用户应用授权有覆盖值时使用覆盖值
             否则只合并该应用内角色的数据范围
```

- 已开通但没有有效角色时不允许进入；该应用无可用菜单和功能权限。
- 停用应用授权保留角色配置；删除授权才清理该应用角色关系。
- 平台角色与应用角色不合并，跨应用绝不合并数据范围。
- 不做用户级额外 allow/deny 权限点，用户只通过角色获得功能权限。
- 推荐 `SUPER_ADMIN` 只在 workspace 全量放行；业务应用仍需显式授权，避免应用停用/降权被旁路。
- 应用里的“管理员”只是业务角色，不自动获得 workspace 的用户/角色管理权限。

### 6.3 workspace 侧统一授权服务

建立一个按 `userId + appId` 查询的授权服务，供以下位置共同使用：

- 平台方法权限和数据范围；
- 应用 runtime 准入；
- 平台菜单和应用入口；
- 当前用户应用授权查询；
- 内部用户同步；
- 业务服务的服务端授权查询。

替换当前 runtime、菜单、内部同步三处独立解释 `access_permission` 的逻辑：

- `SysAppServiceImpl.java:49-65`
- `MenuServiceImpl.java:66-96`
- `InternalUserSyncMapper.xml:7-18`

### 6.4 业务服务如何执行权限

业务服务收到 Gateway 写入的可信 userId 后：

1. 从自身配置确定当前 `appCode`，不信任浏览器任意传入的 appCode。
2. 调 workspace 的内部授权接口获取该用户在该应用的启用状态、角色、权限、菜单树和数据范围。
3. 在业务方法执行功能权限和数据条件；菜单隐藏或 qiankun props 都不算授权。
4. 本地缓存必须有 TTL 和授权变更失效；第一版也可以直接查询，先保证正确再优化。
5. workspace 发布 `authz.changed(userId, appCode, version)`，各实例/服务使对应缓存失效；事件不携带完整权限清单。

概念接口：

```http
# 浏览器查看当前自己在某应用的授权
GET /api/auth/me/apps/{appCode}/authorization

# 业务服务服务端鉴权
GET /api/internal/authorization/apps/{appCode}/users/{userId}

# 用户应用授权管理
GET /api/system/users/{userId}/app-grants
PUT /api/system/users/{userId}/apps/{appCode}/grant
# 原子提交 status、roleIds、dataScope、deptIds
```

`/api/auth/me` 保留响应外形，但 roles/permissions 明确只表示 workspace 平台权限，不再返回跨应用并集。

### 6.5 写入保护

- 用户平台角色更新只修改 workspace 角色，不能删除其他应用角色。
- 应用角色分配校验 grant、角色归属、角色状态和应用归属。
- 角色绑定权限资源时校验同应用。
- 平台资料修改与角色分配分权；只有平台超级管理员或专门权限可以分配 `SUPER_ADMIN`。
- 全局用户停用撤销全局会话；应用授权停用不撤销全局 token，但 runtime、业务鉴权和内部同步都必须体现撤权。
- 内部同步不能把停用记录简单从增量结果中删除，否则接收方收不到撤权；状态传播和 tombstone/全量对账必须明确验收。

---

## 7. 集群化必须同步处理的其他代码

这些不是可选优化，而是多实例生产上线 blocker：

| 当前实现 | 集群问题 | 改法 |
|---|---|---|
| Quartz `job-store-type: memory`，`application.yml:47-48` | 每个实例重复执行 | 改 PostgreSQL JDBC JobStore、开启 Quartz cluster、增加 QRTZ 迁移；验证 misfire/recovery |
| WebSocket sessions 在实例内 `ConcurrentHashMap`，`NotificationWebSocketHandler.java:23-27` | 其他实例产生的数据变更到不了本机连接 | Redis Pub/Sub 广播到所有实例，本机只负责自己的连接发送 |
| `LocalFileStorage` 写 `./data/files`，`LocalFileStorage.java:20-49` | 上传到 A 后，从 B 读不到 | 复用 `FileStorage` 接口切换 MinIO/S3 或可靠共享存储；生产禁止单实例本地目录 |
| 雪花 worker/datacenter 默认均为 0，`IdConfig.java:17-20` | 多实例可能生成重复 ID | 每个实例分配唯一 workerId/datacenterId；动态扩容时用带租约的分配器，启动冲突直接失败 |
| 多实例启动同时 Flyway | 并发迁移和发布顺序风险 | 迁移作为独立发布步骤，完成后再扩容应用实例 |
| 本地权限/在线缓存 | 撤销和状态不一致 | 权威状态放 Redis/PostgreSQL；本地缓存必须有版本与失效通知 |

Nacos、Redis、PostgreSQL 自身的高可用和备份属于部署验收，不以“进程能启动”代替集群验证。

---

## 8. 数据库与旧代码迁移

只新增递增迁移，不修改 V1～V4。

### 8.1 建议拆分迁移批次

1. **认证迁移**：不透明 token 元数据、refresh family、会话字段、Redis 初始化/回放所需数据。
2. **应用授权迁移**：`sys_role.app_id`、`sys_user_app`、用户级数据范围关系、`workspace` 注册。
3. **Quartz 集群迁移**：QRTZ 表和调度配置。
4. **清理迁移**：新链路稳定后再删除旧 JWT 黑名单/旧访问权限等死结构，不在第一轮 DROP。

### 8.2 实际库预检

- 同名有效 username、孤儿用户角色/角色菜单、跨应用角色资源。
- `platform/workspace` 菜单编码分布。
- 是否已有手工注册的 workspace 应用。
- 当前 SUPER_ADMIN 和各应用入口授权清单。
- 现有会话是否允许全部失效并强制重新登录。
- Quartz 内存任务定义如何完整转入 JDBC JobStore。

### 8.3 切换原则

- 推荐认证切换时**旧 JWT 会话全部失效、用户重新登录**，不长期维护 JWT + opaque 双栈。
- 新旧认证代码不能混跑在同一个 Nacos 服务名下；按版本分批发布或独立服务名灰度。
- 普通用户按旧 `<appCode>:access` 链回填应用授权，保持入口清单一致。
- SUPER_ADMIN 可一次性回填现有应用授权以保持入口可达，但不形成以后新增应用的自动旁路。
- 旧 `access_permission` 和访问按钮先保留用于核对，所有新读取切换后才在后续版本清理。
- 回滚不能只回滚 jar： opaque token、refresh family、应用授权变化都需要明确回滚边界。

---

## 9. 分阶段实施与验收

| 阶段 | 代码产出 | 必须通过的验收 |
|---|---|---|
| 0. 基线与契约 | 锁定版本兼容矩阵；补现有登录/会话/权限表征测试；冻结 API、身份头、事件契约 | 当前行为可回归；新旧规范冲突有明确替代关系 |
| 1. Nacos + Gateway 骨架 | 新 gateway 模块、workspace 注册、路由、RequestId、健康检查、统一异常 | 用最小演示服务验证 `lb://`；重启/摘除实例后路由正确 |
| 2. 不透明认证 | Redis、token 签发/刷新/轮换/撤销、introspection、可信身份过滤器 | Gateway 每次 introspection；伪造头无效；撤销立即失效；认证故障返回 503 |
| 3. qiankun 宿主 | 替换 single-spa、AuthBridge、统一刷新/退出、生命周期与竞态处理 | 用最小演示前端验证挂载/卸载/深链/401/刷新/退出，不接现有子应用 |
| 4. 应用授权 | V5 授权模型、统一授权服务、管理 API、用户展开配置、业务内部授权契约 | 同一用户 A/B 不同角色；跨应用权限/数据范围隔离；无子账号/无应用 token |
| 5. 集群补齐 | Quartz JDBC cluster、Redis WebSocket 广播、共享文件存储、唯一 ID 分配 | 两实例无重复任务、消息可达、文件可读、ID 不冲突、授权撤权一致 |
| 6. 清理与上线 | 删除 JWT/single-spa 死代码、旧权限链、更新 README/规范/部署文档 | 全量构建测试、故障演练、回滚演练、生产只允许 Gateway 入口 |

每阶段都独立构建、测试和出验收记录，不把六个阶段合成一次无法定位问题的大提交。

---

## 10. 核心测试矩阵

### 认证与 Gateway

- 无 token、伪造 token、过期 token、撤销 token → 401。
- access token 过期但 refresh 有效 → 主应用一次刷新，并发请求共享结果。
- refresh 轮换成功、重放检测、多标签页并发、会话绝对/空闲过期。
- logout 后所有该会话 access token introspection 立即 inactive。
- Redis/认证服务超时 → 503，不误判 401。
- 外部伪造 `X-Workspace-User-Id` 无效，下游身份只来自 Gateway。
- Gateway 按 Nacos 实例路由；实例下线不继续分配流量。
- WebSocket 握手使用同一认证与撤销语义。

### qiankun 主应用

- `getToken` 总返回最新 token，refresh 后订阅者只收到一次有效变化。
- 多子应用同时 401 不发生刷新风暴，子应用永远拿不到 refresh token。
- logout/session expired 通知、卸载和清理完整。
- 快速切换时旧 runtime/模块加载结果不能覆盖新应用。
- mount/unmount 多次无重复监听、残留样式或失效回调。

### 应用权限

- 同一 userId 在 workspace/A/B 是不同角色，只有一个全局登录和一种 access token。
- A 的全部数据权限不能扩大 B；更新 A 角色不影响 B。
- grant 停用、应用停用、全局用户停用分别生效，且不混淆 401/403。
- 无有效角色的 grant 不允许进入，且不返回应用菜单。
- 跨应用角色分配和角色权限绑定被拒绝。
- 菜单隐藏之外，直接调用业务 API 仍由业务服务返回 403。
- 授权变更能失效业务服务缓存；内部同步能传播停用/撤权。

### 集群

- 两实例 Quartz 同一触发器只执行一次。
- 任意实例产生的数据变更都能送达其他实例上的 WebSocket 客户端。
- 上传到实例 A 的文件能从实例 B 下载。
- 两实例并发生成 ID 无冲突。
- 单实例下线不影响登录、刷新、introspection 和业务鉴权。

---

## 11. 仍需要你决定的规则

“是否做 qiankun/Nacos/Gateway/opaque introspection”不再是问题；下面只保留会改变数据模型或使用体验的决策。

| # | 决策 | 我的推荐 |
|---|---|---|
| 1 | 认证服务是否第一版就独立进程 | **先作为 workspace-service 内逻辑独立的认证服务；introspection 契约稳定后再物理拆分** |
| 2 | 会话时长 | **access 30 分钟；会话绝对 12 小时；空闲 30 分钟** |
| 3 | 旧 JWT 用户切换 | **发布时全部重新登录，不保留长期双 token 栈** |
| 4 | SUPER_ADMIN 边界 | **只全量放行 workspace；业务应用显式授权** |
| 5 | 已开通应用但无有效角色 | **不允许进入；应用菜单、角色和角色菜单均由 workspace 统一管理** |
| 6 | 用户级数据范围 | **grant 可空覆盖；无覆盖时只合并本应用角色范围** |
| 7 | 自定义部门组织源 | **统一使用 workspace `sys_dept`** |
| 8 | 当前应用被撤权时 | **立即通知并卸载到 403；普通角色权限变化不卸载** |
| 9 | 生产共享文件存储 | **优先 MinIO/S3 兼容存储；若已有可靠共享文件服务则复用** |

如果你接受全部推荐，后续就按这些值细化接口、DDL 和代码改造清单。

---

## 12. 对抗式检查、不确定性与失效条件

### 12.1 这套方案仍不能保证什么

- Gateway introspection 只解决身份有效性，不自动解决业务越权。
- 新建应用权限表和接口不会自动让现有子应用获得鉴权；业务服务必须实际执行检查。
- qiankun 沙箱不是不可信代码安全边界。
- Redis Pub/Sub 是失效通知，不是持久事实源；订阅方重启后必须能重建状态。
- 共享 Redis 不等于天然一致，refresh 轮换、撤销、workerId 分配都需要原子操作和故障测试。
- 服务注册了 Nacos 不等于完成优雅下线、超时、重试和熔断；非幂等请求不能默认自动重试。

### 12.2 当前不确定性

- 尚未核实 Spring Cloud / Alibaba / Nacos 与 Boot 3.5.16 的最终兼容版本。
- 未读取实际 PostgreSQL/Redis/Nacos 环境，不知道已有数据、部署拓扑和运维约束。
- 未分析现有子应用，因此本计划只定义未来接入契约，不声称现有子应用可直接兼容。
- 共享文件存储、内部网络/mTLS、Nacos/Redis 高可用需要结合实际部署环境选型。
- 当前数据权限实际主要用于用户管理，业务资源中的“仅本人”字段必须逐服务定义，不能由平台猜测。

### 12.3 需要重新评估架构的条件

- 必须第一版就拆分独立 IdP，并要求用户主数据也迁出 workspace。
- 需要接入不可信第三方子应用；此时同 realm qiankun 和原始 access token 下发都不足够。
- 业务服务无法统一使用 workspace 用户/组织/应用授权模型。
- 要求跨数据中心多活，而不只是单集群多实例；token、Redis、PostgreSQL 和 Quartz 方案都需另做一致性设计。

### 12.4 当前状态与置信度

- 对现有代码问题及集群 blocker：**高置信度**，有源码和配置依据。
- 对上述目标架构满足硬性需求：**高置信度**。
- 对具体版本、DDL、迁移可执行性：**中置信度**，必须经实际环境和兼容矩阵验证。
- 本次仅修订架构计划，未修改业务源码、未启动服务、未执行构建或测试。

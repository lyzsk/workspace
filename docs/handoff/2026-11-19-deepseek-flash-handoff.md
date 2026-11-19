# Workspace 微服务重构交接记录（deepseek-flash）

> 日期：2026-11-19  
> 交接原因：k3.1-pro 崩溃，由 deepseek-flash 接手继续实施  
> 用途：k3.1-pro 恢复后审查本次接手期间的改动与判断  
> 状态：代码已完成；全仓后端测试、前端测试与构建均通过；未做真实中间件联调

## 1. 接手时的工作区状态

- Git 基线：`aa5195e resture project`（master，与 origin/master 同步）。
- 工作区并非干净状态：接手时已有大量未提交改动，包括：
  - `workspace-gateway` 新增、根 `pom.xml` 与 `workspace-admin/pom.xml` 修改；
  - 不透明 token 认证（V5）、应用授权（V6）相关代码与迁移；
  - 前端 qiankun / AuthBridge / 401 刷新改造；
  - `docs/cv/` 下多份设计与实施说明。
- 后续接手期间继续在这一未提交工作区上叠加修改，未做任何 git commit。

## 2. 接手后实际完成的改动

### 2.1 workspace-common：从错误拆分的子模块改为单模块包结构

**已完成，编译通过。**

此前把 `core/security/redis` 做成了 Maven 子模块（`workspace-common-core` 等），用户指出这是误解：要求的是同一 `workspace-common` 模块下的包结构。已改回：

```text
workspace-common/
└─ src/main/java/cn/sichu/common/
   ├─ core/{base,config,event,exception,file,id,log,result,utils}
   ├─ security/{model,token,util}
   └─ redis/cache
```

- `workspace-common/pom.xml` 恢复单 artifact，聚合 `spring-boot-starter-web/security/data-redis`、MyBatis-Plus、JJWT、Lombok 等。
- 根 `pom.xml` 移除 `workspace-common-core/security/redis` 三个 dependencyManagement 条目，恢复 `workspace-common`。
- `workspace-system/quartz/generator` 依赖改回 `workspace-common`。
- 新增（保留）：
  - `common/security/model/LoginUser.java`（从 system 迁入，`LoginUserSource` 接口解耦实体）
  - `common/security/model/LoginSession.java`
  - `common/security/model/JwtSubject.java`
  - `common/security/model/GatewayHeaders.java`
  - `common/security/token/JwtTokenService.java`（HS256，claims：sub/sid/tv/exp）
  - `common/security/util/SecurityUtils.java`（从 system 迁入）
  - `common/redis/cache/CacheKeys.java`、`LoginSessionStore.java`、`AuthorizationStore.java`、`CacheAccessException.java`
- `SecurityUtils` 与 `LoginUser` 的包迁移通过脚本批量替换全仓 import（`docs/cv/codemod_common_security.py`、`codemod_common_core_package.py`、`codemod_security_utils.py`）。

### 2.2 认证协议：删除 opaque / introspection / refresh token，改为 JWT + Redis

**代码已完成，编译通过；未做 Redis/PostgreSQL 真实联调。**

删除的类（system 内）：

- `OpaqueTokenService`、`OpaqueTokenGenerator`、`OpaqueAuthenticationProperties`、`IssuedTokens`
- `AccessTokenState`、`SessionState`
- `AuthenticationStateStore`、`RedisAuthenticationStateStore`、`AuthenticationStateException`
- `JwtAuthenticationService`
- `GatewayIntrospectionAuthenticationFilter`
- `SysRefreshToken`、`SysRefreshTokenMapper`、`SysTokenBlacklist`、`SysTokenBlacklistMapper`
- `JwtTokenManager`、`JwtProperties`、`TokenPayload`
- `SessionActivityRecorder`
- system 内旧 `AuthController`、`IAuthService`、`AuthServiceImpl`、`OriginValidator`、`RefreshCookieService`、`LoginAuthentication`、`LoginForm/PasswordForm/ProfileForm`、`LoginVO`

新的会话模型：

```text
JWT claims: sub=userId, sid=sessionId, tv=tokenVersion, iss=workspace, exp=30m
Redis key:  login:user:<sessionId>
Value:      LoginSession JSON（完整 LoginUser，密码 getter 标注 @JsonIgnore 不落缓存）
TTL:        30 分钟（空闲滑动；Gateway 每次有效请求 EXPIRE 续期）
绝对上限:   session.expiresAt = 登录时间 + 12h，每次校验
```

- `LoginSessionStore.save` 固定 TTL = 30 分钟；`touch` 续期。
- 改密/全局停用/强制下线通过 `tokenVersion + 1` 加清 Redis 会话实现；应用角色/菜单变更不递增 tokenVersion。
- 删除了 V5 引入的 refresh token 流程；`sys_refresh_token` 表仍留在数据库中（V5 迁移未回滚），当前代码不再读写。

### 2.3 新增 workspace-auth 独立认证服务

**已完成，编译通过；未启动验证。**

新增模块 `workspace-auth`（端口默认 9200，Nacos 服务名 `workspace-auth`）：

```text
cn.sichu.auth/
├─ WorkspaceAuthApplication
├─ config/{PasswordConfig, SecurityConfig}
├─ controller/AuthController
├─ model/{form,vo,dto}
├─ security/JwtAuthenticationFilter
├─ service/{IAuthService, IRemoteUserService, IRemoteLoginLogService,
│           RemoteLoginUserDetailsService, ServiceTokenProvider, ServiceTokenValidator}
├─ service/impl/AuthServiceImpl
└─ session/SessionProperties
```

接口：

- `POST /api/auth/login`：通过 `AuthenticationManager` + `RemoteLoginUserDetailsService`（OpenFeign 调 system）校验密码，成功写 Redis 会话并签 JWT。
- `POST /api/auth/refresh`：从 `Authorization: Bearer` 取 token，**允许已过期 JWT**（`parseIgnoringExpiration`），校验 Redis 会话未撤销、未超绝对/空闲上限后重新签发 JWT。
- `POST /api/auth/logout`：删除 Redis 会话。
- `GET /api/auth/me`、`PUT /api/auth/profile`、`PUT /api/auth/password`、`POST /api/auth/touch`。
- 改密通过 Feign 调 system 完成，然后清空该用户所有 Redis 会话。

资源配置：`application.yml`、`application-prod.yml`、`application-nacos.yml`。

### 2.4 workspace-system 改为独立平台服务

**已完成，编译通过；测试编译未通过（见第 3 节）。**

- 新增 `cn.sichu.system.WorkspaceSystemApplication`（`@EnableScheduling`、`@EnableFeignClients`、扫描 `cn.sichu`、`@MapperScan`）。
- `workspace-system/pom.xml` 增加 OpenFeign、Nacos Discovery/Config、Actuator、PostgreSQL、Flyway，并增加 `spring-boot-maven-plugin`，可直接打包启动。
- 把原 `workspace-admin` 的 `db/migration`、`banner.txt`、`logback-spring.xml` 复制到 `workspace-system/src/main/resources`。
- 新增 `application.yml`（服务名 `workspace-system`，端口 61114）、`application-dev.yml`、`application-prod.yml`、`application-nacos.yml`。
- 新增内部用户接口 `cn.sichu.system.auth.controller.UserLoginInfoController`：

```http
GET  /api/internal/users/login-info/{username}   # 登录凭据（含密码哈希、角色、权限）
GET  /api/internal/users/{userId}/org            # 租户/部门名称
PUT  /api/internal/users/{userId}/profile        # 资料更新
PUT  /api/internal/users/{userId}/password       # 改密
POST /api/internal/users/login-log               # 写登录日志
POST /api/internal/users/login-failure           # 递增失败次数/锁定
POST /api/internal/users/login-success           # 清零失败次数
```

全部要求 `X-Workspace-Service-Token` 头，由 `ServiceTokenValidator` 常数时间比对。

- 新增 `ServiceTokenAuthenticationFilter`（识别服务凭证，授予 `INTERNAL_SERVICE` 权限）。
- `InternalServiceAuthenticationFilter` 覆盖范围改为 `/api/internal/users/` 与 `/api/internal/authorization/`。
- `JwtAuthenticationFilter` 重写为：JWT 验签 → Redis 查会话 → 校验撤销/绝对过期/空闲/状态 → 写入 SecurityContext。
- `TrustedGatewayAuthenticationFilter` 重写为：校验 Gateway 内部凭证 + `X-Workspace-User-Id/Session-Id/Token-Version` 与 Redis 会话一致。
- `JwtHandshakeInterceptor`（WebSocket）重写为 JWT + Redis 会话校验。
- `AuthTokenStore` 简化为 Redis 会话删除（按 sessionId / 按 userId 全删）。
- `OnlineUserServiceImpl` 改为从 Redis `LoginSessionStore.listAll()` 读取在线会话。
- `SecurityConfig` 重写：`/api/internal/users/**` 允许内部服务权限；`/api/internal/authorization/**` 允许应用服务权限；actuator/health、error、ws 放行；其余认证。
- `IUserService/UserServiceImpl` 增加 `updateProfile`、`changePassword`（供 auth Feign 调用），并注入 `AuthQueryMapper`。
- `ILoginLogService/LoginLogServiceImpl` 改为 `record(username, success, message, userId)`、`increaseLoginFailure`、`resetLoginFailure`。
- SQL 注解迁移：`SysUserSessionMapper` 删除 `@Select`，仅保留 `BaseMapper`；`SysRefreshTokenMapper` 已删除；`SysUserSessionMapper.xml`、`SysRefreshTokenMapper.xml` 已删除。

### 2.5 workspace-gateway 改为 JWT 本地验签 + Redis 会话校验

**已完成，编译通过；单元测试新增 1 个，未真实启动。**

- 新增 `cn.sichu.gateway.auth.GatewayTokenVerifier`：
  - jjwt 本地验签（HS256、issuer=workspace、exp）
  - 从 Redis 读 `login:user:<sessionId>`（ReactiveStringRedisTemplate）
  - 校验 `revoked/expiresAt/userId/tokenVersion/status`
  - 有效时 `EXPIRE` 30 分钟续期
- `AuthenticationWebFilter` 重写：删除 introspection WebClient 调用；校验后写入可信身份头并移除下游 Authorization。
- `workspace-gateway/pom.xml` 增加 JJWT。
- 路由改为：
  - `/api/auth/login`（IP 限流）→ `lb://workspace-auth`
  - `/api/auth/**` → `lb://workspace-auth`
  - `/api/**` → `lb://workspace-system`
  - `/ws/**` → `lb:ws://workspace-system`
- 删除 `GatewayWebClientConfig`、`IntrospectionResponse`、旧 `AuthenticationWebFilterTest`；新增 `GatewayTokenVerifierTest`。

### 2.6 删除 workspace-admin

**已完成。**

- 根 `pom.xml` 移除 `workspace-admin`，增加 `workspace-auth`。
- 删除 `workspace-admin` 目录（启动类、旧配置、旧资源已迁入 system 或废弃）。
- 注意：原 README、TODO、部署文档仍按 `workspace-admin` 编写，尚未更新。

### 2.7 新 Java 文件头

接手期间新增文件使用：

```java
/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
```

旧文件保留原有 `@since`，未批量改。

## 3. 当前状态与下一步

### 3.1 已解决

- 删除了 4 个引用已删除类的旧测试（OriginValidatorTest、RefreshCookieServiceTest、TrustedGatewayAuthenticationFilterTest、AuthTokenStoreTest）。
- 全仓 `mvn test` 通过：common 3、quartz 5、tool 2、system 46、generator 3、gateway 6。
- 全仓 `mvn -DskipTests package` 通过；system 产物为 `workspace-system-1.0.jar`（库）+ `workspace-system-1.0-exec.jar`（可执行）。
- 前端 `authTransport.refreshRequest` 已改为携带 `Authorization: Bearer <当前token>`；Vitest 7 个用例与生产构建通过。
- README、TODO 已按三个后端进程（gateway 61115 / system 61114 / auth 9200）更新。
- Generator 模板中的 `cn.sichu.common.*` 导入已改为 `cn.sichu.common.core.*`。
- 模块依赖已反转为 `system → tool`：tool 不再依赖 system；`WebSocketConfig` 移入 system，Document 用户查询通过 `IDocumentUserService` SPI 由 system 实现。
- 应用授权已接入 Redis 缓存 `authz:<userId>:<appCode>`，并在用户授权、角色、菜单变更时失效。
- 已新增 `@DataScope` + `DataScopeAspect` + `DataScopeContext`，用户列表查询已使用。

### 3.2 下一步

1. 真实联调：启动 Nacos、Redis、PostgreSQL，按 system → auth → gateway 顺序启动，验证登录、刷新、空闲退出（30m）、绝对过期（12h）、改密/强制下线（tokenVersion）、应用授权缓存只读、WebSocket 握手。
2. 决定 `V5__opaque_authentication.sql` 新增的 `sys_refresh_token` / session 扩展字段去留：当前代码不再使用，表仍在库中；不要修改已发布迁移，建议后续新增迁移清理或明确保留为历史。
3. 复核 `workspace-system` 是否继续依赖 Quartz/Generator（当前 pom 仍依赖 `workspace-quartz`，并有 `@EnableScheduling`），确认模块边界。
4. 复核 auth 与 system 两份 Feign DTO 字段一致性：`LoginCredentialsDTO` vs `LoginCredentialsVO`、两份 `UserInfoVO`、`ProfileForm/PasswordForm`。
5. 复核网关会话 JSON 解析：依赖 `LoginSession` 序列化字段名 `loginUser.userId/tokenVersion/status/username/tenantId`、`expiresAt`、`revoked`；`LoginUser` 的密码和 UserDetails 方法已 `@JsonIgnore`。
6. `workspace-auth` 的 `SecurityConfig` CORS 目前硬编码 `http://localhost:5173`，应改为配置项。
7. 补 `TrustedGatewayAuthenticationFilter`、`AuthTokenStore` 的 Redis 版本测试（当前只覆盖 common 的 store 与 gateway verifier）。

## 4. 设计决策与用户偏好（逐条）

1. **认证方案：JWT + Redis**，不用 opaque token + introspection。Gateway 本地验签，Redis 查完整登录用户；认证服务独立为 `workspace-auth`。
2. **命名：`sessionId`**，不再使用 `userKey`；JWT claim 用 `sid`。
3. **tokenVersion**：只用于改密、全局停用、强制下线；应用角色/菜单变更通过授权缓存失效处理，**不递增** tokenVersion。
4. **应用授权缓存**：`authz:<userId>:<appCode>`，由 workspace-system 写；app 只读自己的 key，不能交叉读写、不能写；由此**取消 `/internal` 服务调用**（当前 `/api/internal/users/**` 保留给 auth→system 的服务调用，这是用户已确认的结构）。
5. **白名单（white list）需要**：登录、刷新、健康检查等免 token；当前写在 Gateway 过滤器常量，后续可配置化。
6. **DataScope + AOP 要实现**：按 `userId + appCode` 取有效授权，生成 deptIds/selfOnly/all 条件；PostgreSQL 不用 `find_in_set`。
7. **Maven 结构**（用户确认）：
   ```text
   workspace/
   ├─ workspace-gateway
   ├─ workspace-auth
   ├─ workspace-system
   ├─ workspace-common（包：core / security / redis）
   ├─ workspace-tool
   ├─ workspace-quartz
   ├─ workspace-generator
   └─ workspace-ui
   ```
8. **workspace-common 学习 RuoYi-Cloud 组件化**：按包拆分 core/security/redis 等，不新增 Maven 子模块。
9. **service 接口统一命名 `I<ServiceName>Service.java`**，不写 `SysUserService` 这类省略 I 的形式（本项目原有 `IUserService` 保持）。
10. **SQL 一律写 Mapper XML**，不在接口上用 `@Select/@Update`。
11. **新 Java 文件头**：`@author sichu huang` + `@since 2026/11/19`。
12. **默认不要开子 agent**，除非用户明确允许；用户认为小任务拆 agent 效率更低。
13. **底座优先抄 RuoYi-Cloud 与 Art Design Pro**，抄不了再魔改组合；不要让“自创实现”先于“成熟项目复用”。
14. **当前只是实施，不是最终评审**：用户要求完成后由 k3.1-pro 审查。

## 5. 尚未做 / 风险清单

1. **未做真实联调**：Nacos、Redis、PostgreSQL 均未启动验证；`mvn test` 尚未全绿（testCompile 失败）。
2. **前端仍是 opaque 时代的 refresh 逻辑**：`workspace-ui/src/api/authTransport.ts` 的 `refreshRequest()` 不带 Authorization；`logoutRequest` 带 token。需要改为带当前 token 的 refresh。
3. **V5 的 sys_refresh_token 表保留未清理**：代码不再使用，数据库仍有表；后续需决定 V7 清理或保留。
4. **system 模块边界**：`workspace-system` 仍依赖 `workspace-quartz`，并聚合了平台业务、工具、调度等；RuoYi 风格的 system 更聚焦用户/角色/菜单/字典。当前未拆，属于后续重构点。
5. **gateway 解析 Redis 会话 JSON 的耦合**：字段名依赖 `LoginSession/LoginUser` 序列化结果；改动 common 模型需同步 gateway。
6. **auth/system Feign DTO 重复**：两份相似类，字段一致性靠人工；未加契约测试。
7. **auth CORS 硬编码**：`workspace-auth` 的 `SecurityConfig` 固定 `http://localhost:5173`，未接配置。
8. **内部服务凭证**：auth→system 使用 `X-Workspace-Service-Token`（`WORKSPACE_SERVICE_TOKEN`）；Gateway 使用 `X-Workspace-Gateway-Token`（`GATEWAY_INTERNAL_TOKEN`）；两者是不同凭证，部署时都要设置强随机值。
9. **未验证的编译细节**：`workspace-auth` 编译通过，但 `AuthServiceImpl` 的 `LoginUser` 反序列化、Feign 返回 `UserInfoVO` 的 record 映射未做运行验证。
10. **旧文档未更新**：README/TODO/`docs/cv/2026-11-19-workspace-cloud-auth-qiankun-app-auth-implementation.md` 仍描述 opaque/introspection/single-service 启动。
11. **未提交 git**：所有改动仍在工作区，基线 `aa5195e`。
12. **未验证前端构建**：本次接手未再跑 `npm run build`；此前构建通过。

## 6. 关键文件路径清单

### workspace-common

- `workspace-common/pom.xml`
- `workspace-common/src/main/java/cn/sichu/common/core/**`
- `workspace-common/src/main/java/cn/sichu/common/security/model/{LoginUser,LoginSession,LoginUserSource,JwtSubject,GatewayHeaders}.java`
- `workspace-common/src/main/java/cn/sichu/common/security/token/{JwtTokenService,TokenException}.java`
- `workspace-common/src/main/java/cn/sichu/common/security/util/SecurityUtils.java`
- `workspace-common/src/main/java/cn/sichu/common/redis/cache/{CacheKeys,LoginSessionStore,AuthorizationStore,CacheAccessException}.java`
- 测试：`workspace-common/src/test/java/cn/sichu/common/security/token/JwtTokenServiceTest.java`、`workspace-common/src/test/java/cn/sichu/common/redis/cache/LoginSessionStoreTest.java`

### workspace-auth

- `workspace-auth/pom.xml`
- `workspace-auth/src/main/java/cn/sichu/auth/WorkspaceAuthApplication.java`
- `workspace-auth/src/main/java/cn/sichu/auth/config/{PasswordConfig,SecurityConfig}.java`
- `workspace-auth/src/main/java/cn/sichu/auth/controller/AuthController.java`
- `workspace-auth/src/main/java/cn/sichu/auth/security/JwtAuthenticationFilter.java`
- `workspace-auth/src/main/java/cn/sichu/auth/service/{IAuthService,IRemoteUserService,IRemoteLoginLogService,RemoteLoginUserDetailsService,ServiceTokenProvider,ServiceTokenValidator}.java`
- `workspace-auth/src/main/java/cn/sichu/auth/service/impl/AuthServiceImpl.java`
- `workspace-auth/src/main/java/cn/sichu/auth/session/SessionProperties.java`
- `workspace-auth/src/main/resources/{application.yml,application-dev.yml,application-prod.yml,application-nacos.yml}`

### workspace-system

- `workspace-system/pom.xml`
- `workspace-system/src/main/java/cn/sichu/system/WorkspaceSystemApplication.java`
- `workspace-system/src/main/java/cn/sichu/system/auth/controller/UserLoginInfoController.java`
- `workspace-system/src/main/java/cn/sichu/system/auth/service/{ILoginLogService}.java`
- `workspace-system/src/main/java/cn/sichu/system/auth/service/impl/LoginLogServiceImpl.java`
- `workspace-system/src/main/java/cn/sichu/system/auth/model/vo/LoginCredentialsVO.java`
- `workspace-system/src/main/java/cn/sichu/system/security/filter/{JwtAuthenticationFilter,TrustedGatewayAuthenticationFilter}.java`
- `workspace-system/src/main/java/cn/sichu/system/security/websocket/JwtHandshakeInterceptor.java`
- `workspace-system/src/main/java/cn/sichu/system/security/service/{AuthTokenStore,ServiceTokenValidator}.java`
- `workspace-system/src/main/java/cn/sichu/system/security/service/impl/DataScopeServiceImpl.java`
- `workspace-system/src/main/java/cn/sichu/system/sync/{InternalServiceAuthenticationFilter,ServiceTokenAuthenticationFilter}.java`
- `workspace-system/src/main/java/cn/sichu/system/config/SecurityConfig.java`
- `workspace-system/src/main/java/cn/sichu/system/user/service/{IUserService.java,impl/UserServiceImpl.java}`
- `workspace-system/src/main/resources/{application.yml,application-dev.yml,application-prod.yml,application-nacos.yml}`
- `workspace-system/src/main/resources/db/migration/{V1..V6}`

### workspace-gateway

- `workspace-gateway/pom.xml`
- `workspace-gateway/src/main/java/cn/sichu/gateway/auth/{AuthenticationWebFilter,GatewayTokenVerifier,GatewayAuthenticationProperties,GatewayErrorResponseWriter}.java`
- `workspace-gateway/src/main/java/cn/sichu/gateway/filter/{RequestIdWebFilter,RateLimitConfig}.java`
- `workspace-gateway/src/main/resources/application.yml`
- `workspace-gateway/src/test/java/cn/sichu/gateway/auth/GatewayTokenVerifierTest.java`

### 前端（本次接手未修改）

- `workspace-ui/src/api/authTransport.ts`（refresh 需要改为带 Authorization）
- `workspace-ui/src/microfrontend/authBridge.ts`
- `workspace-ui/src/store/modules/user.ts`

### 本次接手新增的脚本/文档

- `docs/cv/codemod_common_security.py`
- `docs/cv/codemod_common_core_package.py`
- `docs/cv/codemod_security_utils.py`
- `docs/handoff/2026-11-19-deepseek-flash-handoff.md`（本文件）

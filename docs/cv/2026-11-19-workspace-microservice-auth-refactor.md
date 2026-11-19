# Workspace 微服务与 JWT 认证改造记录

> 日期：2026-11-19  
> 状态：代码完成、离线测试通过；真实 Nacos/Redis/PostgreSQL 联调未执行。

## 1. 最终模块结构

```text
workspace/
├─ workspace-common
│  ├─ cn.sichu.common.core      通用实体、结果、异常、事件、工具
│  ├─ cn.sichu.common.security  LoginUser、JWT、身份头常量
│  └─ cn.sichu.common.redis     登录会话缓存、应用授权缓存
├─ workspace-auth      登录、刷新、退出、当前用户、资料、改密
├─ workspace-system    用户、角色、菜单、应用授权、通知、文件、Quartz、Generator、Tool 运行时装配
├─ workspace-tool
├─ workspace-quartz
├─ workspace-generator
├─ workspace-gateway
└─ workspace-ui
```

`workspace-admin` 已删除。`workspace-common` 是单一 Maven 模块，`core/security/redis` 是包，不是子模块。

## 2. 认证协议

```text
POST /api/auth/login
→ workspace-auth 通过 OpenFeign 调 workspace-system 获取用户和密码哈希
→ BCrypt 校验
→ 生成 sessionId
→ Redis: login:user:<sessionId> = 完整 LoginUser + 登录时间/最后活跃/绝对过期
→ 签发 HS256 JWT：sub=userId, sid=sessionId, tv=tokenVersion, exp=30m
```

- Redis 保存完整 `LoginUser`，但密码字段不序列化。
- 空闲超时：Redis key TTL 30 分钟，每次有效请求续期。
- 绝对上限：会话记录中的 `expiresAt`，默认 12 小时。
- 刷新：`POST /api/auth/refresh` 使用 Authorization 头中的 JWT（允许刚过期），校验 Redis 会话后重新签发。
- 退出：删除 Redis 会话。
- 改密 / 全局停用 / 强制下线：tokenVersion + 1，并清理该用户全部 Redis 会话。
- 应用角色或菜单变化：只清理 `authz:<userId>:<appCode>`，不改 tokenVersion。

## 3. 服务间调用

| 调用 | 方式 | 保护 |
|---|---|---|
| auth → system 用户信息 | OpenFeign `IRemoteUserService` | `X-Workspace-Service-Token` |
| auth → system 登录日志/失败计数 | OpenFeign `IRemoteLoginLogService` | 同上 |
| Gateway → auth/system | Nacos `lb://` 路由 | Gateway 验签 |
| 子应用 → workspace 授权 | 只读 Redis `authz:<userId>:<appCode>` | 不提供写接口 |

`/api/internal/users/**` 仅允许内部服务凭证访问，Gateway 禁止外部调用 `/api/internal/**`。

## 4. 关键文件

- `workspace-common/src/main/java/cn/sichu/common/security/token/JwtTokenService.java`
- `workspace-common/src/main/java/cn/sichu/common/security/model/LoginUser.java`
- `workspace-common/src/main/java/cn/sichu/common/redis/cache/LoginSessionStore.java`
- `workspace-common/src/main/java/cn/sichu/common/redis/cache/AuthorizationStore.java`
- `workspace-auth/src/main/java/cn/sichu/auth/service/impl/AuthServiceImpl.java`
- `workspace-auth/src/main/java/cn/sichu/auth/security/JwtAuthenticationFilter.java`
- `workspace-auth/src/main/java/cn/sichu/auth/service/IRemoteUserService.java`
- `workspace-system/src/main/java/cn/sichu/system/auth/controller/UserLoginInfoController.java`
- `workspace-system/src/main/java/cn/sichu/system/security/filter/JwtAuthenticationFilter.java`
- `workspace-gateway/src/main/java/cn/sichu/gateway/auth/GatewayTokenVerifier.java`
- `workspace-gateway/src/main/java/cn/sichu/gateway/auth/AuthenticationWebFilter.java`

## 5. 验证结果

- `mvn test`：全仓通过（common 3、quartz 5、tool 2、system 46、generator 3、gateway 6）。
- `mvn -DskipTests package`：全部模块通过；`workspace-system` 产出普通 jar 与 `-exec.jar`。
- 前端 Vitest：7 个用例通过。
- 前端 `npm run build`：通过。
- 未启动 Nacos、Redis、PostgreSQL，未做真实登录联调。

## 5.1 补充实现

- 模块依赖反转为 `system → tool`，tool 仅依赖 common；WebSocket 配置移入 system，文档用户查询通过 `IDocumentUserService` SPI 解耦。
- 应用授权结果写入 Redis `authz:<userId>:<appCode>`，用户授权、角色、菜单变更时失效。
- 新增 `@DataScope`、`DataScopeAspect`、`DataScopeContext`；用户列表查询已接入。
- auth CORS 改为 `workspace.cors.allowed-origins` 配置。

## 6. 遗留

- `sys_refresh_token` 表由 V5 建立，当前 JWT 方案不再使用，暂保留待决定清理。
- auth 与 system 的 Feign DTO 是两份相似类，字段靠 JSON 名称对齐，需后续加契约测试。
- auth 的 CORS 目前硬编码 `http://localhost:5173`，生产需配置化。
- `workspace-system` 仍聚合 Quartz/Generator/Tool，模块边界后续可继续收紧。
- 旧 `sys_user_session` 表与实体保留，当前代码不再写入。

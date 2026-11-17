# 2026-11-17 Workspace Phase 0-4 实施记录

## 范围

已实施 workspace-ui Phase 0-2 与 workspace 后端 Track B（B1-B13）。未启动服务，未执行 Git 提交，未实现制品上传或自动部署。

## 关键口径

- 微前端事件统一为 `workspace:micro-frontend`，`detail.version=1`；props 使用 `getWorkspaceToken()` 动态取 token。
- 子应用路由同时注册 `basePath` 与 `basePath/:pathMatch(.*)*`；宿主只在 `appCode/basePath` 变化时重载。
- 宿主与子应用双向同步主题，子应用退出触发 workspace 退出；应用栏为 64px。
- 门户会话为 12 小时绝对上限、30 分钟空闲；活动心跳 60 秒节流；认证按 JTI 强制校验 `sys_user_session`。
- workspace token 改为 `sessionStorage`，不再写入 `localStorage`；升级后首次加载会清理旧本地副本。
- 所有 Result code 为数字，HTTP status 与 body code 一致；业务异常 warn，系统异常 error。
- 应用访问权限固定为 `{code}:access`；自动维护隐藏 BUTTON `apps:{code}:access`，删除时清理角色引用。
- `/actuator/health` 无鉴权；`X-Request-Id` 接受合法值或生成 UUID，写入 MDC、响应头与文件日志。
- 新密码使用 BCrypt cost 12。已有 cost 10 哈希不能离线升级，需在用户下次改密时升级。

## 内部用户同步

端点：`GET /api/internal/users/sync?updatedAfter=&page=&size=`。

认证：HTTP Basic，username 为应用编码，password 为该应用独立服务凭证。普通用户 Bearer token 不具有 `INTERNAL_USER_SYNC` 权限。仅返回通过 `sys_app.access_permission → sys_menu → sys_role_menu → sys_user_role` 授权的用户，包含状态、更新时间和 BCrypt 哈希，不返回其他用户内部字段。

配置示例（使用环境变量，不写真实密钥）：

```text
WORKSPACE_INTERNAL_USER_SYNC_CLIENTS_0_APP_CODE=eap-tool-system
WORKSPACE_INTERNAL_USER_SYNC_CLIENTS_0_TOKEN=<service-secret>
```

多个应用递增索引。生产环境还应在网关/防火墙限制该端点仅内网服务可达。

`updatedAfter` 使用 ISO 本地日期时间，例如 `2026-11-17T10:00:00`；`page` 从 1 开始，`size` 为 1-500。无 `updatedAfter` 时为当前授权用户全量分页。子应用应通过全量对账清理已取消授权或已删除的本地镜像；增量接口不返回逻辑删除/撤权 tombstone。

## 验证

- `npm --prefix workspace-ui run build`：通过。
- `mvn -f pom.xml -DskipTests compile`：全 reactor 通过。
- `mvn -f pom.xml test`：全 reactor 通过，共 62 个测试，0 failure / 0 error / 0 skipped。
- `V3__internal_user_sync.sql` 仅新增同步索引；未连接数据库执行 Flyway，需由用户手动启动 admin 时验证。

## 未完成的运行验收

深链刷新、主题双向同步、子应用退出、30 分钟空闲、Actuator health、真实 PostgreSQL 同步查询及服务间 SSO 仍需服务启动后验收。

# TODO

## 微服务认证改造（2026-11-19 深夜）

- [x] 模块拆分：workspace-auth 独立；删除 workspace-admin；workspace-common 按 core/security/redis 包分层
- [x] 认证改为 JWT(HS256, 30m) + Redis 完整 LoginUser；空闲 30m TTL、绝对 12h、tokenVersion 撤销
- [x] Gateway 本地验签 + Redis 会话校验；/api/auth/** → workspace-auth，其余 → workspace-system
- [x] auth → system 使用 OpenFeign；内部接口 /api/internal/users/**，服务凭证保护
- [x] 全仓后端测试通过、前端测试与构建通过
- [ ] 启动 Nacos/Redis/PostgreSQL，联调 system → auth → gateway → UI 全链路
- [ ] 真实 Redis 会话过期、空闲退出、改密撤销、强退验证
- [ ] V5 的 sys_refresh_token 表保留但已不再使用，决定是否后续迁移清理
- [ ] 验收 V7__normalize_menu_sort.sql：按 app_code+parent_id 保持现有相对顺序并重排为 1、2、3…；打包已通过，待用户重启 system 执行 Flyway 后核对页面与 flyway_schema_history
- [ ] 统一 auth/system 两份 Feign DTO 的字段校验（当前靠 JSON 字段名一致）
- [ ] 修复权限变更后旧会话鉴权：refresh 查到 fresh LoginUser 后未回写 Redis，getMe 与 system 仍读旧权限；先补“旧会话无权限→授权→刷新→放行”测试再改
- [ ] 联调任意子应用跨应用免密：所有子应用跨应用入口统一调用 authBridge.navigate 进入目标应用的 workspace basePath，不得跳转目标应用独立地址/登录页；宿主已注入 token 与通用 navigate；源码修改交接见 `docs/handoff/2026-11-19-cross-application-navigation.md`
- [x] 本机联调前置：Nacos 重复空 token.secret.key 已删除；Nacos 启动成功，23:07 日志确认 workspace-auth/workspace-gateway/workspace-system 均已新增注册；Redis 可执行文件与 redis.conf 已验证
- [x] 修复 refresh 主链路：新增 GET /api/internal/users/{userId}/login-info，auth loadUserById 改走专用接口，不再把数字 ID 当 username；auth/system 两端回归测试各 1 个通过，待用户重启 system/auth 后做真实 refresh 验收
- [ ] 修复应用授权查询路由：/api/auth/me/apps/** 在 system 实现但被 Gateway /api/auth/** 全量送到 auth；调整归属或精确路由并补测试
- [ ] 恢复 SUPER_ADMIN 只旁路 workspace 的既定边界：当前 computeAuthorization/listAccessibleApps 对全部业务应用绕过用户授权和角色要求
- [ ] 平台用户角色变更同步失效 authz:<userId>:workspace：UserServiceImpl.update/saveRoles 当前未清授权缓存，且登录会话仍是旧权限

## 工作台与访问分析（2026-11-20）

- [ ] 重启 Gateway 与 system 执行 REST 计数改造及 V8__app_access_analytics.sql，验收真实事件采集；工作台=四卡片/周同比/近 7 日 REST 点击量/用户部门分布/热门应用，分析页=应用访问数/近 7 日 PV-UV/TOP 用户；历史无事件时应为 0，不使用演示数据
- [ ] 验收非 admin/SUPER_ADMIN 开发者工具守卫：F12、Ctrl+Shift+I/J/C、自动检测后按 /devtools-blocked→/dashboard（未登录→/login）使用 replace 自动返回，右键仍可用；管理员不受限制
- [ ] 验收全局多行水印 Workspace - realName（缺省 username）/ 动态本地时间 yyyy-MM-dd HH:mm:ss：登录、受限、异常和结果页不显示，其余已登录页面每秒更新时间且不阻挡点击

## 微前端与云原生改造（2026-11-19）

- [x] Nacos Discovery + Spring Cloud Gateway 骨架、RequestId、统一错误、Redis 限流配置
- [x] 不透明 access token、HttpOnly refresh 轮换、Redis 集群状态、Gateway introspection 与可信身份头
- [x] single-spa → qiankun、统一 AuthBridge、401 single-flight 刷新、退出和竞态收口
- [x] 用户—应用授权、角色应用归属、子应用菜单/角色统一管理与有效授权查询
- [x] Flyway V4～V6 已在本地库执行并登记成功
- [ ] 启动 Nacos/Redis/Gateway/新版 workspace-service，完成真实登录、刷新、撤权和 WebSocket 联调
- [ ] 将历史 single-spa ESM 入口迁移为 qiankun HTML entry；未迁移前不算现有子应用已接入
- [ ] 为现有应用配置 workspace 管理的应用角色与菜单；无有效角色时新规则不允许进入
- [ ] 集群上线前完成 Quartz JDBC Cluster、Redis WebSocket 广播、共享文件存储和唯一雪花 workerId
- [ ] 生产部署前启用并验收 Nacos 配置中心：规划 shared/system/auth/gateway Data ID 与环境 namespace/group；非敏感共享配置由控制台或 API 发布，密钥仍走环境变量/密钥管理；验证动态刷新、缺失配置和 Nacos 不可用时的启动行为
- [ ] 生产部署前建设链路追踪：评估并接入 SkyWalking 或同类 APM，打通 Gateway→auth→system 与 Feign 的 TraceId/Span、服务拓扑、耗时和异常分析；保留 RequestId 日志关联并验收跨服务透传
- [ ] 生产部署前评估 Sentinel：在现有限流之外，按真实故障场景决定是否引入熔断、慢调用保护、降级和热点限流；不为凑技术栈直接引入
- [ ] 生产部署前评估 Seata：先列出必须跨服务同时成功/失败的事务场景，比较本地事务+幂等/补偿/可靠事件与 AT/TCC/Saga；Redis 等不被全局事务覆盖的资源单独设计补偿，不默认引入

## 本轮（2026-11-17）

- [x] 删除 ticket 死通道（2026-11-17）：workspace 侧 `EapToolServiceTicket*` 5 类 + `workspace-ui/src/api/system/eap-tool-service.ts` + `application.yml` 的 `workspace.eap-tool-service.*`；EAP 侧 `TicketVerifier` + `/auth/ticket-login` + 前端 `ticketLogin` + `eap-tool-service.ticket.*` 配置；`.claude/CLAUDE.md` 命名示例已更换
- [x] 活通道改名（2026-11-17）：EAP 路由 `/auth/ticket` → `/auth/workspace`，组件 `WorkspaceLogin`，目录 `views/auth/workspace/`，页面文案"正在登录…"；`micro.ts` 同步
- [ ] `eap-tool-service:access` 口径写文档：平台宿主入口权限 vs EAP 本地用户闸；已验证 EAP `loginKnownUser` 拒绝已停用用户（`AuthController.java:115-118`），口径 1 成立，剩落笔
- [ ] 生产 moduleEntry 改造：`build:micro` 已验证通过，构建/托管/SQL/preview 联调步骤见 `docs/deploy-microfrontend.md`；剩 preview 联调验收（需手动起 preview 与 workspace-ui）
- [ ] 浏览器人工验收：角色父子联动开关、用户展开应用授权树表的宽度与配置、部门默认全展开、字典分页序号、/api-doc（需重启 Gateway）、草稿恢复、权限即时刷新、在线时间、子应用连续进入/离开

## 后续

- [ ] Generator 扩展：生成配置持久化、字段映射与查询配置、主子表、目标微前端模板、前端预览细化

## 已完成（2026-11-16）

- [x] 角色：roleSort/remark/menuCheckStrictly/menuIds、权限树、角色保护、使用中删除保护
- [x] 权限：角色/菜单/用户角色变更后刷新用户权限、菜单和动态路由
- [x] 在线用户：最后活跃时间展示；记录失败可降级且不阻断认证
- [x] 事件：DataChangedEvent 与 WebSocket 传输解耦
- [x] 微前端：sys_app 只保留微前端配置；MicroFrontendHost + singleSpa + CustomEvent；运行配置后端鉴权
- [x] 验证：后端 21 个测试报告 48 个测试通过；前端构建通过
- [x] 模块拆分：framework 与 system 合并；document→workspace-tool；job→workspace-quartz；common 提取 BaseImpl/DataChangedEvent/Log
- [x] 内置保护：admin/SUPER_ADMIN/总公司前端只读且后端拦截
- [x] Quartz：RAMJobStore、afterCommit 立即注册、启动全量对账、misfirePolicy、cron 生成与预览
- [x] Generator：PostgreSQL 元数据 + Java/TypeScript/Vue 模板，预览/ZIP，生成 Java 已通过真实编译
- [x] Logback：全量与 WARN/ERROR 独立滚动日志、UTF-8、MyBatis Slf4j、workspace banner
- [x] Flyway 已合并为唯一 V1；admin/test 独立本地 seed；migrator 与 workspace DML 配置分离；sql/ 仅保留说明和本地账号 seed
- [x] 开发库按 V1+V2 重建并 validate 通过（旧库备份在 `20261116/flyway-backup/`）；`sys_app` 的 base_path/access_permission 与 `sys_menu` 206 统一为 `eap-tool-service` 全名

# TODO

## 本轮（2026-11-17）

- [x] 删除 ticket 死通道（2026-11-17）：workspace 侧 `EapToolServiceTicket*` 5 类 + `workspace-ui/src/api/system/eap-tool-service.ts` + `application.yml` 的 `workspace.eap-tool-service.*`；EAP 侧 `TicketVerifier` + `/auth/ticket-login` + 前端 `ticketLogin` + `eap-tool-service.ticket.*` 配置；`.claude/CLAUDE.md` 命名示例已更换
- [x] 活通道改名（2026-11-17）：EAP 路由 `/auth/ticket` → `/auth/workspace`，组件 `WorkspaceLogin`，目录 `views/auth/workspace/`，页面文案"正在登录…"；`micro.ts` 同步
- [ ] `eap-tool-service:access` 口径写文档：平台宿主入口权限 vs EAP 本地用户闸；已验证 EAP `loginKnownUser` 拒绝已停用用户（`AuthController.java:115-118`），口径 1 成立，剩落笔
- [ ] 生产 moduleEntry 改造：`build:micro` 已验证通过，构建/托管/SQL/preview 联调步骤见 `docs/deploy-microfrontend.md`；剩 preview 联调验收（需手动起 preview 与 workspace-ui）
- [ ] 浏览器人工验收：角色树、草稿恢复、权限即时刷新、在线时间、子应用连续进入/离开

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

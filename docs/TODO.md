# TODO

## 本轮（2026-11-16）

- [x] 角色：roleSort/remark/menuCheckStrictly/menuIds、权限树、角色保护、使用中删除保护
- [x] 权限：角色/菜单/用户角色变更后刷新用户权限、菜单和动态路由
- [x] 在线用户：最后活跃时间展示；记录失败可降级且不阻断认证
- [x] 事件：DataChangedEvent 与 WebSocket 传输解耦
- [x] 微前端：sys_app 只保留微前端配置；MicroFrontendHost + singleSpa + CustomEvent；运行配置后端鉴权
- [x] 验证：后端 14 个测试类 33 个测试通过；前端构建通过；本地 sys_role/sys_app 结构更新
- [x] 模块拆分：framework 与 system 合并；document→workspace-tool；job→workspace-quartz；common 提取 BaseImpl/DataChangedEvent/Log
- [x] 内置保护：admin/SUPER_ADMIN/总公司前端只读且后端拦截
- [x] Quartz：RAMJobStore、afterCommit 立即注册、启动全量对账、misfirePolicy、cron 生成与预览
- [x] Generator：PostgreSQL 元数据 + Java/TypeScript/Vue 模板，预览/ZIP，生成 Java 已通过真实编译
- [ ] Generator 扩展：生成配置持久化、字段映射与查询配置、主子表、目标微前端模板、前端预览细化
- [x] Logback：全量与 WARN/ERROR 独立滚动日志、UTF-8、MyBatis Slf4j、workspace banner
- [ ] 浏览器人工验收：角色树、草稿恢复、权限即时刷新、在线时间、子应用连续进入/离开
- [x] Flyway 已合并为唯一 V1；admin/test 独立本地 seed；migrator 与 workspace DML 配置分离；sql/ 仅保留说明和本地账号 seed

详见 `docs/cv/48-platform-rbac-microfrontend-session.md`。

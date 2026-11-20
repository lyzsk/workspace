INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (9100000000000000101, 'eap-tool-service', 'dashboard', 0, '仪表盘', 'DIR', '/apps/eap-tool-service/dashboard', NULL, NULL, 'ri:pie-chart-line', 1, 1, 1, now()),
    (9100000000000000102, 'eap-tool-service', 'dashboard:console', 9100000000000000101, '工作台', 'MENU', '/apps/eap-tool-service/dashboard/console', NULL, NULL, 'ri:home-smile-2-line', 1, 1, 1, now()),
    (9100000000000000103, 'eap-tool-service', 'dashboard:analysis', 9100000000000000101, '分析页', 'MENU', '/apps/eap-tool-service/dashboard/analysis', NULL, NULL, 'ri:align-item-bottom-line', 2, 1, 1, now()),
    (9100000000000000110, 'eap-tool-service', 'tool', 0, '机台管理', 'DIR', '/apps/eap-tool-service/tool', NULL, NULL, 'ri:server-line', 2, 1, 1, now()),
    (9100000000000000111, 'eap-tool-service', 'tool:ledger', 9100000000000000110, '机台台账', 'MENU', '/apps/eap-tool-service/tool/ledger', NULL, NULL, 'ri:list-check-2', 1, 1, 1, now()),
    (9100000000000000112, 'eap-tool-service', 'tool:field-config', 9100000000000000110, '台账字段配置', 'MENU', '/apps/eap-tool-service/tool/field-config', NULL, NULL, 'ri:settings-4-line', 2, 1, 1, now()),
    (9100000000000000113, 'eap-tool-service', 'tool:coding-rule', 9100000000000000110, '机台编号规则配置', 'MENU', '/apps/eap-tool-service/tool/coding-rule', NULL, NULL, 'ri:price-tag-3-line', 3, 1, 1, now()),
    (9100000000000000120, 'eap-tool-service', 'utility', 0, '常用工具', 'DIR', '/apps/eap-tool-service/utility', NULL, NULL, 'ri:tools-line', 3, 1, 1, now()),
    (9100000000000000121, 'eap-tool-service', 'utility:gantt', 9100000000000000120, '甘特图', 'MENU', '/apps/eap-tool-service/utility/gantt', NULL, NULL, 'ri:task-line', 1, 1, 1, now()),
    (9100000000000000130, 'eap-tool-service', 'log', 0, '日志管理', 'DIR', '/apps/eap-tool-service/log', NULL, NULL, 'ri:file-shield-2-line', 4, 1, 1, now()),
    (9100000000000000131, 'eap-tool-service', 'log:login', 9100000000000000130, '登录日志', 'MENU', '/apps/eap-tool-service/log/login', NULL, NULL, 'ri:login-circle-line', 1, 1, 1, now()),
    (9100000000000000132, 'eap-tool-service', 'log:oper', 9100000000000000130, '操作日志', 'MENU', '/apps/eap-tool-service/log/oper', NULL, NULL, 'ri:file-list-3-line', 2, 1, 1, now()),
    (9100000000000000140, 'eap-tool-service', 'system', 0, '系统管理', 'DIR', '/apps/eap-tool-service/system', NULL, NULL, 'ri:shield-user-line', 5, 1, 1, now()),
    (9100000000000000141, 'eap-tool-service', 'system:authz-user', 9100000000000000140, '用户管理', 'MENU', '/apps/eap-tool-service/system/authz-user', NULL, NULL, 'ri:team-line', 1, 1, 1, now()),
    (9100000000000000142, 'eap-tool-service', 'system:authz-role', 9100000000000000140, '角色与资源', 'MENU', '/apps/eap-tool-service/system/authz-role', NULL, NULL, 'ri:shield-user-line', 2, 1, 1, now()),
    (9100000000000000143, 'eap-tool-service', 'system:authz-field', 9100000000000000140, '字段权限', 'MENU', '/apps/eap-tool-service/system/authz-field', NULL, NULL, 'ri:input-field', 3, 1, 1, now()),
    (9100000000000000144, 'eap-tool-service', 'system:authz-grant', 9100000000000000140, '时效授权', 'MENU', '/apps/eap-tool-service/system/authz-grant', NULL, NULL, 'ri:time-line', 4, 1, 1, now()),
    (9100000000000000145, 'eap-tool-service', 'system:authz-scope', 9100000000000000140, '数据权限', 'MENU', '/apps/eap-tool-service/system/authz-scope', NULL, NULL, 'ri:shield-keyhole-line', 5, 1, 1, now()),
    (9100000000000000201, 'ws-test', 'tm', 0, '测试管理', 'DIR', '/apps/ws-test/tm', NULL, NULL, 'ri:flask-line', 1, 1, 1, now()),
    (9100000000000000202, 'ws-test', 'tm:dashboard', 9100000000000000201, '测试看板', 'MENU', '/apps/ws-test/tm/dashboard', NULL, NULL, 'ri:dashboard-line', 1, 1, 1, now()),
    (9100000000000000203, 'ws-test', 'tm:requirement', 9100000000000000201, '需求管理', 'MENU', '/apps/ws-test/tm/requirement', NULL, NULL, 'ri:file-list-3-line', 2, 1, 1, now()),
    (9100000000000000204, 'ws-test', 'tm:plan', 9100000000000000201, '测试计划', 'MENU', '/apps/ws-test/tm/plan', NULL, NULL, 'ri:calendar-todo-line', 3, 1, 1, now()),
    (9100000000000000205, 'ws-test', 'tm:case', 9100000000000000201, '用例库', 'MENU', '/apps/ws-test/tm/case', NULL, NULL, 'ri:file-check-line', 4, 1, 1, now()),
    (9100000000000000206, 'ws-test', 'tm:task', 9100000000000000201, '测试任务', 'MENU', '/apps/ws-test/tm/task', NULL, NULL, 'ri:play-list-2-line', 5, 1, 1, now()),
    (9100000000000000207, 'ws-test', 'tm:defect', 9100000000000000201, '缺陷管理', 'MENU', '/apps/ws-test/tm/defect', NULL, NULL, 'ri:bug-line', 6, 1, 1, now()),
    (9100000000000000208, 'ws-test', 'tm:report', 9100000000000000201, '测试报告', 'MENU', '/apps/ws-test/tm/report', NULL, NULL, 'ri:file-chart-line', 7, 1, 1, now()),
    (9100000000000000209, 'ws-test', 'tm:script-studio', 9100000000000000201, '脚本管理', 'MENU', '/apps/ws-test/tm/script-studio', NULL, NULL, 'ri:code-box-line', 8, 1, 1, now()),
    (9100000000000000210, 'ws-test', 'tm:env', 9100000000000000201, '测试环境', 'MENU', '/apps/ws-test/tm/env', NULL, NULL, 'ri:server-line', 9, 1, 1, now()),
    (9100000000000000211, 'ws-test', 'tm:exec-env', 9100000000000000201, '执行环境配置', 'MENU', '/apps/ws-test/tm/exec-env', NULL, NULL, 'ri:settings-3-line', 10, 1, 1, now())
ON CONFLICT DO NOTHING;

DELETE FROM system.sys_role_menu rm
USING system.sys_role r, system.sys_app a, system.sys_menu m
WHERE rm.role_id = r.id
  AND r.app_id = a.id
  AND rm.menu_id = m.id
  AND a.code IN ('eap-tool-service', 'ws-test')
  AND m.app_code <> a.code;

INSERT INTO system.sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT m.id, 0, r.id, m.id, now()
FROM system.sys_menu m
JOIN system.sys_app a ON a.code = m.app_code AND a.is_deleted = 0
JOIN system.sys_role r ON r.app_id = a.id AND r.is_deleted = 0 AND r.status = 1
WHERE m.id BETWEEN 9100000000000000101 AND 9100000000000000211
  AND ((a.code = 'eap-tool-service' AND r.role_code = 'IES_ADMIN')
    OR (a.code = 'ws-test' AND r.role_code = 'TM_ADMIN'))
ON CONFLICT DO NOTHING;

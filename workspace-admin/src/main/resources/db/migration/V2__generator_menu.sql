INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (219, 'workspace', 'tools:generator', 210, '代码生成', 'MENU', '/tools/generator',
     'tools/generator/index', 'system:generator:list', 'ri:code-line', 5, 1, 1, now()),
    (220, 'workspace', 'tools:generator:preview', 219, '预览代码', 'BUTTON', NULL, NULL,
     'system:generator:preview', NULL, 1, 1, 1, now()),
    (221, 'workspace', 'tools:generator:download', 219, '下载代码', 'BUTTON', NULL, NULL,
     'system:generator:download', NULL, 2, 1, 1, now())
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT 10000 + id, 0, 1, id, now()
FROM system.sys_menu
WHERE id IN (219, 220, 221)
ON CONFLICT DO NOTHING;

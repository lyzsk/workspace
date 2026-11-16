INSERT INTO system.sys_role
    (id, tenant_id, role_code, role_name, data_scope, role_sort, menu_check_strictly, status, create_time)
VALUES
    (1, 0, 'SUPER_ADMIN', '超级管理员', 1, 0, true, 1, now()),
    (2, 0, 'GENERAL', '普通用户', 5, 1, true, 1, now())
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_user
    (id, tenant_id, username, password, real_name, gender, status, create_time)
VALUES
    (1, 0, 'admin', '$2a$10$q9ZzAITMEKcPWcFC/Y.8TOa7SPk2sa59b0xUjwySB5kGKJfDcUNwK', '管理员', 1, 1, now()),
    (2, 0, 'test', '$2b$10$cbtUAwZJPnkZihbF/XnpNuKSr/2KSU.ChIS6CR6XcQWEIvqTFQp4G', '测试用户', 0, 1, now())
ON CONFLICT DO NOTHING;

-- 只初始化本地账号，不删除或停用其他账号

INSERT INTO system.sys_user_role (id, tenant_id, user_id, role_id, create_time)
SELECT 1, 0, u.id, r.id, now()
FROM system.sys_user u, system.sys_role r
WHERE u.username = 'admin' AND u.is_deleted = 0
  AND r.role_code = 'SUPER_ADMIN' AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM system.sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.is_deleted = 0
  )
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_user_role (id, tenant_id, user_id, role_id, create_time)
SELECT 2, 0, u.id, r.id, now()
FROM system.sys_user u, system.sys_role r
WHERE u.username = 'test' AND u.is_deleted = 0
  AND r.role_code = 'GENERAL' AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM system.sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.is_deleted = 0
  )
ON CONFLICT DO NOTHING;

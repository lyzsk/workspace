UPDATE system.sys_role
SET role_name = '系统管理员', update_time = now()
WHERE role_code = 'ADMIN' AND is_deleted = 0;

INSERT INTO system.sys_user
    (id, tenant_id, username, password, real_name, gender, status, token_version,
     login_fail_count, create_time, update_time, is_deleted)
SELECT 3900000000000000002, 0, 'superadmin',
       '$2a$12$pyEiFpwaF0n9SdMrwUyoMO.6F8cDvHU/Ud9GGU3BSoabJ7X7w/As.',
       '超级管理员', 0, 1, 0, 0, now(), now(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system.sys_user WHERE username = 'superadmin' AND is_deleted = 0
);

INSERT INTO system.sys_user_role (id, tenant_id, user_id, role_id, create_time)
SELECT 3900000000000000101, 0, u.id, r.id, now()
FROM system.sys_user u
JOIN system.sys_role r ON r.role_code = 'SUPER_ADMIN' AND r.is_deleted = 0
JOIN system.sys_app a ON a.id = r.app_id AND a.code = 'workspace' AND a.is_deleted = 0
WHERE u.username = 'superadmin' AND u.is_deleted = 0
ON CONFLICT DO NOTHING;

DELETE FROM system.sys_user_role ur
USING system.sys_user u, system.sys_role r
WHERE ur.user_id = u.id
  AND ur.role_id = r.id
  AND u.username = 'admin'
  AND u.is_deleted = 0
  AND r.role_code = 'SUPER_ADMIN';

INSERT INTO system.sys_user_role (id, tenant_id, user_id, role_id, create_time)
SELECT 3900000000000000102, 0, u.id, r.id, now()
FROM system.sys_user u
JOIN system.sys_role r ON r.role_code = 'ADMIN' AND r.is_deleted = 0
JOIN system.sys_app a ON a.id = r.app_id AND a.code = 'workspace' AND a.is_deleted = 0
WHERE u.username = 'admin' AND u.is_deleted = 0
ON CONFLICT DO NOTHING;

UPDATE system.sys_user
SET token_version = token_version + 1, update_time = now()
WHERE username = 'admin' AND is_deleted = 0;

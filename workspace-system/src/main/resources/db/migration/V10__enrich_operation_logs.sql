ALTER TABLE system.sys_oper_log ADD COLUMN IF NOT EXISTS dept_name varchar(100);
ALTER TABLE system.sys_oper_log ADD COLUMN IF NOT EXISTS method varchar(500);
ALTER TABLE system.sys_oper_log ADD COLUMN IF NOT EXISTS request_query text;
ALTER TABLE system.sys_oper_log ADD COLUMN IF NOT EXISTS user_agent varchar(500);
ALTER TABLE system.sys_oper_log ADD COLUMN IF NOT EXISTS error_type varchar(500);

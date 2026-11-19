WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY app_code, parent_id
               ORDER BY sort_no, id
           )::int4 AS normalized_sort_no
    FROM system.sys_menu
    WHERE is_deleted = 0
)
UPDATE system.sys_menu AS menu
SET sort_no = ranked.normalized_sort_no
FROM ranked
WHERE menu.id = ranked.id
  AND menu.sort_no <> ranked.normalized_sort_no;

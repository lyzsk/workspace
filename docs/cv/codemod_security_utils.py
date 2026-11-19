from pathlib import Path

root = Path(r'D:\dev\workspace')
old = 'cn.sichu.system.security.util.SecurityUtils'
new = 'cn.sichu.common.security.util.SecurityUtils'
for path in root.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    updated = text.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        print(path)

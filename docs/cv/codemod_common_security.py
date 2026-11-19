from pathlib import Path

root = Path(r'D:\dev\workspace')
replacements = {
    'cn.sichu.system.security.model.LoginUser': 'cn.sichu.common.security.model.LoginUser',
    'cn.sichu.system.security.token.TokenException': 'cn.sichu.common.security.token.TokenException',
}
for path in root.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        print(path)

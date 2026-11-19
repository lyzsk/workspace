from pathlib import Path
import re

root = Path(r'D:\dev\workspace')
packages = 'base|config|event|exception|file|id|log|result|utils'
pattern = re.compile(r'cn\.sichu\.common\.(' + packages + r')\b')
for path in root.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    updated = pattern.sub(r'cn.sichu.common.core.\1', text)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        print(path)

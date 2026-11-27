"""보드를 claude.ai 아티팩트로 올릴 파일로 바꿔요 (Leader 전용).

    python tools/pm/publish_board.py

docs/pm/board.html 을 읽어 doctype, html, head, body 태그를 빼고,
'링크 복사' 주소를 아티팩트 주소로 고정해 docs/exports/board-artifact.html 로 써요.
그 뒤 Leader가 Artifact 도구로 같은 파일을 다시 올려요(주소는 그대로).
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "docs/pm/board.html"
OUT = ROOT / "docs/exports/board-artifact.html"
ARTIFACT_URL = "https://claude.ai/artifact/E1UdTv9hCMTq4YTKQGbuF2"

html = SRC.read_text(encoding="utf-8")
html = re.sub(r"(?is)<!doctype[^>]*>", "", html)
html = re.sub(r"(?is)</?(html|head|body)(\s[^>]*)?>", "", html)
html = re.sub(r'(?i)<meta\s+charset=[^>]*>\s*', "", html)
html = re.sub(r'(?i)<meta\s+name="viewport"[^>]*>\s*', "", html)
old = 'let base = location.href.split("#")[0];'
if old not in html:
    raise SystemExit("링크 주소 줄을 못 찾았어요: board_template.html 의 linkOf 를 확인해요")
html = html.replace(old, f'let base = "{ARTIFACT_URL}";')
memo = (ROOT / "tools/pm/memo_block.html").read_text(encoding="utf-8")
anchor = '<section class="stats" id="stats"'
if anchor not in html:
    raise SystemExit("메모 칸 넣을 자리를 못 찾았어요")
html = html.replace(anchor, memo + anchor, 1)
OUT.write_text(html.strip() + "\n", encoding="utf-8")
print("wrote", OUT, len(html) // 1024, "KB")

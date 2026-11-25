# TSK-206: 스플래시 손글씨 빠르기 미리보기 GIF. 앱 코드(SplashHandwriting.kt)와 같은 획, 시간표, 빠르기 곡선으로 그려요.
# 폰 화면 녹화가 아니라 같은 계산으로 그린 그림이에요. 쓰는 법: python make_preview.py <저장소 폴더>
import math, os, re, sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1]
XML = os.path.join(ROOT, 'app/src/main/res/drawable/ic_launcher_app_foreground.xml')
KT = os.path.join(ROOT, 'feature/settings/src/main/kotlin/com/lumian/morningup/feature/settings/SplashHandwriting.kt')
OUT = os.path.join(ROOT, 'docs/exports/tsk-206')

paths = re.findall(r'android:pathData="([^"]+)"', open(XML, encoding='utf-8').read())
kt = open(KT, encoding='utf-8').read()
num = r'(-?\d+(?:\.\d+)?)f?'
DRAW = int(re.search(r'DRAW_MILLIS = ([\d_]+)', kt).group(1).replace('_', ''))
HOLD = int(re.search(r'HOLD_MILLIS = ([\d_]+)', kt).group(1).replace('_', ''))
PACE = [float(x) for x in re.findall(num, re.search(r'PACE = listOf\(([^)]*)\)', kt).group(1))]
PAUSES = [float(x) for x in re.findall(r'\d+', re.search(r'PAUSES = listOf\(([^)]*)\)', kt).group(1))]
EASE = [tuple(float(v) for v in re.findall(num, m)) for m in re.findall(r'CubicBezierEasing\(([^)]*)\)', kt)]


def parse(d):
    toks = re.findall(r'[MC]|-?\d*\.?\d+', d)
    i, cur, cmd, pts = 0, None, None, []
    while i < len(toks):
        t = toks[i]
        if t in 'MC':
            cmd = t; i += 1; continue
        if cmd == 'M':
            cur = (float(toks[i]), float(toks[i + 1])); i += 2; pts.append(cur)
        else:
            c1 = (float(toks[i]), float(toks[i + 1])); c2 = (float(toks[i + 2]), float(toks[i + 3]))
            p = (float(toks[i + 4]), float(toks[i + 5])); i += 6
            for k in range(1, 61):
                s = k / 60; u = 1 - s
                pts.append((u**3 * cur[0] + 3 * u * u * s * c1[0] + 3 * u * s * s * c2[0] + s**3 * p[0],
                            u**3 * cur[1] + 3 * u * u * s * c1[1] + 3 * u * s * s * c2[1] + s**3 * p[1]))
            cur = p
    return pts


polys = [parse(d) for d in paths]
cum = []
for pl in polys:
    c = [0.0]
    for a, b in zip(pl, pl[1:]):
        c.append(c[-1] + math.dist(a, b))
    cum.append(c)
lengths = [c[-1] for c in cum]

weights = [l * PACE[i % len(PACE)] for i, l in enumerate(lengths)]
pauses = [PAUSES[i % len(PAUSES)] for i in range(len(lengths) - 1)]
drawing = DRAW - sum(pauses)
slots, t = [], 0.0
for i, w in enumerate(weights):
    end = t + drawing * w / sum(weights)
    slots.append((t, end))
    t = end + (pauses[i] if i < len(pauses) else 0)


def bez(x1, y1, x2, y2, x):
    lo, hi = 0.0, 1.0
    for _ in range(40):
        m = (lo + hi) / 2
        bx = 3 * (1 - m) ** 2 * m * x1 + 3 * (1 - m) * m * m * x2 + m ** 3
        lo, hi = (m, hi) if bx < x else (lo, m)
    m = (lo + hi) / 2
    return 3 * (1 - m) ** 2 * m * y1 + 3 * (1 - m) * m * m * y2 + m ** 3


def progress(i, ms):
    s, e = slots[i]
    raw = min(1.0, max(0.0, (ms - s) / (e - s)))
    return bez(*EASE[i % len(EASE)], raw) if 0 < raw < 1 else raw


W, H, LOGO, SS = 360, 640, 320, 3
BG = (0x17, 0x1A, 0x1F)


def frame(ms):
    img = Image.new('RGB', (W * SS, H * SS), BG)
    d = ImageDraw.Draw(img)
    k = LOGO / 540 * SS
    ox, oy = (W - LOGO) / 2 * SS + 90 * k, (H - LOGO) / 2 * SS + 90 * k
    w = 5.6 * k; r = w / 2
    for i, pl in enumerate(polys):
        p = progress(i, ms)
        if p <= 0:
            continue
        stop = lengths[i] * p
        q = [pl[0]]
        for j in range(1, len(pl)):
            if cum[i][j] <= stop:
                q.append(pl[j])
            else:
                a, b = pl[j - 1], pl[j]
                f = (stop - cum[i][j - 1]) / max(1e-6, cum[i][j] - cum[i][j - 1])
                q.append((a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f))
                break
        q = [(ox + x * k, oy + y * k) for x, y in q]
        if len(q) > 1:
            d.line(q, fill='white', width=max(1, round(w)))
        for x, y in q:
            d.ellipse([x - r, y - r, x + r, y + r], fill='white')
    return img.resize((W, H), Image.LANCZOS)


FPS = 25
step = 1000 / FPS
frames = [frame(n * step) for n in range(int((DRAW + HOLD) / step) + 1)]
frames[0].save(os.path.join(OUT, 'splash-handwriting.gif'), save_all=True, append_images=frames[1:],
               duration=int(step), loop=0, optimize=True)
frame(DRAW).save(os.path.join(OUT, 'splash-handwriting-done.png'))
print('획 시간표(ms):', [(round(s), round(e)) for s, e in slots])
print('프레임', len(frames))

# TSK-200: 피그마 '앱로고_일어나ver2'(1124:21227) SVG로 런처 아이콘 리소스 만들기
import math, os, re, sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1]
SVG = os.path.join(ROOT, 'docs/exports/tsk-200/source/app-logo-figma.svg')
RES = os.path.join(ROOT, 'app/src/main/res')
OUT = os.path.join(ROOT, 'docs/exports/tsk-200')
BG = (0x17, 0x1A, 0x1F)
STROKE_W = 5.6

svg = open(SVG, encoding='utf-8').read()
paths = re.findall(r'<path d="([^"]+)"', svg)


def parse(d):
    toks = re.findall(r'[MC]|-?\d*\.?\d+', d)
    segs, i, cur, cmd = [], 0, None, None
    pts = []
    while i < len(toks):
        t = toks[i]
        if t in 'MC':
            cmd = t; i += 1; continue
        if cmd == 'M':
            cur = (float(toks[i]), float(toks[i + 1])); i += 2
            pts.append(cur)
        else:
            c1 = (float(toks[i]), float(toks[i + 1]))
            c2 = (float(toks[i + 2]), float(toks[i + 3]))
            p = (float(toks[i + 4]), float(toks[i + 5])); i += 6
            for k in range(1, 41):
                s = k / 40; u = 1 - s
                pts.append((u**3 * cur[0] + 3 * u * u * s * c1[0] + 3 * u * s * s * c2[0] + s**3 * p[0],
                            u**3 * cur[1] + 3 * u * u * s * c1[1] + 3 * u * s * s * c2[1] + s**3 * p[1]))
            cur = p
    return pts


polys = [parse(d) for d in paths]

# 안전 영역 확인(프레임 단위, 가운데 180,180). 66dp 원 = 반지름 165, 보이는 72dp = 180
far = max(math.hypot(x - 180, y - 180) for pl in polys for (x, y) in pl) + STROKE_W / 2
print('가운데서 가장 먼 점(선 두께 포함):', round(far, 1), '/ 안전 원 165')
assert far < 165


def draw_strokes(img, scale, ox, oy, color):
    d = ImageDraw.Draw(img)
    w = STROKE_W * scale
    r = w / 2
    for pl in polys:
        q = [(ox + x * scale, oy + y * scale) for (x, y) in pl]
        d.line(q, fill=color, width=max(1, round(w)))
        for (x, y) in q:
            d.ellipse([x - r, y - r, x + r, y + r], fill=color)


SS = 8


def render(size_px, frame_px, bg=True, alpha_bg=None):
    """size_px 캔버스 가운데에 360 프레임을 frame_px 크기로 그림"""
    S = size_px * SS
    img = Image.new('RGBA', (S, S), (BG + (255,)) if bg else (0, 0, 0, 0))
    scale = frame_px * SS / 360
    off = (S - 360 * scale) / 2
    draw_strokes(img, scale, off, off, (255, 255, 255, 255))
    return img.resize((size_px, size_px), Image.LANCZOS)


def circle_mask(img):
    S = img.size[0] * SS
    m = Image.new('L', (S, S), 0)
    ImageDraw.Draw(m).ellipse([0, 0, S - 1, S - 1], fill=255)
    m = m.resize(img.size, Image.LANCZOS)
    out = img.copy(); out.putalpha(m)
    return out


dens = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
for name, f in dens.items():
    px = int(48 * f)
    sq = render(px, px)  # 옛 기기용: 프레임 전체를 48dp에
    sq.convert('RGB').save(os.path.join(RES, f'mipmap-{name}', 'ic_launcher_app.png'), optimize=True)
    circle_mask(sq).save(os.path.join(RES, f'mipmap-{name}', 'ic_launcher_app_round.png'), optimize=True)

store = render(512, 512)
store.convert('RGB').save(os.path.join(ROOT, 'app/src/main/ic_launcher_app-playstore.png'), optimize=True)
store.convert('RGB').save(os.path.join(OUT, 'playstore-icon-512.png'), optimize=True)

# 앞 그림 벡터(108dp, viewport 540, 프레임 360을 가운데 72dp에: translate 90)
vec = ['<?xml version="1.0" encoding="utf-8"?>',
       '<!-- 앱 아이콘 앞 그림(11-25 피그마 \'앱로고_일어나ver2\' 1124:21227, TSK-200). 손글씨 선 그대로(선 두께 5.6, 둥근 끝). -->',
       '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
       '    android:width="108dp" android:height="108dp"',
       '    android:viewportWidth="540" android:viewportHeight="540">',
       '    <group android:translateX="90" android:translateY="90">']
for d in paths:
    vec.append(f'        <path android:pathData="{d}" android:strokeColor="#FFFFFFFF" android:strokeWidth="5.6" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
vec += ['    </group>', '</vector>', '']
open(os.path.join(RES, 'drawable', 'ic_launcher_app_foreground.xml'), 'w', encoding='utf-8', newline='\n').write('\n'.join(vec))

# 마스크 미리보기: 108dp 적응형 레이어를 원, 둥근 사각(삼성), 사각으로 자르고 안전 원 표시
P = 432  # 108dp @4x
layer = render(P, P * 72 / 108)
sheet = Image.new('RGBA', (P * 4 + 50, P + 20), (255, 255, 255, 255))
guide = layer.copy(); g = ImageDraw.Draw(guide)
c = P / 2
for rad, col in ((P * 33 / 108, (255, 60, 60, 255)), (P * 36 / 108, (255, 210, 0, 255))):
    g.ellipse([c - rad, c - rad, c + rad, c + rad], outline=col, width=2)
sheet.paste(guide, (10, 10))
vis = layer.crop((int(P * 18 / 108), int(P * 18 / 108), int(P * 90 / 108), int(P * 90 / 108))).resize((P, P), Image.LANCZOS)
for idx, shape in enumerate(('circle', 'squircle', 'square')):
    m = Image.new('L', (P * SS, P * SS), 0); md = ImageDraw.Draw(m)
    if shape == 'circle':
        md.ellipse([0, 0, P * SS - 1, P * SS - 1], fill=255)
    elif shape == 'squircle':
        md.rounded_rectangle([0, 0, P * SS - 1, P * SS - 1], radius=int(P * SS * 0.30), fill=255)
    else:
        md.rectangle([0, 0, P * SS - 1, P * SS - 1], fill=255)
    m = m.resize((P, P), Image.LANCZOS)
    tile = vis.copy(); tile.putalpha(m)
    sheet.alpha_composite(tile, (10 + (idx + 1) * (P + 10), 10))
sheet.convert('RGB').save(os.path.join(OUT, 'preview-masks.png'))
# 앱 서랍처럼 작게 본 모습
small = Image.new('RGB', (3 * 120 + 40, 160), (240, 240, 240))
for idx, shape in enumerate(('circle', 'squircle', 'square')):
    t = vis.resize((96, 96), Image.LANCZOS)
    m = Image.new('L', (96 * SS, 96 * SS), 0); md = ImageDraw.Draw(m)
    if shape == 'circle': md.ellipse([0, 0, 96 * SS - 1, 96 * SS - 1], fill=255)
    elif shape == 'squircle': md.rounded_rectangle([0, 0, 96 * SS - 1, 96 * SS - 1], radius=int(96 * SS * .3), fill=255)
    else: md.rectangle([0, 0, 96 * SS - 1, 96 * SS - 1], fill=255)
    t.putalpha(m.resize((96, 96), Image.LANCZOS))
    small.paste(t, (20 + idx * 130, 32), t)
small.save(os.path.join(OUT, 'preview-launcher.png'))
# 피그마 크기 비교용 360 그림
render(360, 360).convert('RGB').save(os.path.join(OUT, 'render-360.png'))
print('완료')

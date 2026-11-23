"""모닝업 일정, 이슈 관리 CLI. 원본: docs/pm/tasks.jsonl, issues.jsonl, events.jsonl (한 줄에 한 항목).

읽기 (--as 없어도 됨)
  pm.py list [--status todo,doing] [--area dev] [--owner "[개발 13]"] [--verify user] [--issues]
  pm.py show TSK-042                 # 필드 전부 + 댓글 + 활동 기록
  pm.py mine --as "[개발 13]"         # 내 일, 내가 검토할 것, 내 이슈
  pm.py ready [--as X]               # 선행이 다 끝난 todo
  pm.py blocked                      # 선행이 안 끝나 막힌 일
  pm.py log [TSK-042] [--limit 30]   # 변경 기록
  pm.py check | build [--force]

쓰기 (모두 --as "[방 이름]" 필수, 기록은 events.jsonl)
  pm.py add-task --parent 4 --title "..." --as Leader [key=value ...]   # parent는 묶음 WBS(1~7) 또는 TSK 키
  pm.py add-issue --title "..." --as "[QA 1]" [severity=high task=TSK-042 repro="..."]
  pm.py set <TSK-/ISS-> --as X key=value [key+=v ...]
  pm.py claim TSK-042 --as X          # owner=X, doing
  pm.py submit TSK-042 --as X [--reviewer Leader] [--note "..."]   # → review
  pm.py approve <id> --as X [--user]  # 일: review → done (--user 면 waiting_user), waiting_user → done / 이슈: resolved → closed
  pm.py reject <id> --as X --note "이유"  # 일: review → doing / 이슈: resolved → reopened
  pm.py resolve ISS-007 --as X [--fix "..."] [commits+=abc1234]
  pm.py reopen ISS-007 --as X [--note "..."]
  pm.py comment <id> --as X "내용"
  pm.py add-item TSK-042 --as X "체크리스트 항목"
  pm.py check-item TSK-042 2 --as X [--undo]

리스트 필드는 공백이나 쉼표 구분: deps="TSK-041 TSK-040". key+=v 는 덧붙임, key-=v 는 빼기, key= 는 비움.
여러 줄 필드(acceptance)는 '|' 로 구분: acceptance="조건1|조건2".
표준 라이브러리만 쓴다. 동시에 여러 세션이 써도 되도록 docs/pm/.lock 잠금 + 임시 파일 교체로 저장한다.
"""
import argparse
import copy
import datetime as dt
import json
import os
import re
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PM = Path(os.environ.get("PM_DIR") or ROOT / "docs" / "pm")
TASKS = PM / "tasks.jsonl"
ISSUES = PM / "issues.jsonl"
EVENTS = PM / "events.jsonl"
LOCK = PM / ".lock"
OUT = PM / "board.html"
TEMPLATE = Path(__file__).with_name("board_template.html")

TASK_STATUS = ["todo", "doing", "review", "waiting_user", "done", "hold", "cancelled"]
ISSUE_STATUS = ["open", "doing", "reopened", "resolved", "closed"]
SEVERITY = ["high", "medium", "low"]
VERIFY = ["auto", "agent", "user"]  # 자동 시험 / Claude 가 adb·로그로 확인 / 사용자가 실제 폰으로
AREA = ["ops", "plan", "design", "dev", "release", "marketing", "qa"]
AREA_WBS = {"ops": "1", "plan": "2", "design": "3", "dev": "4", "release": "5", "marketing": "6", "qa": "7"}
DEFAULT_REVIEWER = {"ops": "Leader", "plan": "Leader", "design": "Leader", "dev": "QA", "release": "Leader",
                    "marketing": "Leader", "qa": "Leader"}
CLOSED_TASK = {"done", "cancelled"}
OPEN_ISSUE = {"open", "doing", "reopened"}

TASK_FIELDS = ["id", "wbs", "parent", "title", "area", "owner", "reviewer", "verify", "status", "start", "due",
               "deps", "relates", "decisions", "commits", "refs", "screens", "figma",
               "desc", "acceptance", "checklist", "comments", "milestone"]
ISSUE_FIELDS = ["id", "title", "area", "status", "severity", "owner", "reporter", "reviewer", "task",
                "found", "resolved", "reopen_count", "desc", "repro", "expected", "actual", "cause", "fix",
                "relates", "commits", "refs", "screens", "comments"]
LIST_FIELDS = {"deps", "relates", "decisions", "commits", "refs", "screens", "figma", "acceptance",
               "checklist", "comments"}
DATE_FIELDS = {"start", "due", "found", "resolved"}
# set 으로 직접 못 고치는 필드 (전용 명령이 있음)
MANAGED = {"id", "wbs", "comments", "checklist", "reopen_count"}

KO_TASK = {"todo": "대기", "doing": "진행 중", "review": "검토 요망", "waiting_user": "확인 요망", "done": "완료",
           "hold": "보류", "cancelled": "취소"}
KO_ISSUE = {"open": "열림", "doing": "진행 중", "reopened": "재발생", "resolved": "해결", "closed": "닫힘"}
KO_SEV = {"high": "높음", "medium": "보통", "low": "낮음"}
KO_VERIFY = {"auto": "자동 시험", "agent": "Claude 확인", "user": "사용자 확인"}
KO_AREA = {"ops": "운영", "plan": "기획", "design": "디자인", "dev": "개발", "release": "출시",
           "marketing": "마케팅", "qa": "QA"}
ROLE_OF_AREA = {"plan": "기획", "design": "디자인", "dev": "개발", "marketing": "마케팅", "qa": "QA"}

ACTOR_RE = re.compile(r"^(Leader|사용자|\[(기획|디자인|개발|마케팅|QA) \d+(-\d+)?\])$")


def die(msg, code=2):
    print("오류:", msg, file=sys.stderr)
    sys.exit(code)


def now():
    return dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def today():
    return dt.date.today().isoformat()


def role(name):
    """'[개발 13]' → '개발', 'Leader' → 'Leader', '디자인 방' → '디자인', 'QA' → 'QA'."""
    if not name:
        return ""
    m = re.match(r"^\[(\S+) [\d~,\- ]+\]", name)
    if m:
        return m.group(1)
    return name.replace(" 방", "").strip()


def matches(person, actor):
    """담당/검토자 칸이 이 세션을 가리키는지. 정확히 같거나, 칸이 역할 이름(QA, 개발 방)이면 같은 역할."""
    if not person or not actor:
        return False
    if person == actor:
        return True
    return not person.startswith("[") and person not in ("사용자",) and role(person) == role(actor)


# ---------- 잠금과 저장 ----------

class Lock:
    def __init__(self, wait=30.0, stale=120.0):
        self.wait, self.stale = wait, stale

    def __enter__(self):
        PM.mkdir(parents=True, exist_ok=True)
        end = time.time() + self.wait
        while True:
            try:
                fd = os.open(LOCK, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                os.write(fd, f"{os.getpid()} {now()}".encode())
                os.close(fd)
                return self
            except FileExistsError:
                try:
                    if time.time() - LOCK.stat().st_mtime > self.stale:
                        LOCK.unlink()  # 죽은 세션이 남긴 잠금
                        continue
                except FileNotFoundError:
                    continue
                if time.time() > end:
                    die(f"다른 세션이 {self.wait:.0f}초 넘게 쓰는 중이에요 ({LOCK}). 잠시 뒤 다시 해 주세요")
                time.sleep(0.05)

    def __exit__(self, *exc):
        try:
            LOCK.unlink()
        except FileNotFoundError:
            pass


def atomic_write(path, text):
    tmp = path.with_name(f".{path.name}.{os.getpid()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
        f.flush()
        os.fsync(f.fileno())
    for i in range(100):  # 윈도우는 다른 프로세스가 읽는 중이면 잠깐 거부함
        try:
            os.replace(tmp, path)
            return
        except PermissionError:
            time.sleep(0.05)
    die(f"{path.name} 저장 실패 (다른 프로그램이 잡고 있어요)")


def read_text(path):
    for i in range(100):
        try:
            return path.read_text(encoding="utf-8") if path.exists() else ""
        except PermissionError:
            time.sleep(0.05)
    die(f"{path.name} 를 못 읽었어요")


def default_of(k):
    if k in LIST_FIELDS:
        return []
    if k == "milestone":
        return False
    if k == "reopen_count":
        return 0
    return None


def load(path, fields):
    items = []
    for n, line in enumerate(read_text(path).splitlines(), 1):
        if not line.strip():
            continue
        try:
            o = json.loads(line)
        except json.JSONDecodeError as e:
            die(f"{path.name} {n}줄 JSON 오류: {e}")
        items.append({k: o.get(k, default_of(k)) for k in fields})
    return items


def dump(items, fields):
    return "".join(json.dumps({k: o.get(k) for k in fields}, ensure_ascii=False) + "\n" for o in items)


def load_events():
    out = []
    for line in read_text(EVENTS).splitlines():
        if line.strip():
            try:
                out.append(json.loads(line))
            except json.JSONDecodeError:
                pass
    return out


def append_events(evs):
    if not evs:
        return
    with open(EVENTS, "a", encoding="utf-8", newline="\n") as f:
        for e in evs:
            f.write(json.dumps(e, ensure_ascii=False) + "\n")
        f.flush()
        os.fsync(f.fileno())


def diff_events(before, after, actor, action, extra=None):
    """전후 비교로 바뀐 필드를 기록. comments 는 길이만 비교해 새 댓글 내용을 남김."""
    evs = []
    at = now()
    for id_, o in after.items():
        b = before.get(id_)
        ch = {}
        for k, v in o.items():
            bv = b.get(k) if b else default_of(k)
            if v == bv:
                continue
            if k == "comments":
                new = v[len(bv or []):]
                ch[k] = [None, [c["text"] for c in new]]
            else:
                ch[k] = [bv, v]
        if ch:
            e = {"at": at, "by": actor, "id": id_, "action": action if b else "create", "changes": ch}
            if extra:
                e.update(extra)
            evs.append(e)
    return evs


# ---------- 값 다루기 ----------

def coerce(key, raw):
    if key in ("checklist",):
        return [{"text": p.strip(), "done": False} for p in raw.split("|") if p.strip()]
    if key == "acceptance":
        return [p.strip() for p in raw.split("|") if p.strip()]
    if key in LIST_FIELDS:
        return [p for p in raw.replace(",", " ").split() if p]
    if key == "milestone":
        return raw.lower() in ("1", "true", "yes", "y", "예")
    if raw == "":
        return None
    if key in DATE_FIELDS:
        try:
            dt.date.fromisoformat(raw)
        except ValueError:
            die(f"{key} 는 YYYY-MM-DD 여야 해요: {raw}")
    return raw


def apply_pairs(obj, pairs, fields, allow_managed=False):
    for p in pairs:
        m = re.match(r"^([a-z_]+)([+-]?)=(.*)$", p, re.S)
        if not m:
            die(f"key=value 모양이 아니에요: {p}")
        k, op, v = m.groups()
        if k not in fields or (k in MANAGED and not allow_managed):
            ok = [f for f in fields if f not in MANAGED]
            die(f"고칠 수 없는 필드: {k} (가능: {', '.join(ok)})")
        val = coerce(k, v)
        if op:
            if k not in LIST_FIELDS:
                die(f"+= -= 는 리스트 필드에만: {k}")
            cur = list(obj.get(k) or [])
            obj[k] = cur + [x for x in val if x not in cur] if op == "+" else [x for x in cur if x not in val]
        else:
            obj[k] = val


def wbs_key(w):
    return [int(p) if p.isdigit() else 0 for p in str(w or "").split(".")]


def is_group(t):
    return t.get("parent") is None


def next_key(items, prefix):
    nums = [int(o["id"].split("-")[1]) for o in items if o["id"].startswith(prefix + "-")]
    return f"{prefix}-{max(nums, default=0) + 1:03d}"


def next_wbs(tasks, group):
    nums = [int(t["wbs"].split(".")[1]) for t in tasks
            if t["parent"] == group["id"] and t["wbs"] and t["wbs"].split(".")[1].isdigit()]
    return f"{group['wbs']}.{max(nums, default=0) + 1}"


def by_id(tasks, issues):
    d = {o["id"]: o for o in tasks}
    d.update({o["id"]: o for o in issues})
    return d


def find(db, id_):
    id_ = id_.upper() if re.match(r"^(tsk|iss)-", id_, re.I) else id_
    if id_ in db:
        return db[id_]
    for o in db.values():  # WBS 번호로도 찾음
        if o.get("wbs") == id_:
            return o
    die(f"{id_} 를 못 찾았어요")


def label(o):
    w = f" ({o['wbs']})" if o.get("wbs") else ""
    a = f"[{KO_AREA.get(o.get('area'), o.get('area') or '')}] " if o.get("area") else ""
    return f"{o['id']}{w} {a}{o['title']}"


def deps_done(t, db):
    return all(db.get(d, {}).get("status") in CLOSED_TASK for d in t["deps"] or [])


# ---------- 검사 ----------

def validate(tasks, issues):
    errs = []
    db = by_id(tasks, issues)
    seen, wseen = set(), set()
    for t in tasks:
        w = f"{t['id']}"
        if not re.match(r"^TSK-\d{3,}$", t["id"] or ""):
            errs.append(f"{w}: 키는 TSK-001 모양")
        if t["id"] in seen:
            errs.append(f"{w}: 키 중복")
        seen.add(t["id"])
        if t["wbs"] in wseen:
            errs.append(f"{w}: wbs {t['wbs']} 중복")
        wseen.add(t["wbs"])
        if t["parent"] is not None and t["parent"] not in db:
            errs.append(f"{w}: parent {t['parent']} 없음")
        if t["area"] not in AREA:
            errs.append(f"{w}: area '{t['area']}' (가능: {', '.join(AREA)})")
        if not is_group(t) and t["status"] not in TASK_STATUS:
            errs.append(f"{w}: status '{t['status']}' (가능: {', '.join(TASK_STATUS)})")
        for d in (t["deps"] or []) + (t["relates"] or []):
            if d not in db:
                errs.append(f"{w}: 참조 {d} 없음")
        if t["start"] and t["due"] and t["due"] < t["start"]:
            errs.append(f"{w}: due 가 start 보다 빠름")
        if t["status"] == "done" and not is_group(t) and not t["due"]:
            errs.append(f"{w}: done 인데 due 없음")
        if t["verify"] is not None and t["verify"] not in VERIFY:
            errs.append(f"{w}: verify '{t['verify']}' (가능: {', '.join(VERIFY)})")
        if t["area"] == "qa" and not is_group(t) and not t.get("milestone") and t["status"] != "cancelled" and not t["verify"]:
            errs.append(f"{w}: QA 일은 verify 가 필요해요 (auto, agent, user)")
        if t["status"] == "review" and not t["reviewer"]:
            errs.append(f"{w}: review 인데 reviewer 없음")
        if re.match(r"^\[(기획|디자인|개발|QA|운영|출시|마케팅)\]", t["title"] or ""):
            errs.append(f"{w}: 제목에 [분야]를 넣지 않아요 (area 에서 자동으로 붙음)")
    seen = set()
    for i in issues:
        w = f"{i['id']}"
        if not re.match(r"^ISS-\d{3,}$", i["id"] or ""):
            errs.append(f"{w}: 키는 ISS-001 모양")
        if i["id"] in seen:
            errs.append(f"{w}: 키 중복")
        seen.add(i["id"])
        if i["status"] not in ISSUE_STATUS:
            errs.append(f"{w}: status '{i['status']}' (가능: {', '.join(ISSUE_STATUS)})")
        if i["severity"] not in SEVERITY:
            errs.append(f"{w}: severity '{i['severity']}' (가능: {', '.join(SEVERITY)})")
        if i["area"] and i["area"] not in AREA:
            errs.append(f"{w}: area '{i['area']}'")
        for d in ([i["task"]] if i["task"] else []) + (i["relates"] or []):
            if d not in db:
                errs.append(f"{w}: 참조 {d} 없음")
        if i["status"] in ("resolved", "closed") and not i["resolved"]:
            errs.append(f"{w}: {i['status']} 인데 resolved 날짜 없음")
    return errs


# ---------- 화면 데이터 ----------

def build(tasks, issues):
    events = load_events()
    groups = {t["id"]: t for t in tasks if is_group(t)}
    out = []
    for t in tasks:
        o = dict(t)
        if is_group(t):
            kids = [k for k in tasks if k["parent"] == t["id"]]
            ss = [k["start"] or k["due"] for k in kids if k["start"] or k["due"]]
            ee = [k["due"] or k["start"] for k in kids if k["start"] or k["due"]]
            o["start"], o["due"] = (min(ss) if ss else None), (max(ee) if ee else None)
        out.append(o)
    data = {
        "built": dt.datetime.now().strftime("%Y-%m-%d %H:%M"),
        "today": today(),
        "labels": {"task": KO_TASK, "issue": KO_ISSUE, "sev": KO_SEV, "area": KO_AREA, "verify": KO_VERIFY},
        "order": {"task": TASK_STATUS, "issue": ISSUE_STATUS, "sev": SEVERITY, "area": AREA, "verify": VERIFY},
        "tasks": out, "issues": issues, "events": events[-3000:],
    }
    blob = json.dumps(data, ensure_ascii=False).replace("</", "<\\/")
    atomic_write(OUT, read_text(TEMPLATE).replace("/*__DATA__*/null", blob))
    print(f"built {OUT} (일 {len([t for t in tasks if not is_group(t)])}, 이슈 {len(issues)}, 기록 {len(events)})")


# ---------- 읽기 명령 ----------

def row(t):
    rv = f" 검토:{t['reviewer']}" if t.get("reviewer") and t["status"] == "review" else ""
    vf = f" <{KO_VERIFY[t['verify']]}>" if t.get("verify") in KO_VERIFY else ""
    return f"{t['id']:<8} {t.get('wbs') or '':<5} {t['status']:<12} {(t.get('start') or '-')}~{(t.get('due') or '-')}  {t.get('owner') or '-'}{rv}  [{KO_AREA.get(t['area'], '')}] {t['title']}{vf}"


def irow(i):
    return f"{i['id']:<8} {i['status']:<9} {i['severity']:<6} {i.get('owner') or '-':<10} {i.get('task') or '-':<8} {i['title']}"


def cmd_list(a, tasks, issues):
    st = set(a.status.split(",")) if a.status else None
    ar = set(a.area.split(",")) if a.area else None
    if a.issues:
        for i in issues:
            if st and i["status"] not in st or ar and i["area"] not in ar or a.owner and not matches(i["owner"], a.owner):
                continue
            print(irow(i))
        return
    for t in tasks:
        if is_group(t):
            continue
        if st and t["status"] not in st or ar and t["area"] not in ar or a.owner and a.owner not in (t["owner"] or "")                 or a.verify and t.get("verify") not in a.verify.split(","):
            continue
        print(row(t))


def cmd_show(a, tasks, issues):
    db = by_id(tasks, issues)
    o = find(db, a.id)
    print(label(o))
    for k, v in o.items():
        if k in ("id", "title", "comments", "checklist") or v in (None, [], "", False):
            continue
        if k == "acceptance":
            print("  acceptance:")
            for n, x in enumerate(v, 1):
                print(f"    {n}. {x}")
            continue
        print(f"  {k}: {' '.join(v) if isinstance(v, list) else v}")
    if o.get("checklist"):
        print("  checklist:")
        for n, c in enumerate(o["checklist"], 1):
            print(f"    {n}. [{'x' if c['done'] else ' '}] {c['text']}")
    blocks = [t["id"] for t in tasks if o["id"] in (t["deps"] or [])]
    if blocks:
        print(f"  blocks: {' '.join(blocks)}")
    rel = [i["id"] for i in issues if i["task"] == o["id"]]
    if rel:
        print(f"  issues: {' '.join(rel)}")
    for c in o.get("comments") or []:
        print(f"  💬 {c['at']} {c['by']}: {c['text']}")
    evs = [e for e in load_events() if e["id"] == o["id"]]
    for e in evs[-15:]:
        print(f"  · {e['at']} {e['by']} {e['action']} {', '.join(e['changes'])}")


def cmd_mine(a, tasks, issues):
    actor = need_actor(a)
    mine = [t for t in tasks if not is_group(t) and matches(t["owner"], actor) and t["status"] not in CLOSED_TASK | {"hold"}]
    rev = [t for t in tasks if t["status"] == "review" and matches(t["reviewer"], actor)]
    iss = [i for i in issues if matches(i["owner"], actor) and i["status"] in OPEN_ISSUE]
    irev = [i for i in issues if i["status"] == "resolved" and matches(i["reviewer"], actor)]
    if actor in ("사용자", "Leader"):
        wu = sorted([t for t in tasks if t["status"] == "waiting_user"], key=lambda t: t["due"] or "9")
        print(f"== 사용자 확인 차례 ({len(wu)})  확인하면 Leader 가 approve")
        for t in wu:
            print("  " + row(t))
    order = {s: n for n, s in enumerate(["doing", "review", "todo", "waiting_user"])}
    print(f"== {actor} 의 일 ({len(mine)})")
    for t in sorted(mine, key=lambda t: (order.get(t["status"], 9), t["due"] or "9")):
        print("  " + row(t))
    print(f"== 내가 검토할 일 ({len(rev)})")
    for t in rev:
        print("  " + row(t))
    print(f"== 내 이슈 ({len(iss)})")
    for i in iss:
        print("  " + irow(i))
    print(f"== 내가 확인할 해결 이슈 ({len(irev)})")
    for i in irev:
        print("  " + irow(i))


def cmd_ready(a, tasks, issues):
    db = by_id(tasks, issues)
    for t in tasks:
        if not is_group(t) and t["status"] == "todo" and deps_done(t, db):
            if a.actor and not matches(t["owner"], a.actor):
                continue
            print(row(t))


def cmd_blocked(a, tasks, issues):
    db = by_id(tasks, issues)
    for t in tasks:
        if not is_group(t) and t["status"] not in CLOSED_TASK and not deps_done(t, db):
            wait = [d for d in t["deps"] if db.get(d, {}).get("status") not in CLOSED_TASK]
            print(row(t) + f"   ← 기다림: {' '.join(wait)}")


def cmd_log(a, tasks, issues):
    evs = load_events()
    if a.id:
        o = find(by_id(tasks, issues), a.id)
        evs = [e for e in evs if e["id"] == o["id"]]
    for e in evs[-a.limit:]:
        parts = []
        if e["action"] == "create":
            parts.append(f"만듦 \"{e['changes'].get('title', [None, ''])[1]}\"")
        elif not e["changes"]:
            parts.append(e.get("text", ""))
        for k, (b, v) in ({} if e["action"] == "create" else e["changes"]).items():
            if k == "comments":
                parts.append(f"댓글 \"{' / '.join(v)}\"")
            elif isinstance(v, (list, dict)) or isinstance(b, (list, dict)):
                parts.append(k)
            else:
                parts.append(f"{k} {b}→{v}")
        print(f"{e['at']}  {e['by']:<10} {e['id']:<8} {e['action']:<9} {'; '.join(parts)}")


# ---------- 쓰기 명령 ----------

def need_actor(a):
    actor = getattr(a, "actor", None)
    if not actor:
        die('쓰기 명령은 --as "[방 이름]" 이 필요해요 (예: --as "[개발 13]", --as Leader)')
    if not ACTOR_RE.match(actor):
        die(f"--as 이름 모양이 틀려요: {actor} (Leader, 사용자, [기획 4], [개발 13], [QA 1], [디자인 12-1])")
    return actor


def comment(o, actor, text):
    o["comments"] = list(o.get("comments") or []) + [{"at": now(), "by": actor, "text": text}]


def mutate(a, actor, tasks, issues):
    """명령 하나를 적용. (action 이름, 출력할 항목) 을 돌려줌."""
    db = by_id(tasks, issues)
    c = a.cmd
    if c == "set":
        o = find(db, a.id)
        apply_pairs(o, a.pairs, ISSUE_FIELDS if o["id"].startswith("ISS-") else TASK_FIELDS)
        return "set", o
    if c == "add-task":
        g = find(db, a.parent)
        if not is_group(g):
            die(f"--parent 는 묶음이어야 해요 (1~7 또는 묶음 키). {g['id']} 는 일이에요")
        o = {k: default_of(k) for k in TASK_FIELDS}
        o.update(id=next_key(tasks, "TSK"), wbs=a.wbs or next_wbs(tasks, g), parent=g["id"], title=a.title,
                 area=g["area"], status="todo", reviewer=DEFAULT_REVIEWER.get(g["area"]))
        if any(t["wbs"] == o["wbs"] for t in tasks):
            die(f"wbs {o['wbs']} 가 이미 있어요")
        apply_pairs(o, a.pairs, TASK_FIELDS)
        tasks.append(o)
        tasks.sort(key=lambda t: wbs_key(t["wbs"]))
        return "create", o
    if c == "add-issue":
        o = {k: default_of(k) for k in ISSUE_FIELDS}
        o.update(id=next_key(issues, "ISS"), title=a.title, status="open", severity="medium", found=today(),
                 reporter=actor, reviewer="QA")
        apply_pairs(o, a.pairs, ISSUE_FIELDS)
        if o["task"] and not o["area"]:
            o["area"] = find(db, o["task"])["area"]
        issues.append(o)
        return "create", o
    o = find(db, a.id)
    is_issue = o["id"].startswith("ISS-")
    st = o["status"]
    if c == "comment":
        comment(o, actor, a.text)
        return "comment", o
    if c == "add-item":
        o["checklist"] = list(o.get("checklist") or []) + [{"text": a.text, "done": False}]
        return "add-item", o
    if c == "check-item":
        items = [dict(x) for x in o.get("checklist") or []]
        if not 1 <= a.n <= len(items):
            die(f"{o['id']} 체크리스트는 1~{len(items)}번까지 있어요")
        items[a.n - 1]["done"] = not a.undo
        o["checklist"] = items
        return "check-item", o
    if c == "claim":
        if is_issue:
            if st not in OPEN_ISSUE:
                die(f"{o['id']} 는 {st} 라 가져갈 수 없어요")
            o.update(owner=actor, status="doing")
            return "claim", o
        if st not in ("todo", "doing", "hold") or (st == "doing" and o["owner"] != actor and not a.force):
            die(f"{o['id']} 는 지금 {st} ({o['owner']}) 이에요. 가로채려면 --force")
        if not deps_done(o, db):
            print(f"  주의: 선행이 안 끝났어요: {' '.join(d for d in o['deps'] if db[d]['status'] not in CLOSED_TASK)}")
        o.update(owner=actor, status="doing")
        if not o["start"] or o["start"] > today():
            o["start"] = today()
        if o["due"] and o["due"] < o["start"]:
            o["due"] = o["start"]
        return "claim", o
    if c == "submit":
        if is_issue:
            die("이슈는 submit 대신 resolve 를 써요")
        if st not in ("doing", "todo"):
            die(f"{o['id']} 는 {st} 라 검토로 넘길 수 없어요 (doing 이어야 해요)")
        undone = [x["text"] for x in o.get("checklist") or [] if not x["done"]]
        if undone:
            print(f"  주의: 체크리스트 {len(undone)}개가 안 끝났어요")
        o.update(status="review", reviewer=a.reviewer or o["reviewer"] or DEFAULT_REVIEWER.get(o["area"], "Leader"))
        if o.get("verify") == "user":
            o["status"] = "waiting_user"  # 사용자가 실제 폰으로 볼 차례
            print("  사용자 확인 일이라 waiting_user 로 넘겼어요")
        if a.note:
            comment(o, actor, a.note)
        return "submit", o
    if c in ("approve", "reject") and st in ("review", "resolved") and o.get("reviewer") \
            and actor != "Leader" and not matches(o["reviewer"], actor):
        die(f"{o['id']} 검토자는 {o['reviewer']} 예요 (Leader 도 가능)")
    if c == "approve":
        if is_issue:
            if st != "resolved":
                die(f"{o['id']} 는 {st} 예요. resolved 일 때만 닫을 수 있어요")
            o["status"] = "closed"
        elif st == "review":
            if o["owner"] == actor and actor != "Leader":
                die("자기 일은 스스로 승인하지 않아요. 검토자에게 맡겨 주세요")
            o["status"] = "waiting_user" if a.user else "done"
            if o["status"] == "done":
                o["due"] = today() if not o["due"] or o["due"] > today() or (o["start"] and o["due"] < o["start"]) else o["due"]
        elif st == "waiting_user":
            if actor not in ("Leader", "사용자") and not matches(o.get("reviewer"), actor):
                die("확인 요망(waiting_user) 일은 사용자 확인 뒤 Leader(또는 검토자)가 승인해요")
            o["status"] = "done"
            o["due"] = today() if not o["due"] or o["due"] > today() else o["due"]
        else:
            die(f"{o['id']} 는 {st} 예요. review 나 waiting_user 일 때만 승인해요")
        if o.get("start") and o.get("due") and o["due"] < o["start"]:
            o["start"] = o["due"]
        if a.note:
            comment(o, actor, a.note)
        return "approve", o
    if c == "reject":
        if not a.note:
            die('반려는 이유가 필요해요: --note "..."')
        if is_issue:
            if st != "resolved":
                die(f"{o['id']} 는 {st} 예요. resolved 일 때만 반려해요")
            o.update(status="reopened", resolved=None, reopen_count=(o["reopen_count"] or 0) + 1)
        else:
            if st not in ("review", "waiting_user"):
                die(f"{o['id']} 는 {st} 예요. review 일 때만 반려해요")
            o["status"] = "doing"
        comment(o, actor, "반려: " + a.note)
        return "reject", o
    if c == "resolve":
        if not is_issue:
            die("resolve 는 이슈에만 써요")
        if st not in OPEN_ISSUE:
            die(f"{o['id']} 는 {st} 예요")
        o.update(status="resolved", resolved=today())
        if a.fix:
            o["fix"] = a.fix
        apply_pairs(o, a.pairs, ISSUE_FIELDS)
        return "resolve", o
    if c == "reopen":
        if not is_issue:
            die("reopen 은 이슈에만 써요")
        o.update(status="reopened", resolved=None, reopen_count=(o["reopen_count"] or 0) + 1)
        if a.note:
            comment(o, actor, "재발생: " + a.note)
        return "reopen", o
    die(f"모르는 명령 {c}")


def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except (AttributeError, ValueError):
        pass
    ap = argparse.ArgumentParser(description="모닝업 일정, 이슈 관리")
    sp = ap.add_subparsers(dest="cmd", required=True)

    def P(name, *ids, write=True):
        p = sp.add_parser(name)
        for i in ids:
            p.add_argument(i)
        p.add_argument("--as", dest="actor", required=False)
        return p

    p = P("list", write=False)
    p.add_argument("--status"); p.add_argument("--area"); p.add_argument("--owner")
    p.add_argument("--issues", action="store_true"); p.add_argument("--verify")
    P("show", "id", write=False)
    P("mine", write=False)
    P("ready", write=False)
    P("blocked", write=False)
    p = P("log", write=False)
    p.add_argument("id", nargs="?"); p.add_argument("--limit", type=int, default=40)
    P("check", write=False)
    p = P("build", write=False)
    p.add_argument("--force", action="store_true")

    p = P("set", "id"); p.add_argument("pairs", nargs="+")
    p = P("add-task"); p.add_argument("--parent", required=True); p.add_argument("--title", required=True)
    p.add_argument("--wbs"); p.add_argument("pairs", nargs="*")
    p = P("add-issue"); p.add_argument("--title", required=True); p.add_argument("pairs", nargs="*")
    p = P("claim", "id"); p.add_argument("--force", action="store_true")
    p = P("submit", "id"); p.add_argument("--reviewer"); p.add_argument("--note")
    p = P("approve", "id"); p.add_argument("--user", action="store_true"); p.add_argument("--note")
    p = P("reject", "id"); p.add_argument("--note")
    p = P("resolve", "id"); p.add_argument("--fix"); p.add_argument("pairs", nargs="*")
    p = P("reopen", "id"); p.add_argument("--note")
    P("comment", "id", "text")
    P("add-item", "id", "text")
    p = P("check-item", "id"); p.add_argument("n", type=int); p.add_argument("--undo", action="store_true")
    a = ap.parse_args()

    reads = {"list": cmd_list, "show": cmd_show, "mine": cmd_mine, "ready": cmd_ready,
             "blocked": cmd_blocked, "log": cmd_log}
    if a.cmd in reads:
        return reads[a.cmd](a, load(TASKS, TASK_FIELDS), load(ISSUES, ISSUE_FIELDS))
    if a.cmd == "check":
        errs = validate(load(TASKS, TASK_FIELDS), load(ISSUES, ISSUE_FIELDS))
        for e in errs:
            print("  -", e)
        print("ok" if not errs else f"{len(errs)}개 문제")
        return 1 if errs else 0
    if a.cmd == "build":
        tasks, issues = load(TASKS, TASK_FIELDS), load(ISSUES, ISSUE_FIELDS)
        errs = validate(tasks, issues)
        for e in errs:
            print("  -", e)
        if errs and not a.force:
            die("검사 오류가 있어 만들지 않았어요 (--force 로 강제)")
        return build(tasks, issues)

    actor = need_actor(a)
    with Lock():
        tasks, issues = load(TASKS, TASK_FIELDS), load(ISSUES, ISSUE_FIELDS)
        before = copy.deepcopy(by_id(tasks, issues))
        action, o = mutate(a, actor, tasks, issues)
        evs = diff_events(before, by_id(tasks, issues), actor, action)
        if not evs:
            print("바뀐 것이 없어요")
            return 0
        if TASKS.exists() or tasks:
            atomic_write(TASKS, dump(tasks, TASK_FIELDS))
        atomic_write(ISSUES, dump(issues, ISSUE_FIELDS))
        append_events(evs)
        errs = validate(tasks, issues)
    print(f"{action}: {label(o)} [{o['status']}]" + (f" 담당 {o['owner']}" if o.get("owner") else ""))
    for e in errs[:10]:
        print("  주의:", e)
    return 0


if __name__ == "__main__":
    sys.exit(main() or 0)

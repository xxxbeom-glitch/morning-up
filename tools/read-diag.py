"""모닝업 개발용 빌드의 진단 기록을 폰에서 꺼내 읽어요(결정 69). 화면에는 보여 주지 않는 기록이에요.

쓰는 법(Windows, 폰을 adb로 연결한 상태):
    python tools/read-diag.py            # 최근 80줄
    python tools/read-diag.py 300        # 최근 300줄
    python tools/read-diag.py 80 17      # 17번 알람만

Git Bash는 /data 경로를 바꿔 버려서 PowerShell이나 cmd에서 실행해요.
"""
import datetime
import os
import sqlite3
import subprocess
import sys
import tempfile

PKG = 'com.lumian.morningup.debug'
DB = f'/data/user_de/0/{PKG}/databases/morningup.db'


def pull(dest_dir):
    for suffix in ('', '-wal'):
        out = os.path.join(dest_dir, 'morningup.db' + suffix)
        with open(out, 'wb') as f:
            subprocess.run(['adb', 'exec-out', 'run-as', PKG, 'cat', DB + suffix], stdout=f, check=False)
    return os.path.join(dest_dir, 'morningup.db')


def main():
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 80
    alarm = int(sys.argv[2]) if len(sys.argv) > 2 else None
    with tempfile.TemporaryDirectory() as tmp:
        con = sqlite3.connect(pull(tmp))
        sql = 'select at, event, alarmId, detail from diagnostic_log'
        args = ()
        if alarm is not None:
            sql += ' where alarmId = ?'
            args = (alarm,)
        rows = con.execute(sql + ' order by id desc limit ?', (*args, limit)).fetchall()
        con.close()
    for at, event, alarm_id, detail in reversed(rows):
        time = datetime.datetime.fromtimestamp(at / 1000).strftime('%m-%d %H:%M:%S')
        aid = f'#{alarm_id}' if alarm_id is not None else '-'
        print(f'{time}  {event:<20} {aid:<5} {detail or ""}')


if __name__ == '__main__':
    main()

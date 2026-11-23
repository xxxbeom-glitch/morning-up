# morning-up 작업 규칙 (모든 방 공통)

## 일 관리판
- 일과 이슈의 원본은 `docs/pm/`(tasks.jsonl, issues.jsonl). 자세한 규칙은 `docs/pm/README.md`.
- 고칠 때는 `python tools/pm/pm.py ...`만 써요. 파일을 직접 고치지 않아요.
- 모든 쓰기 명령에 `--as "[내 방 이름]"`을 붙여요.

## 시작할 때
1. `python tools/pm/pm.py mine --as "[내 방 이름]"`으로 내 일, 내가 검토할 일을 봐요.
2. 일을 시작하면 `claim 키`, 끝나면 `submit 키 --note "한 일"`.
3. 검토자는 완료 조건을 보고 `approve` 또는 `reject --note "이유"`. 자기 일은 스스로 승인하지 않아요(Leader만 예외).
4. 결함을 찾으면 `add-issue --title "증상 한 줄" task=TSK-xxx`. 같은 문제가 또 나오면 `reopen`.

## 일 추가
- `add-task --parent 묶음번호 --title "대상 + 할 일"`. 묶음: 1 운영, 2 기획, 3 디자인, 4 개발, 5 출시, 6 마케팅, 7 QA.
- 제목에 [분야], 결정 번호, 커밋을 넣지 않고 필드(decisions, commits, screens)에 넣어요.
- Leader는 사용자 지시를 받으면 상위 일부터 등록하고(owner, reviewer, 날짜, 완료 조건), 팀 방에 넘겨요.

## 방 이름
- `Leader` 하나, 팀 방 `[분야 번호] 하는 일`(예: `[디자인 12] 온보딩 모션`), 일꾼 세션 `[분야 번호-n] 하는 일`.
- 일꾼 세션은 상태를 바꾸지 않고 자기 이름으로 `comment`, `check-item`만 해요.

## 화면 이름
- 화면 ID와 피그마 프레임 이름은 `docs/pm/naming.md`를 따라요.

## QA
- QA 기준과 확인 방법 구분(auto, agent, user)은 `docs/pm/qa-guide.md`. 폰 조용한 시간(밤 10시~아침 9시)에는 설치, 권한 변경, 알람 시험을 하지 않아요.

## 넣지 않는 것
- 개인정보와 사업자 정보(이름, 주소, 전화, 메일, 등록번호)는 관리판, 문서, 커밋에 넣지 않아요.

## 일정판 링크로 소통
- 일정판(비공개 아티팩트): https://claude.ai/artifact/E1UdTv9hCMTq4YTKQGbuF2 . 주소 뒤에 `#TSK-번호`를 붙이면 그 일이 펼쳐져요.
- Leader는 사용자에게 보고할 때 관련 일의 링크(`…E1UdTv9hCMTq4YTKQGbuF2#TSK-xxx`)를 붙여요. 사용자가 TSK 번호로 물으면 그 일부터 봐요.
- 관리판이 바뀌면 Leader가 `pm.py build` → `python tools/pm/publish_board.py` → `docs/exports/board-artifact.html`을 같은 경로로 다시 올리고, 오른쪽 패널에 열어 둬요.

# docs/pm 규칙 (모든 Claude 방용)

일정(WBS), 이슈, 변경 기록의 원본. 사람이 보는 화면은 `board.html`(빌드 산출물, 직접 고치지 않음). GitHub에 올리지 않음.
`docs/leader-board.md`는 방 목록과 세션 id만 적고, **일의 상태 원본은 이 보드**예요.

## 파일
| 파일 | 뜻 |
|---|---|
| `tasks.jsonl` | 일. 한 줄에 JSON 한 개, 키 순서 고정 |
| `issues.jsonl` | 이슈(결함, 운영 문제) |
| `events.jsonl` | 모든 변경 기록(시각, 누가, 키, 필드 전후). 덧붙이기만 함 |
| `board.html` | `pm.py build` 산출물. 데이터가 안에 들어가 오프라인으로 열림 |
| `naming.md` | 화면 ID, 프레임 이름, 용어집 |
| `qa-guide.md` | QA 기준(바탕 기준, verify 구분, 심각도, 품질 관문) |
| `reference.md` | 보드 화면 레퍼런스(Mobbin) |
| `claude-md-draft.md` | 프로젝트 루트 CLAUDE.md 초안(사용자 확인 대기) |
| `.lock` | 쓰는 동안만 생기는 잠금 파일. 2분 넘게 남으면 자동으로 치움 |

편집은 `tools/pm/pm.py`로만 해요. 여러 세션이 동시에 써도 잠금(`.lock`)과 임시 파일 교체로 안전해요(11-23 시험: 4세션 동시 100번, 빠짐 0).

## 키와 제목 규칙
- 일 키 `TSK-001`부터, 이슈 키 `ISS-001`부터. 세 자리, 넘치면 네 자리. **한 번 정하면 안 바꾸고 재사용 안 함.** deps, task, relates 참조는 모두 키로 써요.
- `wbs`(`4.31`)는 간트 묶기와 정렬용 위치 번호예요. 묶음 7개: 1 운영 ops, 2 기획 plan, 3 디자인 design, 4 개발 dev, 5 출시 release, 6 마케팅 marketing, 7 QA qa.
- 화면 표시는 `TSK-042 · 4.31 [개발] 제목`. `[분야]`는 area에서 자동으로 붙으니 **제목에 넣지 않아요**(check가 막음).
- 제목은 `대상 + 할 일`, 명사형, 40자 안팎. 결정 번호, 커밋, 화면 ID는 제목 말고 필드로.
  - 좋음: `다시 잠들기 확인 횟수 구현`, `S4 시트 '몇 번 물을까요' 줄 추가`, `잠자기 모드 정시 울림 검증`
  - 피함: `[개발] 결정 137 반영 (abc1234)`, `이것저것 고치기`
- 이슈 제목은 증상 한 줄(무엇이 어떻게 안 됨): `다시 울릴 때 꺼진 화면이 안 켜지고 소리만 남`.

## 업무 흐름
```
일:   todo ─claim→ doing ─submit→ review ─approve→ done
                    ↑               │  └approve --user→ waiting_user ─approve→ done
                    └──── reject ───┘ (이유 필수, 댓글로 남음)
      hold(일부러 미룸, 되살리면 todo) / cancelled(안 하기로 함, desc 에 이유)
이슈: open ─claim→ doing ─resolve→ resolved ─approve→ closed
                                       └reject→ reopened (reopen_count+1) → 다시 resolve
```
- 기본 검토자: 운영, 기획, 디자인, 출시, 마케팅, QA → `Leader` / 개발 → `QA`. 디자인이 구현에 영향을 주면 submit 때 댓글로 개발 방을 불러요. 이슈 검토자는 `QA`(QA 방이 없으면 Leader가 대신).
- 검토자는 acceptance(완료 조건)를 보고 approve 또는 reject. 자기 일은 스스로 승인하지 않아요(Leader만 예외).
- 검토 중 결함을 찾으면 reject + `add-issue ... task=TSK-xxx`.
- 같은 문제가 또 나오면 새 이슈 말고 `reopen`.
- 사용자 차례(폰 확인, 결정)면 `approve --user`로 waiting_user. 사용자가 확인하면 Leader가 `approve`.
- QA 일은 `verify`가 꼭 있어요: `auto`(자동 시험) / `agent`(Claude가 adb, 로그로 확인) / `user`(사용자가 실제 폰으로). `user` 일은 `submit`하면 바로 waiting_user로 가요. 자세한 기준은 `qa-guide.md`.
- 출시 마일스톤 앞에 품질 관문 두 개: TSK-156(비공개 테스트 전), TSK-157(프로덕션 신청 전).

## 누가 언제 쓰나
| 누구 | 할 일 |
|---|---|
| 모든 방 | 시작할 때 `mine --as "[내 방]"`. 일을 시작하면 `claim`, 끝나면 `submit`. 진행 메모와 질문은 `comment`, 체크리스트는 `check-item` |
| 검토자 | `mine`의 '내가 검토할 일'을 보고 `approve` / `reject --note` |
| Leader | 사용자 지시를 받으면 **상위 일부터 `add-task`**(owner, reviewer, start, due, deps, acceptance). 팀 방에서 보고를 받으면 보드와 `leader-board.md`가 맞는지 보고 `build` |
| QA | 이슈 `add-issue`(repro, expected, actual), 해결 이슈 `approve` / `reject` |
| 일꾼 세션 | 자기 팀 방 이름이 아닌 자기 이름(`[개발 13-1]`)으로 `comment`, `check-item`만. 상태 변경은 팀 방이 |

- 모든 쓰기 명령은 `--as "[방 이름]"` 필수. 이름 모양: `Leader`, `사용자`, `[기획 4]`, `[개발 13]`, `[QA 1]`, `[디자인 12-1]`.
- 마일스톤 due가 밀리면 사용자에게 보고.
- 개인정보, 사업자 정보(번호, 주소, 전화, 메일)는 넣지 않음.

## 명령 (`D:\project\morning-up`에서 `python tools/pm/pm.py ...`)
| 명령 | 뜻 |
|---|---|
| `mine --as X` | 내 일, 내가 검토할 일, 내 이슈, 내가 확인할 해결 이슈(사용자, Leader는 '사용자 확인 차례'도) |
| `ready [--as X]` | 선행이 다 끝나 시작할 수 있는 todo |
| `blocked` | 선행이 안 끝나 막힌 일과 기다리는 키 |
| `list [--status ..] [--area ..] [--owner ..] [--verify user] [--issues]` | 목록 |
| `show TSK-042` | 필드 전부, 체크리스트, 댓글, 막는 일, 관련 이슈, 활동 기록 |
| `log [키] [--limit 40]` | 변경 기록 |
| `add-task --parent 4 --title ".." --as X [key=v ..]` | 일 추가(parent는 묶음 wbs 1~7 또는 묶음 키). 키, wbs 자동 |
| `add-issue --title ".." --as X [severity= task= repro= expected= actual=]` | 이슈 추가. reporter=X, found=오늘 |
| `set 키 --as X key=v key+=v key-=v` | 필드 고치기(`key=`는 비움) |
| `claim 키 --as X [--force]` | 담당을 X로, doing. 남이 doing 중이면 --force |
| `submit 키 --as X [--reviewer ..] [--note ..]` | review 로 |
| `approve 키 --as X [--user] [--note ..]` | 일 done(또는 waiting_user), 이슈 closed |
| `reject 키 --as X --note ".."` | 일 doing, 이슈 reopened |
| `resolve ISS-.. --as X [--fix ..] [commits+=..]` | 이슈 resolved |
| `reopen ISS-.. --as X [--note ..]` | 재발생 |
| `comment 키 --as X "내용"` | 댓글(반려 사유, 질문과 답, 진행 메모) |
| `add-item 키 --as X "항목"` / `check-item 키 2 --as X [--undo]` | 체크리스트 |
| `check` / `build [--force]` | 검사 / board.html 만들기 |

- 리스트 필드는 공백이나 쉼표로: `deps="TSK-041 TSK-040"`. `acceptance`와 `checklist`는 `|`로: `acceptance="처음 값 2번|3번 뒤 1분"`.
- 쓰기 명령은 저장 뒤 검사 결과를 '주의:'로 보여 줘요. `build`는 검사 오류가 있으면 만들지 않음(`--force`).

## tasks.jsonl 필드
| 키 | 타입 | 뜻 |
|---|---|---|
| id | str | `TSK-042` 고정 키 |
| wbs | str | `4.31` 위치 번호. 묶음은 `1`~`7` |
| parent | str/null | 묶음 키. null이면 묶음 줄(status, 날짜 null, 빌드가 자식에서 계산) |
| title | str | 위 제목 규칙 |
| area | enum | `ops` `plan` `design` `dev` `release` `marketing` `qa` |
| owner | str | 방 이름(`[개발 13]`), `Leader`, `사용자`. 끝난 일은 그때 방 이름 그대로 |
| reviewer | str/null | 검토자. 방 이름 또는 역할(`Leader`, `QA`) |
| verify | enum/null | QA 확인 방법 `auto` `agent` `user`. QA 일은 필수 |
| status | enum | `todo` `doing` `review` `waiting_user` `done` `hold` `cancelled`. 화면 이름은 차례대로 대기, 진행 중, 검토 요망, 확인 요망, 완료, 보류, 취소(11-23 확정, 키는 그대로) |
| start, due | date/null | `YYYY-MM-DD`. done이면 due=실제 끝난 날 |
| deps | [키] | 선행 일(이게 끝나야 시작). 반대 방향(막는 일)은 자동 계산 |
| relates | [키] | 관련 일, 이슈 |
| decisions | [str] | `docs/decisions-*.md` 번호, 범위는 `"1-44"` |
| commits | [str] | 짧은 해시 7자리 |
| refs | [str] | 관련 문서 경로 |
| screens | [str] | 화면 ID(`naming.md`), 예 `S4`, `D0` |
| figma | [str] | 피그마 노드 id, 예 `765:13473` |
| desc | str/null | 왜, 무엇, 범위. 여러 줄 가능 |
| acceptance | [str] | 완료 조건. 검토자가 이걸 보고 판단 |
| checklist | [{text, done}] | 하위 할 일 (`add-item`, `check-item`) |
| comments | [{at, by, text}] | 댓글 (`comment`, 반려 사유 자동) |
| milestone | bool | 마일스톤이면 true, start=due |

## issues.jsonl 필드
| 키 | 타입 | 뜻 |
|---|---|---|
| id | str | `ISS-007` |
| title | str | 증상 한 줄 |
| area | enum/null | 분야 |
| status | enum | `open` `doing` `reopened` `resolved` `closed` |
| severity | enum | `high`(알람 실패 가능, 출시 막음) `medium` `low` |
| owner, reporter, reviewer | str/null | 고칠 방, 찾은 사람, 확인할 방(기본 QA) |
| task | 키/null | 관련 일 |
| found, resolved | date/null | 처음 발견일, 해결일 |
| reopen_count | int | 재발생 횟수 |
| desc, repro, expected, actual | str/null | 설명, 재현 방법, 기대 결과, 실제 결과 |
| cause, fix | str/null | 원인, 해결 방법 |
| relates, commits, refs, screens, comments | list | 일과 같음 |

모르는 칸은 비워 둬요. 지어내지 않아요.

## events.jsonl
`{"at": "2026-11-23 13:29:37", "by": "[QA 1]", "id": "TSK-091", "action": "reject", "changes": {"status": ["review", "doing"], "comments": [null, ["반려: ..."]]}}`
action: create, set, claim, submit, approve, reject, resolve, reopen, comment, add-item, check-item. 한꺼번에 옮긴 일은 `id: "*"`, `action: migrate/import` + `text`.

## 일정 기준 (11-23 Leader 4주안, 1일째 = 2026-11-23)
| 일째 | 날짜 | 내용 |
|---|---|---|
| 1~4 | 11-23~11-26 | 결정 136~143 개발, adb 체크리스트, 스토어 기본 준비물, 서명된 빌드 |
| 5 | 11-27 | 마일스톤 5.5 비공개 테스트 시작(14일) |
| 5~18 | 11-27~12-10 | 나머지 체크리스트, 문제 검수, 실제 알람음, 앱 이름, 스크린샷, 권한 신고 |
| 19 | 12-11 | 마일스톤 5.7 프로덕션 신청 |
| 20~28 | 12-12~12-20 | 마일스톤 5.10 출시(due는 늦은 쪽 12-20). 전원 종료 방지 거절 시 5.9 |

자세한 흐름: `docs/idea4-release-workflow.md`.

## 바뀐 기록
- 11-23 [기획 4]: WBS 번호 키 → `TSK-` 고정 키(날짜순), `ISSUE-` → `ISS-`, note → desc, review 상태, 검토 명령, 잠금, events.jsonl, QA 묶음 7, 지난 이슈 34개.
- 11-23 [기획 4]: QA 묶음 채움(일 23, 품질 관문 2), `verify` 필드, 체크리스트 72줄을 QA 일로 옮김(옛 TSK-090, 102, 103, 110 취소), 결정 136~143 개발 일 검토자 `[QA 1]`과 짝 검증 일, 열린 이슈 검토자 QA.

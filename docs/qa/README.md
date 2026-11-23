# 알람 시험 도구 (adb 명령과 흐름 로그)

11-23 TSK-164. 앱 안 '테스트' 페이지를 없애고, 같은 기능을 **디버그 빌드에서만** adb 명령으로 불러요. 출시 빌드에는 이 입구가 없어요(`app/src/debug`에만 있음). 시험 경우 A~G의 뜻과 확인할 것은 `docs/test-alarm-cases.md`, QA 기준은 `docs/pm/qa-guide.md`를 봐요.

## 먼저 알아 둘 것

- 대상 앱은 디버그 빌드 `com.lumian.morningup.debug`예요.
- 시험 알람은 **사용자의 알람 목록에 새 알람으로 들어가요.** 사용자가 시키지 않으면 걸지 않아요. 시험이 끝나면 홈에서 지워요.
- 폰 작업 규칙(회전 값 전후 확인, `am start`로 실행, `install --user 0`)은 메모리와 프로젝트 CLAUDE.md를 따라요.
- 폰이 여러 대 붙어 있으면 `adb -s <기기>`를 붙여요. 기기 이름은 `adb devices -l`로 봐요.

## 명령

입구는 `com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver` 하나예요. adb(shell)만 보낼 수 있어요(`DUMP` 권한으로 막음). 결과는 `am broadcast` 출력의 `data="..."`에 나와요.

| 할 일 | 명령 |
|---|---|
| 시험 알람 A~G 걸기 (다음 분 정각) | `adb shell am broadcast -n com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver -a com.lumian.morningup.debug.ALARM_CASE --es case A` |
| N분 뒤에 걸기 (1~60) | 위 명령 끝에 `--ei in_minutes 5` |
| 배터리 전날 밤 알림 바로 보내기 | `adb shell am broadcast -n com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver -a com.lumian.morningup.debug.BATTERY_REMINDER` |

- `case`는 `A`~`G`예요. 틀리면 `data="case must be one of A,B,..."`가 나와요.
- 성공하면 `data="case A alarm 12 at 16:31"`처럼 알람 id와 울릴 시각이 나와요.
- 배터리 알림은 알림 권한이 꺼져 있으면 `blocked`가 나와요.

| 경우 | 설정 요약 |
|---|---|
| A | 전부 기본값(미션 없음, 다시 울림 꺼짐) |
| B | 다시 울림 3분 간격 3번 |
| C | 미션(Drop Words 3개)과 전원 종료 방지 |
| D | 미션(Quick Math 1문제)과 다시 잠들기 방지(1분 뒤, 답할 시간 30초) |
| E | 진동 끔과 부드럽게 깨우기 30초 |
| F | 알람음 끔(진동만) |
| G | 다시 울림 3분 간격, 끌 때까지 |

## 흐름 로그

울림 흐름의 사건은 logcat 태그 `MorningFlow` 하나에 한 줄씩 남아요(디버그 빌드만).

```
adb logcat -c
adb logcat -s MorningFlow:I > flow.log
```

한 줄 모양: `at=16:31:00.012 alarm=12 event=fired phase=- ->Ringing effects=StartSound,ShowRingScreen,...`

| event | 뜻 |
|---|---|
| `debug_case_scheduled` | adb로 시험 알람을 걸었어요 |
| `debug_battery_reminder` | adb로 배터리 알림을 보냈어요 |
| `fired` | 알람 시각이 됐어요(울림 시작, 이미 울리는 중이면 지나감) |
| `timer_ringtimeout` | 2분 울림이 끝났어요(쉬기 또는 놓침) |
| `timer_restended` | 쉬는 시간이 끝나 다시 울려요 |
| `mission_started` | D0 '미션 시작'을 눌렀어요 |
| `timer_missiontimeout` | 미션 중 59초 동안 손을 놓아 다시 울려요 |
| `mission_completed` | 미션을 모두 마쳤어요 |
| `dismiss_without_mission` | 미션 없는 알람의 '알람 끄기' |
| `wake_up_closed` | W1 '확인'을 눌러 닫았어요 |
| `timer_wakecheckask` | W2 '깨어 있나요'를 띄울 때예요 |
| `wake_check_confirmed` | W2 '깼어요'를 눌렀어요 |
| `timer_wakechecktimeout` | W2에 답하지 않아 처음부터 다시 울려요 |
| `resumed` | 앱이나 서비스가 다시 살아나 울림을 이어가요 |
| `missed_after_long_off` | 폰이 오래 꺼져 있어 이어가지 않고 놓친 알람으로 끝냈어요 |

- `phase=앞->뒤`는 울림 단계가 어떻게 바뀌었는지예요(`Ringing`, `Resting`, `InMission`, `AwaitingWakeCheck`, `WakeCheckAsking`, `-`는 울림 없음).
- `effects=`는 그 사건으로 앱이 한 일이에요(소리 시작, 화면 띄우기, 타이머 걸기, `SessionEnded` 등).
- 미션 화면 조작(59초 다시 채우기)은 너무 잦아서 남기지 않아요.
- 소리 크기, 진동, 화면 꺼짐 같은 자세한 기록은 따로 있는 진단 기록(`tools/read-diag.py`)을 봐요.

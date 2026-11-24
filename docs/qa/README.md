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
| 깼는지 묻기 전체 설정 바꾸기 (TSK-088) | `adb shell am broadcast -n com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver -a com.lumian.morningup.debug.WAKE_CHECK --ei ask_minutes 1 --ei answer_seconds 30 --ez recheck true --ei recheck_minutes 1` (값은 빼도 되고, 넣은 것만 바뀌어요) |
| 실험실 '확실히 잠깨기' 바꾸기 (TSK-172) | `adb shell am broadcast -n com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver -a com.lumian.morningup.debug.LAB --ez trick true --es level strong --es speed fast --ez multi true --ei count 3 --ei minute_seconds 6` (넣은 값만 바뀌어요. `level`은 weak, normal, strong, `speed`는 slow, normal, fast, `count`는 3~5. `minute_seconds`는 여러 번 묻기 간격과 1시간 상한에서 1분을 몇 초로 셀지예요(1~60, 60이면 실제 시간). 결과는 `data="lab ..."`) |

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
| `debug_lab` | adb로 실험실 값을 바꿨어요 |
| `lab_trick_on`, `lab_trick_off` * | 설정 T3에서 버튼 속이기를 켜거나 껐어요(`level=`, `speed=`) |
| `lab_recheck_on`, `lab_recheck_off` * | 설정 T3에서 여러 번 묻기를 켜거나 껐어요(`count=`) |
| `trick_shown` | 버튼 속이기를 건 울림 화면이 떴어요(`level=`, `speed=`) |
| `trick_auto_off` * | 접근성 때문에 이번 울림은 버튼 속이기를 껐어요(`reason=talkback`, `a11y_service`, `no_animation`) |
| `trick_start` | 화면을 처음 만졌어요. 여기서부터 20초예요 |
| `trick_fake_tap` | 가짜 버튼을 눌렀어요(`n=` 몇 번째) |
| `trick_end` * | 진짜 버튼을 눌렀어요(`seconds=` 첫 터치부터, `fakes=` 가짜 누른 수, `expired=` 20초가 지나 멈춘 뒤인지) |
| `recheck_ask` | 여러 번 묻기 다음 확인을 예약했어요(몇 번째/전체, 고른 간격 분) |
| `recheck_pass`, `recheck_miss` * | 여러 번 묻기 확인을 통과했거나 놓쳤어요(몇 번째/전체) |
| `recheck_cap_60` * | 미션 끝에서 1시간을 넘겨 남은 확인을 없앴어요(남은 수) |

- `phase=앞->뒤`는 울림 단계가 어떻게 바뀌었는지예요(`Ringing`, `Resting`, `InMission`, `AwaitingWakeCheck`, `WakeCheckAsking`, `-`는 울림 없음).
- `effects=`는 그 사건으로 앱이 한 일이에요(소리 시작, 화면 띄우기, 타이머 걸기, `SessionEnded` 등).
- 미션 화면 조작(59초 다시 채우기)은 너무 잦아서 남기지 않아요.
- 소리 크기, 진동, 화면 꺼짐 같은 자세한 기록은 따로 있는 진단 기록(`tools/read-diag.py`)을 봐요.
- `*` 표시 사건은 출시 빌드에서도 폰 안 진단 기록에 남아요(결정 146 4절). 밖으로는 보내지 않아요.

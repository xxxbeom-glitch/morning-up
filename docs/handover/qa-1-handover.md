# [QA 1] → [QA 2] 넘김 메모 (11-25 15시경)

## 끝난 일
- Z플립5 루프 1~3바퀴(023ebf4, 5cdfa67, a3d8b1a, fec4e23): 결과 `docs/exports/qa-flip5/README.md`
- 승인: TSK-187, 184, 182, 172, 190, 192, 194. 이슈 ISS-043~048 모두 resolved
- S24+ TSK-192, TSK-194 결과: `docs/exports/qa-s24/README.md`

## 지금 맡은 일
- TSK-197(개발 18, 커밋 662d21f, 결정 157): 홈 위 다음 알람 문구/날짜 줄을 누르면 그 알람 편집(S0)이 열림. 확인 5가지:
  1) 켜진 알람이 있을 때 누르면 '다음에 울릴 알람'(쉬는 알람이 먼저면 그것) 편집이 열림
  2) 켜진 알람이 없으면 눌러도 아무 일 없음
  3) 선택 모드에서는 문구가 안 보여 못 누름
  4) TalkBack에서 '다음 알람 편집'으로 읽힘 (TalkBack 켜기는 설정 변경이라 사용자 몫으로 넘기거나 uiautomator로 content-desc 확인)
  5) 위 '한 번 더 확인' 띠는 따로 눌림
- 묶어서 시험할 단위(같은 APK): 5e810a5 결정 159(쉬는 알람 시각, 요일, 공휴일 쉬기 바꿔 저장하면 쉬기 풀림), 674c7fb 결정 160(선택 삭제 확인 창 없이 스낵바, 되돌리기, 남은 확인 토스트), f058256 결정 161(바 '복제', 쉬기 날짜와 남은 확인은 복사 안 됨). 158은 사용자 확인 뒤.
- 폰: S24+ 무선 디버깅은 사용자 몫(Leader가 안내). Z플립5는 [리서치 4]가 다 쓰면 직접 알려 줌. 먼저 되는 폰으로.

## 다음 맡은 일 (11-25 16시대, Leader 지시)
- TSK-197, 198, 200은 승인 끝.
- [개발 19]가 TSK-204(새 패키지 com.lumian.wakeup, 이름 '일어나') + TSK-202, 203(결정 163~168) 판을 알려 주면 그 판 하나로 Z플립5에서 소리 없이 확인:
  1) TSK-204 새 앱 user 0 설치, 서랍 이름과 아이콘. 옛 앱 com.lumian.morningup.debug는 지우지 않음
  2) 결정 163 새 알람 다시 울림 처음 값 켬(5분 3회), 못 들은 척 두면 쉬었다 다시 울림
  3) 결정 165 울리는/쉬는/미션 중 홈 토글, 선택 끄기, 이번만 쉬기, 삭제, 모두 끄기, 편집 저장 막힘 + 토스트. 쉬는 중 홈 '다시 울려요' 띠 → 울림 화면
  4) 결정 166 앞 알람 쉬는 중 뒤 알람 시각 → 뒤 알람 울리고 앞 알람 남은 울림 끝. 울리는/미션 중이면 뒤 알람 지나감
  5) 결정 168 W2 기다리는 동안 홈 띠 '곧 깼는지 물어볼게요', 눌러도 W2 미리 안 열림
  6) 2-1 여러 번 묻기 1시간 상한 끝 W3 '오늘 확인을 다 마쳤어요'(로그나 단위 시험으로 대신 가능)
  7) 결정 164, 167 화면 글(T1, T3 안내, O2-1, H2 배너)
  8) TSK-205 Drop Words가 최근 3일 단어를 빼고 냄(날짜 바꾸기 어려우면 단위 시험과 로그로 확인)
  + 기존 흐름 회귀(울림, 미션, W1~W3). 결함은 add-issue, 끝나면 approve/reject, Leader에 결과 표
- 새 패키지 정보([개발 19], 3444bb7 16:25:20 빌드는 TSK-204만): com.lumian.wakeup.debug, 디버그 입구 `-n com.lumian.wakeup.debug/com.lumian.morningup.debug.DebugCommandReceiver`(action 그대로), 실행 액티비티도 새 패키지로 바뀌었을 수 있음(`cmd package resolve-activity --brief com.lumian.wakeup.debug`로 확인). docs/qa/README.md, tools/read-diag.py 새 주소. 202/203까지 든 판을 기다렸다가 한 번에 시험.
- 주의: 설치 전 APK 빌드 시각이 개발 방이 말한 시각 이후인지 꼭 확인(11-25에 한 번 옛 판을 깔아 헛시험함). 새 패키지면 디버그 명령 패키지 이름도 바뀌었는지 확인.

## 기기 규칙
- Z플립5(`192.168.35.211:42123`)는 지금 [리서치 4]가 씀. Leader가 알려 줄 때까지 쓰지 않음. 쓸 때는 시험 알람을 `--ez silent true`로(소리 없이), 흐름은 로그로. 처음 값: 알람 소리 11, 회전 0/0. 사용자 알람 오전 7:00(월~금, 꺼짐)은 그대로.
- S24+(`adb-R3CX108LP4R-1HcszY._adb-tls-connect._tcp`): 처음 값 회전 0/0, 알람 소리 7. 시험 중 소리 1, 끝나면 7. 사용자 6:30 알람은 손대지 않음. 다른 방이 만든 알람(11:52, 3:06, 3:18, 3:23, 4:03, 5:29)도 손대지 않음. 화면이 빨리 꺼지니 작업 중 30초마다 KEYCODE_WAKEUP(최대 15분), 잠금은 밀어 올리면 풀림.
- 설치 `install -r --user 0`, 실행 `am start -n com.lumian.morningup.debug/com.lumian.morningup.MainActivity`(monkey 금지). Git Bash에서는 `export MSYS_NO_PATHCONV=1`.
- 디버그 명령: `am broadcast -n com.lumian.morningup.debug/com.lumian.morningup.debug.DebugCommandReceiver -a com.lumian.morningup.debug.ALARM_CASE --es case A --ei in_minutes N [--ez silent true]` (in_minutes 최대 60, 한 번만 울리는 알람). 반복 알람은 알람 편집에서 요일을 눌러 저장(아래로 밀면 '저장' 버튼). 지울 때는 홈 ⋮ → 선택 → 고르기 → 삭제(674c7fb부터 확인 창 없이 바로 지워지고 스낵바 '되돌리기'가 뜸. 되돌리기를 누르지 말 것).
- 흐름 로그 `logcat -s MorningFlow:I AndroidRuntime:E`, 진단 기록 `ANDROID_SERIAL=<기기> python tools/read-diag.py 40 <알람id>`.
- 커밋 안 함. 관리판은 `python tools/pm/pm.py ... --as "[QA 2]"`.

## 참고(이슈 아님)
- 울린 지 1분 안에 이번만 쉬기를 누르면 카드가 '23시간 59분 뒤' 대신 '목요일에'로 나옴(초 올림 추정, TSK-192 comment).
- 같은 분에 알람 두 개면 하나만 울리고 나머지는 MarkPassed.

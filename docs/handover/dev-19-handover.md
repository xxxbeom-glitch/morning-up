# [개발 19] 넘김 메모 (11-25)

다음 개발 방([개발 20])이 이어받을 것들이에요. 일 목록은 `python tools/pm/pm.py mine --as "[개발 20]"`로 다시 확인하고, owner가 [개발 19]인 열린 일은 `pm.py set 키 --as "[개발 20]" owner="[개발 20]"`로 바꿔요. 앞 방 메모 `dev-16-handover.md`~`dev-18-handover.md`의 '주의할 점'도 그대로 유효해요.

## 이 방에서 한 일 (모두 `dev/overnight-11-17`에 푸시함)

| 키 | 커밋 | 내용 | 상태 |
|---|---|---|---|
| TSK-200 | d367393 | 앱 아이콘을 피그마 '앱로고_일어나ver2'로 교체. 손글씨 8획 stroke 벡터 `ic_launcher_app_foreground.xml` | QA 승인 |
| TSK-204 | 3444bb7 | 표시 이름 '일어나', applicationId `com.lumian.wakeup`(결정 169). 코드 폴더 `com.lumian.morningup`은 그대로 | QA 확인 대기 |
| TSK-202 | e7c10e8 | 결정 165(울리는 알람 카드 막기, 쉬는 중 띠), 166(쉬는 중이면 뒤 알람 울림), 168(W2 대기 띠), 163(다시 울림 처음 값 3회), W3 1시간 끝 글 | QA 확인 대기 |
| TSK-203 | 122f288 | ADR과 결정 146, 149의 옛 값(60초, 미션 중 20%)을 코드(2분, 소리 끔)에 맞춤. 문서만 | QA 확인 대기 |
| TSK-205 | 8cee221 | Drop Words 최근 사흘 단어 빼기. `RecentDropWords`(core/model), `PrefsDropWordHistory`(기기 보호 저장소 `drop_word_history`) | QA 확인 대기 |
| TSK-206 | 4e32f48 | 스플래시 손글씨(결정 170). `SplashHandwriting.kt`, 약 3초 + 0.5초 멈춤, 움직임 줄이기면 완성 글자. 미리보기 GIF `docs/exports/tsk-206/` | Leader 검토 대기 |
| TSK-207 | 4e3270a | 결정 164(T1 카드 이름 '다시 잠들기 방지', 첫 줄 '묻는 때', 켠 알람 없으면 T1, T3 안내), 167(O2-1, H2-1, H2-2 설명에 비상구 이유) | QA 확인 대기 |

- 마지막 APK: `app/build/outputs/apk/debug/app-debug.apk`, 빌드 시각 11-25 17:12:14(4e3270a, 206, 207 포함). [QA 1]에게 바뀐 점과 함께 넘겼어요.
- [QA 1]은 11-26에 Z플립5에서 소리 없이 TSK-204, 202, 205, 206, 207을 한 번에 봐요. 반려되면 그 고침을 [개발 20]이 이어받아요.

## 남은 일
- TSK-206 빠르기 조정: 사용자가 GIF나 폰 녹화를 보고 정하면 `SplashHandwriting.kt`의 `DRAW_MILLIS`, `HOLD_MILLIS`, `PACE`, `PAUSES`만 바꿔요. 바꾼 뒤 `python docs/exports/tsk-206/make_preview.py .`로 GIF를 다시 만들어요. 실제 폰 녹화는 아직 없어요(설치는 Leader, QA 몫).
- TSK-094(기기 호환성 대책 4개), TSK-178(소리 녹음 확인 기능): Leader 지시가 오면 시작해요.

## 주의할 점 (덧붙임)
- 결정 154('울리는 중 끈 알람 이번만 쉬기')는 결정 165로 울리는 중 끄기가 막혀서, 이제 W2, W3 대기 중에 끈 경우에만 해당돼요.
- core/model은 JVM 모듈이라 시험은 `:core:model:test`예요(`testDebugUnitTest`에 안 들어가요). 전체 확인: `./gradlew assembleDebug assembleRelease assembleOnboarding assembleMission testDebugUnitTest :core:model:test detekt`.
- `feature/settings`의 시험 파일과 LabPage.kt, strings.xml 등은 CRLF예요. 파이썬으로 고칠 때 `\r\n`을 `\n`으로 바꿔 고친 뒤 되돌리면 편해요.
- Bash heredoc 안에서 `\\`가 `\`로 줄어드는 일이 있었어요. strings.xml의 `\'` 같은 글은 Write로 만든 스크립트에서 `chr(92)`로 넣는 게 안전해요.

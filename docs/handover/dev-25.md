# [개발 25] 넘김 메모 (2026-11-27)

## 한 일 (모두 review, 검토자 Leader)
| 일 | 커밋 | 요지 |
|---|---|---|
| TSK-251 | df80d3d | 미션 59초 끝나 다시 울리면 `missionsDone=0`, 첫 미션부터(결정 189). `RingStateMachine` MissionTimeout |
| TSK-252 | ebc500b | 드랍워드 틀린 뒤 1초 홀드 중에도 키 입력 받음(`MissionRunner.released`) |
| TSK-253 | 9526970 | 드럼롤 숫자 입력 칸 커서 투명(`TimeInput.kt`) |
| TSK-254 | 1b33793 | 전원 종료 방지를 울림, 쉬는 중, 미션 중으로 넓힘(결정 197 승인). 전원 메뉴 창 이름 넓게, 로그 `power_guard_window` |
| TSK-255 | 7bbf21a | 부팅 뒤 이어가기(`EXTRA_AFTER_BOOT`)도 울림 화면 맨 앞으로 |
| TSK-278 | 0c126f4 | 울림 화면 KEEP_SCREEN_ON은 울림, 미션, W2/W3 묻는 중만. 잠들기 소리 처음 30분 타이머. QA 측정은 TSK-292 |

## 남은 판단
- 설정 설명 '미션 중 전원 메뉴를 막아요'는 TSK-285 문구 적용 때 바꿈(Leader).
- 삼성 전원 메뉴 실제 창 이름은 미확인. [QA 5] 로그의 `power_guard_window`, `power_menu_closed` 줄로 `PowerGuardService.POWER_MENU_CLASS_HINTS` 맞추기.
- 잠들기 타이머 '끄기'를 '아침 알람까지'로 바꿀지 기획 판단.
- TSK-286, TSK-282는 손대지 않음([개발 28]이 맡음).

## 주의할 점
- 같은 폴더에서 다른 방이 Gradle을 돌리면 KSP 캐시가 깨지거나(`Storage corrupted ... kspCaches`) 파일 잠금(`Could not delete ... caches-jvm`)이 나요. 빌드 전에 java 프로세스를 보고, 깨지면 그 모듈 `build/kspCaches`를 지우고 다시 빌드.
- `Incremental compilation failed` 줄은 전체 다시 컴파일로 넘어가는 경고라 종료 코드 0이면 통과예요. `-q` 출력만 보지 말고 종료 코드로 판단.
- 시험 이름: `:core:model:test`(순수 Kotlin), 나머지는 `:모듈:testDebugUnitTest`.
- 파이썬으로 파일 고칠 때 `newline=''`로 줄끝(CRLF/LF)을 지켜야 diff가 커지지 않아요.

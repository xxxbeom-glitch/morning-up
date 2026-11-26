# [개발 21] 넘김 메모 (11-26)

다음 방: `[개발 22]`. 시작하면 `python tools/pm/pm.py set TSK-094 owner=[개발 22] --as "[개발 22]"`부터 해요.

## 끝난 것
| 일 | 상태 | 커밋 |
|---|---|---|
| TSK-217 이번만 쉬기 되돌리기와 쉬는 날 토스트 | review | 60f88fe |
| TSK-218 W3 대답 1분, 기다리는 시간 1/3/5분, 묻는 횟수 1/2/3번, 실험실 여러 번 묻기 없앰 | review([QA 1]) | b224a37 |
| TSK-094 1단위: 알람 볼륨 0이면 울리는 동안 최대의 30%로 올리고 되돌림(알람 소리, W2/W3 알림음) | doing | 1c4bf94, 2888e0e |
| TSK-094 2단위: 방해 금지 알람 막힘, 배터리 '제한' 검사(앱 열 때 refresh), 홈 배너 순서, '그 밖에 n개 더', H2-6/7/8 글 | doing | 1009bd7 |

## 2단위에서 만든 것
- `core/model`
  - `AlarmSilence.blocks(DndMode, alarmsAllowed)`를 만들었어요.
  - `PermissionReport`에 `backgroundRestricted`, `alarmsSilenced` 필드를 더했어요.
  - `PermissionPrompt.kt`에 `banners`(순서 목록)와 `bannerMore`를 더했어요. `PermissionBanner`에는 BatteryRestricted, AlarmsSilenced, ExactAlarm을 더했고, 이 셋은 `startStep`이 null이에요.
- `core/alarm/AndroidPermissionChecker`: 두 상태를 읽고, 새로 생기면 진단 기록 `battery_restricted`, `dnd_blocks_alarms`를 남겨요.
- `feature/settings/PermissionBanner.kt`
  - `more` 인자와 `openBannerTarget(context, banner)`를 더했어요.
  - 방해 금지 배너는 지금 `android.settings.ZEN_MODE_SETTINGS`를 열어요. T2-1이 생기면 그 화면으로 바꿔요.
- `app/.../MorningNavHost.kt`: 시트가 없는 배너는 `openBannerTarget`으로 열어요.

## 남은 TSK-094 (하나씩)
1. 알람 저장 때 방해 금지 경고 시트 S0-3: '괜찮아요', '설정 열기'를 누르면 T2-1로 가요. 같은 자리에 S0-2 알림+다른 앱 위에 표시 경고('그래도 저장', '권한 켜기', TSK-177 답 1)도 있어요.
2. 울리기 10분 전 검사, 울린 뒤 기록, 다음에 열 때 한 번 알림(R2).
3. 밤 9시 '내일 알람 점검': `BatteryReminderPolicy`와 `core/alarm/BatteryReminder`를 넓혀요.
   - 볼 것: 방해 금지, 배터리 예외, '제한', 배터리 20% 아래(충전 중 아님).
   - 푸시 하나에 모아요. 제목은 첫 문제, 끝에 '그 밖에 n개 더'를 붙여요(시안 N1). 문제가 없으면 푸시를 보내지 않아요.
4. 강제 종료 흔적(B-03, 시안 N2)
   - 알림 '앱이 강제로 닫혀 알람이 꺼져 있었어요 / 지금 다시 맞췄어요'를 한 번 띄워요.
   - 놓친 알람 문구는 '…앱이 닫혀 있는 동안 지나갔어요'로 나눠요(`AndroidCatchUpHandler`).
   - 알아채는 방법: ApplicationExitInfo(API 30 이상)를 써요. minSdk가 26이라 그 아래는 대안이 필요해요.
5. 안드로이드 12에서만 T2 '정확한 알람' 줄을 보여요(TSK-177 답 8). 배너는 이미 12에서만 나와요(13 이상은 늘 허용).
6. 울리는 동안 방해 금지를 '알람만 허용'으로 잠깐 바꾸기(R3)
   - 사용자가 켰을 때만 해요. 처음 값은 꺼짐이고, 방해 금지 접근 권한이 필요해요.
   - 끈 뒤 원래대로 돌려요.
7. T2-1 '방해 금지 중에도 울리게' 화면을 만들어요.
   - 시안: 라이트 1154:15798, 다크 1155:15842, 캡처 `docs/exports/tsk-220/T2-1_light.png`.
   - TSK-214 결과대로 '알람' 켜기 한 단계를 크게 보여 줘요.

근거: `docs/decisions-11-26.md` TSK-177 답 1~8, `docs/permission-matrix.md` 3~6절. 시안은 TSK-220 댓글의 노드 id와 `docs/exports/tsk-220/`. 결정 178: UX 흐름은 알라미를 따르고, 모양과 문구는 우리 것이에요.

## 주의
- 확인 명령: `./gradlew assembleDebug testDebugUnitTest :core:model:test detekt`
  - 로그에서 `BUILD SUCCESSFUL`을 확인한 뒤 커밋해요. 파이프 뒤 EXIT 값은 믿지 않아요.
- detekt가 자주 잡는 것: 한 줄 120자, ReturnCount 4, LargeClass(시험 파일 포함).
- 파일마다 줄바꿈이 CRLF와 LF로 섞여 있어요. 원래 줄바꿈을 지키며 고쳐요.
- Robolectric은 SDK 35예요. `consolidatedNotificationPolicy`가 null을 돌려줘서, 그때는 `notificationPolicy`로 대신 읽어요.
- W3를 묻는 동안 2분마다 다시 나는 알림음(`RingService.RECHECK_NUDGE_MILLIS`)은 대답 시간이 1분이 되면서 거의 울리지 않아요. Leader에게 알렸어요.

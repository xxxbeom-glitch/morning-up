# 플레이 콘솔 권한 신고 답변 초안 (TSK-084)

11-23 [기획 5] 작성. 근거: `core/alarm`, `app` 매니페스트, 결정 85, 134(전원 종료 방지 동의 시트), 140, 141(수면 라운지 제외, 재생 서비스 뺌, TSK-089), 142, 143, `idea4-release-workflow.md` 5단계, `idea6-device-compat.md` 2-5.
플레이 콘솔에 넣지 않았어요(사용자가 직접 넣어요). 심사자는 영어로 읽는 경우가 많아 콘솔에 넣을 글은 **영어 원문 + 한국어 뜻**으로 적었어요.
구글 정책 원문은 이번에 다시 확인하지 않았어요. 넣기 전에 콘솔 화면의 질문과 글자 수 제한을 한 번 맞춰 봐요.

## 결론

| 신고 | 필요한지 | 핵심 답 | 영상 |
|---|---|---|---|
| 정확한 알람 `USE_EXACT_ALARM` | 필요 | 앱의 핵심 기능이 알람 시계 | 없음(콘솔 질문만) |
| 전체 화면 알림 `USE_FULL_SCREEN_INTENT` | 필요(안드로이드 14 이상) | 알람 앱이라 잠금 화면 위에 울림 화면을 띄움 | 없음 |
| 포그라운드 서비스 `systemExempted` | 필요 | 알람이 울리는 동안 소리, 진동, 미션 화면을 유지 | 필요할 수 있음(아래 영상 1) |
| 포그라운드 서비스 `mediaPlayback` | **신고 안 함** | 출시판에서 선언을 뺐어요(TSK-089, `tools:node="remove"`) | |
| 접근성 API (전원 종료 방지) | 필요 | 접근성 도구가 아님, 미션 중 전원 메뉴만 닫음, 화면 내용 안 읽음 | **필요**(아래 영상 2) |
| 위치 | 신고 양식 없음 | 대략적인 위치, 앱을 쓰는 동안만. 백그라운드 위치 없음 | |
| 배터리 최적화 제외 요청 | 양식 없음, 정책 심사 대상 | 알람이 절전으로 늦지 않게 | |

## 1. 정확한 알람 (`USE_EXACT_ALARM`)

- 매니페스트: `USE_EXACT_ALARM`, 안드로이드 12 이하는 `SCHEDULE_EXACT_ALARM`(`maxSdkVersion 32`).
- 콘솔 질문 '앱의 핵심 기능': **알람 시계(Alarm clock)**.

설명 (영어):
> [App name] is an alarm clock app. Users set wake-up alarms, and the app must ring at the exact minute the user chose, even in Doze mode. Exact alarms are also used for the snooze-style re-ring after the alarm stops, and for the "Are you awake?" check a few minutes after the user dismisses the alarm.

한국어 뜻: 알람 시계 앱이에요. 사용자가 정한 분에 정확히 울려야 하고, 다시 울림과 '깨어 있나요?' 확인도 정확한 시각에 띄워야 해요.

## 2. 전체 화면 알림 (`USE_FULL_SCREEN_INTENT`)

- 콘솔 질문 '전체 화면 알림이 필요한 이유': **알람 시계 기능**.

설명 (영어):
> When an alarm goes off, the app shows a full-screen alarm screen over the lock screen so the user can see the time and start the wake-up mission or stop the alarm. The same screen is used for the "Are you awake?" check after dismissal. It is only shown for alarms the user set.

한국어 뜻: 알람이 울리면 잠금 화면 위에 울림 화면을 띄워 미션을 시작하거나 끌 수 있게 해요. 끈 뒤 '깨어 있나요?' 확인에도 같은 방식을 써요. 사용자가 정한 알람에만 띄워요.

## 3. 포그라운드 서비스 (`systemExempted`)

- 매니페스트: `RingService`, `foregroundServiceType="systemExempted"`, 권한 `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`. `USE_EXACT_ALARM`을 가진 알람 앱이라 쓸 수 있는 종류예요.
- 서비스가 떠 있는 때: 알람이 울리는 1분, 미션을 하는 동안, '깨어 있나요?'에 답하는 시간. 쉬는 중과 확인을 기다리는 동안에는 서비스를 멈추고 알림만 남겨요(`adr/0002` 6절).

설명 (영어):
> The foreground service runs only while an alarm is actively ringing, while the user is solving the wake-up mission, and while the "Are you awake?" check is on screen. It keeps the alarm sound, vibration and the mission timer alive so the alarm cannot be silently stopped by the system. It stops as soon as the alarm is dismissed or paused. A notification is always shown while it runs.

한국어 뜻: 알람이 울리는 동안, 미션을 하는 동안, '깨어 있나요?' 확인이 떠 있는 동안에만 돌아요. 소리, 진동, 미션 타이머가 시스템에 의해 끊기지 않게 해요. 끄거나 쉬면 바로 멈추고, 도는 동안 알림이 늘 보여요.

- 사용자에게 보이는 영향: 울리는 동안 알림 창에 '알람이 울리고 있어요' 알림. 결정 59의 기다림 알림은 서비스 없이 올려요.

## 4. 접근성 API (전원 종료 방지)

- 매니페스트: `PowerGuardService`, `BIND_ACCESSIBILITY_SERVICE`. 설정 xml: 이벤트 `typeWindowStateChanged`만, `canRetrieveWindowContent="false"`. 하는 일: 미션 중이고 바뀐 창이 전원 메뉴면 '뒤로'(`GLOBAL_ACTION_BACK`) 한 번. 그 밖의 때는 아무것도 안 해요.
- 앱 안 동의(눈에 띄는 공개): 설정에서 '전원 종료 방지'를 켤 때 권한이 없으면 바텀시트(결정 85, 134)가 먼저 떠요. 안내 글 "'설치된 앱'에서 morning-up을 켜 주세요. 화면 내용은 읽거나 저장하지 않아요.", 버튼 '나중에', '접근성 설정 열기'.
- 콘솔 질문 '접근성 도구인가요': **아니요**(`isAccessibilityTool` 선언 없음).
- 결정 143: 심사에서 거절되면 이 기능만 빼고 다시 내요. 개발은 스위치 하나로 서비스 선언과 화면을 뺄 수 있게 만들어 둬요.

설명 (영어):
> Optional "Power-off guard" feature. Some users turn the phone off to escape the wake-up mission and fall back asleep. When the user turns this feature on, the app uses the AccessibilityService API only to detect that the system power menu window has opened while a wake-up mission is in progress, and closes it with the Back action. The service only listens for window state changes, does not retrieve window content (canRetrieveWindowContent=false), does not read, store or send any screen content or user data, and does nothing outside an active mission. Before the user is sent to Accessibility settings, the app shows an in-app disclosure explaining what the service does, and the feature stays off unless the user enables it.

한국어 뜻: 선택 기능 '전원 종료 방지'예요. 미션을 피하려고 전원을 꺼 버리고 다시 자는 일을 막아요. 켜면 미션 중에 전원 메뉴 창이 열리는 것만 알아채 '뒤로'로 닫아요. 창이 바뀌었다는 신호만 받고 화면 내용은 읽지도 저장하지도 보내지도 않아요. 미션 밖에서는 아무것도 안 해요. 접근성 설정으로 보내기 전에 앱 안에서 먼저 설명하고, 사용자가 켜지 않으면 꺼져 있어요.

## 5. 위치

- 매니페스트: `ACCESS_COARSE_LOCATION`만. `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` 없음. 위치는 앱을 쓰는 동안 날씨를 새로 받을 때만 읽어요.
- 백그라운드 위치 신고 양식은 필요 없어요. 데이터 보안 양식(TSK-092)과 개인정보처리방침 2번에 적었어요.
- 국내 위치정보법 신고 여부는 별개예요. 출시 전 위치정보지원센터 확인이 남아 있어요(`research/H-location-law.md`).

## 6. 그 밖에 심사에서 물을 수 있는 권한

| 권한 | 신고 양식 | 메모 |
|---|---|---|
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 없음 | 구글 정책상 핵심 기능이 이 예외 없이는 안 되는 앱만 써야 해요. 알람이 절전으로 늦거나 안 울리는 것을 막는 용도라고 스토어 설명과 앱 안 안내에 일관되게 적어 둬요. 거절되면 설정 화면으로 보내는 방식(`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`)으로 바꿀 수 있어요 |
| `SYSTEM_ALERT_WINDOW` (다른 앱 위에 표시) | 없음 | **쓰고 있어요, 빼지 않아요**(11-24 ISS-038 확인, 결함 아님). 사용 이유: 알람 울림 화면을 다른 앱 위로 띄우기 위해. 다시 울릴 때 다른 앱을 쓰고 있어도 울림 화면을 맨 앞에 띄워요(결정 73, `RingNotifications.openScreenNow`). 심사에서 물으면 쓸 글(영어): "Used only to bring the alarm ringing screen to the front over other apps when an alarm re-rings, so the user can see and stop it." 앱 안에서는 선택 권한이에요 |
| `ACCESS_NOTIFICATION_POLICY` | 없음 | 방해 금지 예외 줄과 결정 143 방해 금지 검사 |
| `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `INTERNET` | 없음 | 일반 권한 |

## 7. 찍어야 할 영상

| 영상 | 보여 줄 것 | 길이 | 올릴 곳 |
|---|---|---|---|
| 1. 포그라운드 서비스 (콘솔이 요구할 때만) | 알람 울림 → 알림 창의 울림 알림 → 미션 시작 → 미션 끝 → 서비스 알림이 사라짐 | 30초 안팎 | [영상 주소], 비공개(일부 공개) 유튜브 링크 |
| 2. 접근성 | 설정에서 '전원 종료 방지' 켜기 → 동의 바텀시트 → '접근성 설정 열기' → 켜기 → 알람 울림, 미션 시작 → 전원 버튼 길게 → 전원 메뉴가 바로 닫힘 → 미션 밖에서는 전원 메뉴가 그대로 열림 | 1분 안팎 | [영상 주소] |

- 영상은 시험 폰으로 찍어요. 폰 조용한 시간(밤 10시~아침 9시)에는 알람 시험을 하지 않아요.
- 영상에 개인 알림, 연락처, 계정이 보이지 않게 해요.

## 8. 넣기 전에 확인할 것

- 출시판 매니페스트를 실제로 빌드해 합쳐진 결과에서 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`과 `SleepPlaybackService`가 빠졌는지 봐요(TSK-089 검토와 같이).
- 앱 이름이 정해지면 `[App name]`을 바꿔요.

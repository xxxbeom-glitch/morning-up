# Q. 알람앱 표준 QA 목록과 가이드라인, 우리 QA 보강안 (TSK-298)

- 조사: [리서치 7], 2026-11-27. 코드와 `docs/pm/qa-guide.md`는 고치지 않고 제안만 해요. 폰은 건드리지 않았어요.
- 사용자 질문(11-27): 알람 중 전화, 배터리 0% 꺼짐도 QA하나? 다른 알람앱 개발사의 기본 QA 목록이 있나? 안드로이드, 삼성 가이드라인만 따르면 되나?

## 결론 5줄

1. **알람앱 전용 공식 QA 목록은 없어요.** 구글의 '핵심 앱 품질'은 모든 앱 공통 기준이고, 삼성 Galaxy Store 심사도 설치, 실행, 콘텐츠 위주예요. 알람이 제때 울리는지는 어느 가이드도 대신 시험해 주지 않아요.
2. **그래서 '가이드라인만 따르면 된다'는 아니에요.** 가이드라인은 바닥(스토어 통과, 권한 신고)이고, 알람앱의 진짜 위험(안 울림, 못 끔)은 우리가 엣지 케이스 목록으로 직접 막아야 해요. 알라미 같은 회사의 내부 QA 목록은 공개된 것이 없고, 흔한 결함은 버그 신고와 dontkillmyapp.com에서 모을 수 있어요.
3. **우리 목록(dev-plan 체크리스트 72줄)은 이미 70% 정도를 덮어요.** 잠자기 모드, 앱 대기, 재부팅, 시간대, 서머타임(단위 시험 있음), 업데이트, 무음과 방해 금지, 통화 중 알람, 플립 바깥 화면, 글자 크기가 들어 있어요.
4. **빠진 것은 9가지예요.** 울리는 중 전화가 오는 경우, 배터리 부족과 0% 꺼짐 뒤 켜기, 삼성 '절전 앱/딥 슬립 앱', 삼성 최대 절전, 저장 공간 부족, 언어 바꾸기, 다른 알람앱과 같은 시각, 보안 폴더와 듀얼 앱, 스토어 신고 3종(정확한 알람, 전체 화면 알림, 포그라운드 서비스 유형) 점검이에요.
5. **대부분 adb로 흉내 낼 수 있어요.** 전화 수신만 실폰에서 흉내가 안 돼서 에뮬레이터(Claude)와 실폰(사용자) 둘로 나눠요. 배터리 0%는 `dumpsys battery`로 낮은 배터리를, `reboot -p`로 꺼짐을 흉내 내고, 켜는 것만 사용자가 해요.

## 1. 공식 가이드 요약

| 기준 | 알람앱에 해당하는 것 | 우리 상태 | 출처 |
|---|---|---|---|
| 구글 핵심 앱 품질 (Core app quality) | 비정상 종료, ANR 없음, 상태 보존, 48dp 터치, 라이트와 다크, 최소 권한, 최신 SDK 대상, 배터리는 Power Profiler나 Battery Historian으로 진단. 알람 전용 항목은 없음 | qa-guide 1절에 이미 반영 | [Core app quality](https://developer.android.com/docs/quality-guidelines/core-app-quality), [2026-03-20 판](https://developer.android.com/docs/quality-guidelines/archive/core/core-app-quality-2026-03-20) |
| 정확한 알람 권한 | 알람 시계 앱은 `USE_EXACT_ALARM`(설치 때 자동, 사용자가 못 끔)을 쓸 수 있어요. 대신 Play 정책 대상이라 알람이 핵심 기능이어야 올릴 수 있어요. `SCHEDULE_EXACT_ALARM`은 사용자가 끌 수 있고, 끄면 예약된 정확한 알람이 모두 취소돼요 | 13+는 `USE_EXACT_ALARM`, 12 이하는 `SCHEDULE_EXACT_ALARM`(maxSdk 32). 맞게 됨. Play 신고는 TSK-154에서 | [안드로이드 14 정확한 알람](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms), [알람 예약](https://developer.android.com/training/scheduling/alarms) |
| 전체 화면 알림 `USE_FULL_SCREEN_INTENT` | 안드로이드 14부터 통화, 알람 앱만 자동 허용. Play Console에서 '핵심 기능' 신고를 해야 하고, 아니면 설치 때 권한이 빠져요. `canUseFullScreenIntent()`로 확인 | 선언함, 권한 화면에서 확인함. Play 신고는 TSK-154에 넣어야 함 | [FSI 제한(AOSP)](https://source.android.com/docs/core/permissions/fsi-limits), [Play 도움말: FGS와 FSI 요건](https://support.google.com/googleplay/android-developer/answer/13392821) |
| 포그라운드 서비스 유형 | 안드로이드 14부터 유형 선언 필수. 유형마다 권한과 용도가 정해져 있고 Play 앱 콘텐츠 페이지에 신고해요. 2026년 4월 예고로 신고 방식이 바뀜(8월 26일 시행) | 울림 `systemExempted`, 잠들기 소리 `mediaPlayback`. Play 신고 문구 준비 필요 | [FGS 유형](https://developer.android.com/develop/background-work/services/fgs/service-types), [유형 필수 변경](https://developer.android.com/about/versions/14/changes/fgs-types-required), [Play 정책 예고](https://support.google.com/googleplay/android-developer/answer/16965181) |
| 잠자기 모드와 앱 대기 버킷 | 버킷마다 알람 횟수 제한, 안드로이드 12의 '제한' 버킷이 가장 엄격. 시험은 `dumpsys deviceidle force-idle`, `am set-standby-bucket` | A1, A2 줄과 TSK-133에 있음 | [앱 대기 버킷](https://developer.android.com/topic/performance/appstandby), [Doze 시험](https://developer.android.com/training/monitoring-device-state/doze-standby), [전원 관리 표](https://developer.android.com/topic/performance/power/power-details) |
| 재부팅과 시간 변경 | 재부팅 뒤 다시 예약(BOOT_COMPLETED), 권한이 다시 켜지면 다시 예약 | `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, 권한 바뀜 모두 받음. TSK-255 재부팅 통과 | [알람 예약](https://developer.android.com/training/scheduling/alarms) |
| 소리 초점(audio focus) | 안드로이드 12부터 전화가 오면 시스템이 '미디어, 게임' 소리를 줄여요. 알람 용도(`USAGE_ALARM`)는 그 목록에 없어서 **전화가 와도 알람 소리가 저절로 줄지 않을 수 있어요**(문서에서 추론, 실기 확인 필요) | `CallMonitor`가 소리 장치 모드(통화, 인터넷 통화, 전화 울림)를 보고 줄임 | [소리 초점 관리](https://developer.android.com/media/optimize/audio-focus) |
| 삼성 Galaxy Store 심사 | 설치, 실행, 콘텐츠, 연령 등급, 지식재산권 위주. 대상 API 33 이상, 64비트. AAB를 범용 APK로 바꿔 배포. 알람 전용 항목 없음 | 지금은 Play 먼저. Galaxy Store는 나중 | [배포 가이드](https://developer.samsung.com/galaxy/distribute/distribution-guide), [셀프 체크리스트](https://developer.samsung.com/galaxy/distribute/self-check-list), [FAQ](https://developer.samsung.com/galaxy-games/faq.html) |
| 삼성 앱 관리(절전 앱) | '절전 앱'이 되면 Job, Alarm, 포그라운드 서비스가 막혀요. 오래 안 쓰면 '딥 슬립'(현재 16일, 바뀔 수 있음). 3일 안 쓴 앱은 백그라운드 시작이 막힐 수 있다는 보고도 있음 | 배터리 최적화 제외는 받음. 절전 앱 목록 시험은 없음 | [삼성 개발자: 앱 관리](https://developer.samsung.com/mobile/app-management.html), [dontkillmyapp 삼성](https://dontkillmyapp.com/samsung), [삼성 지원: 절전 앱](https://www.samsung.com/us/support/answer/ANS00088422/) |
| 삼성 Remote Test Lab | 웹으로 실제 갤럭시(폴더블 포함)를 빌려 설치, 캡처, adb 가능. 가입 무료, 하루 크레딧 제한 | 안 씀. 다른 기종(A 시리즈, 폴드) 확인에 쓸 만함 | [Remote Test Lab](https://developer.samsung.com/remote-test-lab) |

**정리:** 공식 문서는 '알람이 울리게 만드는 방법'과 '스토어에 올리는 조건'을 알려 줄 뿐, '어떤 상황을 시험하라'는 알람앱 목록은 주지 않아요. 아래 엣지 케이스 표가 그 빈자리를 채워요.

## 2. 엣지 케이스 표

- 우리 상태: **있음**(체크리스트나 일에 이미 있음), **일부**(있지만 빠진 조건이 있음), **없음**.
- 시험 방법: **auto**(단위 시험), **agent**(Claude가 adb로), **user**(사용자가 직접). adb 명령은 시험 뒤 반드시 처음 값으로 되돌려요.
- 우선순위: **P1** 알람이 안 울리거나 못 끌 수 있음, **P2** 틀린 동작이나 특정 기기, **P3** 보기와 편의.

### 2-1. 전화와 소리

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| 통화 중 알람 시각이 됨 | 진동과 작은 소리, 통화가 끝나면 원래대로 울림 (A 줄 '통화 중 알람') | 있음 (TSK-151 user) | user: 실제 통화. agent 보조: 에뮬레이터에서 `adb emu gsm call 01000000000` 받은 뒤 시험 알람 | P1 |
| **알람이 울리는 중 전화가 옴** | 전화 벨이 들리게 알람 소리가 줄거나 멈추고, 전화를 받으면 통화 중 규칙, 거절하거나 끝나면 다시 울림. 미션 시간(59초)이 통화 때문에 0이 되어 벌을 받지 않음. 울림 화면이 전화 화면을 덮지 않음 | **없음** (통화 중 알람만 있음) | agent: 에뮬레이터 `adb emu gsm call` → `gsm accept` → `gsm cancel`, 진단 기록과 `dumpsys audio`로 소리 줄임 확인. user: Z플립5에 다른 폰으로 실제 전화 1번 (실폰에선 adb로 전화 수신 흉내 불가) | P1 |
| 보이스톡 같은 인터넷 통화 중 | 통화 중과 같게 (`MODE_IN_COMMUNICATION`) | 일부 (코드에 있음, 시험 없음) | user: 카카오톡 보이스톡 1번. auto: `CallMonitor.isCallMode` 단위 시험 | P2 |
| 무음, 진동 모드 | 알람 소리 그대로 | 있음 (A 줄, TSK-136) | agent: 폰 소리 모드 바꾸기 뒤 되돌림 | P1 |
| 알람 볼륨 0, 미디어 볼륨 0 | S6 볼륨으로 들림 | 있음 (A 줄) | agent: `adb shell cmd media_session volume --stream 4 --set 0` 뒤 되돌림 | P1 |
| 유선, 블루투스 이어폰 연결 | 설정대로 이어폰 또는 스피커 | 있음 (TSK-287, 이어폰 4칸 user 남음) | user | P2 |
| 방해 금지, 갤럭시 모드와 루틴 | 알람 허용이면 소리 남. 막히면 미리 경고 | 있음 (TSK-214 끝, ISS-057 열림) | agent: `adb shell cmd notification set_dnd alarms` / `off` | P1 |

### 2-2. 전원과 배터리

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| **배터리 부족(15%, 5%) 중 알람** | 삼성 자동 절전이 켜져도 울림. 미션을 끝까지 할 수 있음 | **없음** | agent: `adb shell dumpsys battery unplug` → `dumpsys battery set level 4` → 시험 알람 → `dumpsys battery reset`. 앱은 가짜 값을 믿고, 시스템 절전 동작은 일부만 흉내 됨 | P2 |
| **울리는 중 배터리 0%로 꺼짐 → 충전하고 켬** | 켠 뒤 규칙대로(10분 안이면 다시 울림, 넘으면 놓친 알람 알림). 알람 목록이 그대로. 반복 알람은 다음 번이 예약됨 | **일부** (A 줄 '꺼진 동안 지난 알람'은 있음. 울리는 중 꺼짐은 TSK-255 재부팅으로 비슷하게 통과) | agent+user: 울리는 중 `adb shell reboot -p`(전원 끄기, 갑자기 꺼짐 흉내) → 사용자가 몇 분 뒤 켬 → Claude가 USB로 진단 기록 확인. 진짜 0%는 하지 않아요(배터리에 나쁘고 오래 걸림) | P1 |
| 꺼진 채 알람 시각이 한참 지남 | 놓친 알람 알림 한 번, 다음 반복 예약 | 있음 (A 줄, TSK-152 user) | agent: `reboot -p` 뒤 알람 시각 넘겨 켬 | P1 |
| 절전 모드 | 울림 | 있음 (A 줄, TSK-136) | agent: `adb shell settings put global low_power 1` 뒤 0 | P1 |
| **삼성 최대 절전 모드** | 허용 앱이 아니면 못 울릴 수 있음. 막히면 경고하거나 안내 | **없음** | user: 설정에서 최대 절전 켜고 시험 알람 (adb로 켜는 공개 명령 없음) | P2 |
| **삼성 '절전 앱', '딥 슬립 앱' 목록에 들어감** | 우리 앱이 들어가면 알람이 막힐 수 있음. 권한 화면이나 점검에서 알려 줌 | **없음** (배터리 최적화 제외만 확인) | user: 기기 관리 > 배터리 > 백그라운드 사용 제한에서 앱을 '딥 슬립 앱'에 넣고 시험 알람. agent: `am set-standby-bucket restricted`로 가까운 흉내(A 줄에 있음) | P1 |
| 2~3일, 16일 이상 안 열기 | 울림 | 일부 (2~3일 TSK-150 user. 16일 딥 슬립 기준은 없음) | user: 출시판 실사용 기간에 자연히 확인. agent: `am set-inactive true`로 흉내 | P2 |
| 밤새 배터리 소모 | 대기 중 소모 거의 0 | 있음 (TSK-292 B1~B5) | agent | P2 |

### 2-3. 시간

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| 시간대 변경(서울 ↔ 뉴욕) | 그 지역 시각으로 울림 | 있음 (A 줄, TSK-135) | agent: 자동 시간대 끄고 `adb shell cmd alarm set-timezone America/New_York` (안드로이드 12+ 명령, 폰에서 되는지 먼저 확인) 뒤 되돌림 | P1 |
| 수동 시계 변경(앞으로, 뒤로) | 지난 알람은 놓친 처리, 앞 알람은 맞게 | 있음 (A 줄) | agent: 자동 시간 끄고 `adb shell cmd alarm set-time <밀리초>` 뒤 자동으로 되돌림 | P1 |
| 서머타임(없는 시각, 두 번 있는 시각) | 없는 시각은 1시간 뒤로, 두 번은 앞의 것 한 번만 | 있음 (`NextRingCalculatorTest` dstGap, dstOverlap) | auto. 한국은 서머타임이 없어 실기 시험은 안 해도 됨 | P3 |
| 자정 넘김, 공휴일 | 맞게 | 있음 (B 줄, TSK-142) | auto | P1 |
| 2028년(공휴일 목록 끝) | 요일대로 울림 | 있음 (G 줄) | agent | P3 |

### 2-4. 앱과 시스템 상태

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| 재부팅, 잠금 풀기 전 | 울림 | 있음 (TSK-255 통과) | agent: **USB 연결이면 재부팅 뒤에도 adb가 붙어요**(무선 디버깅만 꺼짐). 재부팅 시험은 USB로 하면 돼요 | P1 |
| 최근 앱에서 밀기, 강제 종료 | 울림, 강제 종료면 다음 실행 때 다시 예약 | 있음 (A 줄, TSK-134) | agent: `am kill`, `am force-stop` | P1 |
| 미션 중 앱이 죽음 | 다시 울리고 하던 미션부터 | 있음 (A 줄) | agent | P1 |
| **알람 시각 직전, 직후에 앱 업데이트** | 업데이트 뒤에도 울림. 울리는 중 업데이트되면 다시 울림 | **일부** (업데이트 뒤 울림은 있음. 울리는 순간 업데이트는 없음) | agent: 시험 알람 울리는 중 `adb install -r --user 0` | P2 |
| 데이터베이스 옮김(옛 판 → 새 판) | 알람이 사라지지 않음 | 있음 (G 줄, TSK-258 v6→v7) | agent | P1 |
| **저장 공간 거의 꽉 참** | 알람 저장이 실패하면 알려 줌. 울림과 진단 기록이 앱을 죽이지 않음 | **없음** | agent: 에뮬레이터를 작은 저장 공간으로 만들어 채우기. 실폰은 하지 않음(사용자 파일 위험) | P3 |
| 다른 앱 쓰는 중(게임, 영상, 내비) | D0이 맨 앞(다른 앱 위 허용 시), 없으면 위 알림 | 있음 (A 줄) | agent | P1 |
| **다른 알람앱이 같은 시각에 울림** | 둘 다 울림. 우리 소리가 끊기거나 미션이 막히지 않음. 끈 뒤 우리 알람이 '지나감'으로 잘못 처리되지 않음 | **없음** | agent: Z플립5(지워도 되는 폰)에서 삼성 시계 알람을 같은 시각에 맞춤(`am start -a android.intent.action.SET_ALARM`), 사용자 허락 뒤 | P2 |
| 우리 알람 둘이 같은 시각 | 뒤 알람 '지나감' | 있음 (A 줄) | auto + agent | P2 |
| 정확한 알람 권한 꺼졌다 켜짐(12 이하) | 다시 예약 | 있음 (D 줄, 12 에뮬레이터) | agent | P1 |
| 구글 백업 복원 | 알람 따라오고 다시 예약 | 있음 (G 줄) | user | P2 |

### 2-5. 기기, 계정, 화면

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| 화면 잠금(PIN) 있음, 없음 | 둘 다 D0이 잠금 위에 뜸. 끈 뒤 잠금으로 돌아감 | 일부 (W1 뒤 잠금 화면 줄은 있음, 잠금 종류별은 없음) | user: 잠금 없음 1번, PIN 1번 | P2 |
| **보안 폴더에 앱을 하나 더 깔았음** | 각자 알람만 울림. 보안 폴더 쪽이 잠겨 있을 때 동작 확인 | **없음** | user (보안 폴더는 adb로 다루지 않음) | P3 |
| **듀얼 앱(user 95)에 복제가 생김** | 알람이 두 번 울리지 않음. 설치는 `--user 0` | 일부 (설치 규칙만 있음) | agent: `adb shell pm list packages --user 95 | grep lumian`으로 복제 확인, 있으면 같은 알람이 두 번 예약됐는지 `dumpsys alarm` | P3 |
| Z플립5 바깥 화면, 접고 펴기 | 바깥 화면에 보이고 미션 진행 유지 | 있음 (A, E 줄, `cmd device_state state 0` 시험함) | agent + user (TSK-151) | P1 |
| 다크 모드 | 앱 규칙대로(폰 설정 아닌 시각 따라) | 있음 | agent | P3 |
| 글자 크기 최대, 화면 크기 최대 | 시각과 키패드 그대로, 나머지 잘림 없음 | 있음 (E 줄, TSK-146) | agent: `settings put system font_scale 2.0` 뒤 1.0. 삼성 '화면 크게'도 한 번 | P2 |
| **언어 바꾸기(영어)** | 문구가 영어로, 잘림 없음, 알림 채널 이름도 바뀜. 알람 시각 표시(오전/AM) 맞음 | **없음** (영어 준비 중) | agent: `adb shell cmd locale set-app-locales com.lumian.wakeup --locales en-US`(앱만 바꿈, 안드로이드 13+) | P3 (영어 출시 때 P2) |
| 애니메이션 줄이기, TalkBack | 움직임 멈춤, 주요 버튼 읽힘 | 있음 (E 줄) | agent | P2 |
| 다른 갤럭시 기종(A 시리즈, 폴드, 안드로이드 13~14) | 같은 동작 | 없음 (S24+, Z플립5만) | agent: 삼성 Remote Test Lab에서 1~2대 짧게 | P2 |

### 2-6. 스토어 신고

| 상황 | 기대 동작 | 우리 상태 | 시험 방법 | 우선순위 |
|---|---|---|---|---|
| **`USE_EXACT_ALARM` 신고** | Play Console 신고에 '알람 시계 앱' 선택, 핵심 기능 설명 | 일부 (TSK-154 Play Console 항목 안에 묶여 있음, 줄로는 없음) | user (Play Console) | P1 |
| **전체 화면 알림 신고** | '알람' 핵심 기능으로 신고, 자동 허용 유지 | 없음 (줄 없음) | user | P1 |
| **포그라운드 서비스 유형 신고** | `systemExempted`, `mediaPlayback` 용도 설명과 영상. 8월 26일 시행 새 신고 방식 확인 | 없음 (줄 없음) | user + 리서치(신고 문구 초안) | P1 |
| 사전 출시 보고서 | 비정상 종료 0 | 있음 (H 줄, TSK-149) | agent | P1 |

## 3. 폰으로 못 하거나 하지 않을 것

| 항목 | 왜 못 하나 | 대신 |
|---|---|---|
| 실폰에서 전화 수신 흉내 | adb로 전화를 '받게' 하는 공개 명령이 없음 | 에뮬레이터 `adb emu gsm call`(agent) + 실제 전화 1번(user) |
| 진짜 배터리 0% | 몇 시간 걸리고 배터리에 나쁨 | `dumpsys battery set level`(낮은 배터리) + `reboot -p`(갑자기 꺼짐) |
| 무선 디버깅 재부팅 시험 | 삼성은 재부팅하면 무선 디버깅이 꺼짐 | **USB로 연결해서 재부팅**하면 adb가 다시 붙어요 |
| 삼성 최대 절전, 절전 앱 목록 | adb 공개 명령 없음 | 사용자가 설정에서 바꾸고 Claude가 로그 확인 |
| 보안 폴더 | adb로 다루기 어렵고 개인 데이터 영역 | user, 낮은 우선순위 |
| 실폰 저장 공간 채우기 | 사용자 파일 위험 | 에뮬레이터 |

## 4. 보강안 추천 순서

체크리스트 72줄에 아래 줄을 '(기준 추가)'로 더하고, 관리판 QA 묶음 7의 맞는 일에 붙이는 것을 추천해요. 새 일은 꼭 필요한 것만 만들어요.

| 순서 | 더할 것 | 붙일 일 | 방법 | 이유 |
|---|---|---|---|---|
| 1 | 울리는 중 전화가 옴(받기, 거절, 끊김), 미션 시간이 통화 때문에 벌받지 않음 | TSK-151 (user) + 에뮬레이터 부분은 TSK-137 (agent) | agent + user | 사용자가 직접 물었고, 지금 목록에 반대 방향만 있음 |
| 2 | 울리는 중 갑자기 꺼짐 → 켬 (`reboot -p`) | TSK-134 | agent + user(켜기만) | 배터리 0% 질문의 답. 알람이 사라지면 high |
| 3 | 스토어 신고 3종(정확한 알람, 전체 화면 알림, FGS 유형)을 따로 줄로 | TSK-154 | user | 빠지면 출시가 막히거나 전체 화면 권한이 빠짐 |
| 4 | 삼성 '딥 슬립 앱'에 들어간 상태의 알람 | TSK-136 | user + agent(로그) | 삼성 고유 위험 1순위(dontkillmyapp) |
| 5 | 배터리 부족(5%) 중 알람과 미션 | TSK-136 | agent | adb로 쉽게 됨 |
| 6 | USB 재부팅으로 A '재부팅 뒤 잠금 풀기 전' 다시 확인(출시 빌드) | TSK-134 | agent | 무선 디버깅 문제를 USB로 풀 수 있음 |
| 7 | 다른 알람앱(삼성 시계)과 같은 시각 | TSK-137 | agent (Z플립5, 허락 뒤) | 실제 사용자 흔한 상황 |
| 8 | 알람 울리는 중 앱 업데이트 | TSK-135 | agent | 출시 뒤 자동 업데이트가 아침에 걸릴 수 있음 |
| 9 | 삼성 최대 절전 모드 | TSK-136 | user | 막히면 경고 문구가 필요한지 판단 |
| 10 | Remote Test Lab으로 다른 갤럭시 1~2대(안드로이드 13~14, 폴드) | TSK-140 | agent | 우리 폰 두 대 밖의 기종 확인 |
| 11 | 화면 잠금 없음 / PIN 두 경우 | TSK-144 | user | 짧게 끝남 |
| 12 | 인터넷 통화 판정 단위 시험 | TSK-143 또는 개발 일 | auto | 코드만 있고 시험 없음 |
| 13 | 언어 영어(앱만) | TSK-147 (영어 출시 때) | agent | 영어 준비 일과 같이 |
| 14 | 듀얼 앱 복제, 보안 폴더, 저장 공간 부족 | TSK-140 | agent / user | 낮은 우선순위 |

- **하지 말 것:** 실폰에서 진짜 배터리 0% 만들기, 실폰 저장 공간 채우기, 폰 회전 설정 바꾸기. 시험에 바꾼 값(자동 시간, 시간대, 절전, 글자 크기, 소리)은 끝나면 처음 값으로 되돌리고 README에 남겨요.
- **qa-guide.md 고칠 곳(제안):** 1절 표에 '정확한 알람 Play 정책', '전체 화면 알림 제한', 'FGS 유형', '삼성 앱 관리', 'dontkillmyapp 삼성' 5줄을 더하고, 3절 '폰으로 못 하는 것' 표를 붙이면 돼요. 고치는 일은 QA 방이나 기획 방 몫이에요.

## 5. 확인 못 한 것

- `cmd alarm set-timezone`, `cmd alarm set-time`, `cmd media_session volume`이 Z플립5(안드로이드 16, One UI)에서 그대로 되는지는 폰에서 확인해야 해요.
- 전화가 올 때 시스템이 `USAGE_ALARM` 소리를 줄이는지는 문서에 없어 추론이에요. 1번 시험으로 확인해요.
- 알라미 같은 다른 알람앱 회사의 내부 QA 목록은 공개 자료를 찾지 못했어요.

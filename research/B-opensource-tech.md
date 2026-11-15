# B. 오픈소스와 기술 조사 (morning-up)

- 조사일: 2026-11-15
- 대상: 안드로이드 전용 앱 (Kotlin + Jetpack Compose), 미션을 풀어야 꺼지는 알람 앱 (알라미 비슷한 앱)
- 읽는 분: UI 디자이너. 어려운 말은 처음 나올 때 괄호로 뜻을 붙였어요.
- 라이선스와 업데이트 날짜는 GitHub에서 직접 확인한 값이에요 (2026-11-15 기준).

---

## 1) 결론 요약

1. **코드를 그대로 가져다 쓸 수 있는 건 Apache 2.0 라이선스 프로젝트뿐이에요.** AOSP DeskClock(안드로이드 기본 시계 앱의 공개 원본), Alarmio, Chronos, Simple Alarm Clock(yuriykulikov)이 여기에 해당해요. 유료 앱이나 소스를 공개하지 않는 앱에 넣어도 돼요. 단, 원본 저작권 표시와 라이선스 문구는 앱 안(오픈소스 고지 화면)에 남겨야 해요.
2. **Fossify Clock, Clock You(BlackyHawky), Chrono는 GPL-3.0이라 코드를 복사하면 우리 앱 전체 소스를 공개해야 해요.** 이 앱들은 "화면 흐름과 기능 아이디어 참고용"으로만 보고, 코드는 복사하지 않는 게 안전해요.
3. **Compose로 만든 참고 앱은 Chronos 하나 정도예요.** 나머지는 예전 방식(XML View, Java)이라 화면 코드는 어차피 새로 만들어야 해요. 그래서 오픈소스에서 실제로 가져올 가치가 큰 부분은 화면이 아니라 "알람 예약, 재부팅 후 복구, 울림 서비스" 같은 뒷단 로직이에요.
4. **알람이 확실히 울리게 하는 핵심 조합**은 `setAlarmClock()`(시스템이 가장 중요하게 다루는 알람 예약 방식) + `USE_EXACT_ALARM`(알람 앱 전용 정확한 알람 권한) + 전체 화면 알림(`USE_FULL_SCREEN_INTENT`) + 재부팅/시간대 변경 시 다시 예약이에요. 구글플레이에 "알람 앱"이라고 신고(선언)해야 두 권한이 자동으로 허용돼요.
5. **제일 큰 적은 제조사의 배터리 절약 기능이에요.** 삼성과 샤오미가 dontkillmyapp.com 최악 등급(5점)이에요. 첫 실행 안내(온보딩)에 "절전 예외 설정" 단계를 꼭 넣고, 제조사별 안내 화면을 따로 디자인해야 해요.
6. **미션 기능은 대부분 안드로이드 기본 기능이나 무료 구글 라이브러리로 만들 수 있어요.** 흔들기는 기본 센서(Seismic은 보관 처리돼 참고만), 사진/바코드는 CameraX + ML Kit, 손글씨는 ML Kit Digital Ink(한국어 지원), 걸음 수는 기본 걸음 센서(권한 필요)예요. 수학, 따라 쓰기, 기억력 게임은 라이브러리 없이 직접 만들면 돼요.

---

## 2) 오픈소스 비교 표

용어 풀이
- **Apache 2.0 / MIT**: 마음대로 써도 되는 관대한 라이선스. 상업 앱, 소스 비공개 앱에 넣어도 돼요. 저작권 표시만 남기면 돼요.
- **GPL-3.0**: "카피레프트" 라이선스. 이 코드를 섞어 배포하면 우리 앱도 GPL로 소스를 공개해야 해요. 돈을 받고 팔 수는 있지만 소스 공개 의무가 생겨요.
- **Compose**: 지금 안드로이드에서 권장하는 새 화면 제작 방식. **XML View**: 예전 방식.

| 이름 | 라이선스 | 상업/비공개 앱에 코드 사용 | 기술 | 최근 업데이트 (GitHub 기준) | 참고할 점 |
|---|---|---|---|---|---|
| **AOSP DeskClock** (안드로이드 기본 시계 원본, LineageOS 판 기준) | Apache 2.0 | 가능 (고지 필요) | Java, XML View | 2026-11-15 (LineageOS 저장소) | 알람 목록, 시간 선택, 요일 반복, 다시 울림(스누즈), 울림 화면, 재부팅 복구까지 "정석 구조"가 다 있어요. `AlarmStateManager`(알람 상태 관리), `AlarmService`/`AlarmKlaxon`(울림 소리와 진동), `AlarmActivity`(울림 화면), `AlarmInitReceiver`(재부팅 후 복구)가 핵심이에요. 권한 목록도 그대로 참고할 만해요 (`USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` 등). 단, 시스템 앱 전용 권한(`FOREGROUND_SERVICE_SYSTEM_EXEMPTED` 일부 용도, `DISABLE_KEYGUARD` 등)이 섞여 있어 그대로 복사하면 안 돼요. |
| **Simple Alarm Clock** (yuriykulikov/AlarmClock) | Apache 2.0 (README 표기) | 가능 | Kotlin, XML View, minSdk 21 / targetSdk 33 | 2026-06-03 | "가장 인기 있는 오픈소스 알람 앱"을 표방해요. 알람 상태를 단계별로 관리하는 구조가 깔끔해요. 대상 버전(targetSdk 33)이 낮아 안드로이드 14 이후 규칙은 따로 챙겨야 해요. 저장소에 LICENSE 파일이 따로 잡히지 않아 실제로 가져다 쓸 때는 한 번 더 확인이 필요해요. |
| **Alarmio** (fennifith) | Apache 2.0 | 가능 | Java, XML View | 2023-01-30, **보관(archived, 더 이상 관리 안 함)** | 디자인과 읽기 쉬움에 집중한 앱이에요. 관리가 끝나서 기술 참고용으로는 약해요. |
| **Chronos** (meenbeese, Alarmio 계열) | Apache 2.0 | 가능 | **Kotlin + Compose + Material 3**, Room(앱 안 저장소), minSdk 26 / targetSdk 36 | 2026-04-27 | **우리와 기술 구성이 가장 비슷해요.** Compose로 알람 목록/편집 화면을 어떻게 짰는지 참고하기 좋아요. 별 120개 정도의 작은 프로젝트라 품질 검증은 필요해요. |
| **Fossify Clock** (구 Simple Clock) | GPL-3.0 | **코드 복사 시 소스 공개 의무** | Kotlin, 주로 XML View, Room, targetSdk 36 | 2026-11-09 (활발) | 알람, 타이머, 스톱워치, 위젯을 갖춘 깔끔한 앱. 화면 구성과 설정 항목만 참고하세요. |
| **Clock You** (BlackyHawky/Clock) | GPL-3.0 | **코드 복사 시 소스 공개 의무** | Java, XML View, minSdk 23 / targetSdk 37 | 2026-11-14 (매우 활발) | "AOSP 시계를 바탕으로 개인 맞춤을 강화한 앱"이에요. 최신 안드로이드 대응이 가장 빨라서, 새 버전에서 생기는 문제를 어떻게 푸는지(이슈 게시판) 보는 용도로 좋아요. |
| **Chrono** (vicolo-dev/chrono) | GPL-3.0 | **코드 복사 시 소스 공개 의무** | Flutter(Dart) | 2025-01-13 | 별 1,800개대로 인기 많지만 Flutter라 우리 기술과 달라요. 화면 아이디어만 참고. |
| **Upupup** (Purgator) | GPL-3.0 | **코드 복사 시 소스 공개 의무** | Kotlin | 2026-11-13 (2026-07 생성) | "알라미에서 영감을 받은 미션 알람(흔들기, 수학)"이에요. 미션 알람 오픈소스는 거의 이것뿐이지만 아주 작은 신생 프로젝트예요. 아이디어 참고만. |

**우리에게 맞는 사용법 추천**
- 뒷단 구조(알람 예약, 상태 관리, 재부팅 복구, 울림 서비스): **AOSP DeskClock**의 구조를 따라 Kotlin으로 새로 작성. 필요하면 부분 코드를 가져오고 오픈소스 고지 화면에 표시.
- Compose 화면 코드 참고: **Chronos**.
- 최신 안드로이드 문제 해결 사례: **Clock You**, **Fossify Clock**의 이슈 게시판 (코드는 복사 금지).

---

## 3) 알람 안정성 체크리스트

용어 풀이
- **정확한 알람(exact alarm)**: "7시 00분 정각"에 울리는 알람. 안드로이드 12부터 특별 권한이 필요해요.
- **Doze(잠자기 모드)**: 폰을 오래 안 쓰면 배터리를 아끼려고 앱 활동을 미뤄 두는 기능.
- **포그라운드 서비스**: 화면 밖에서도 계속 돌아가는 작업. 상단 알림이 반드시 같이 떠요. 알람 소리를 계속 울릴 때 써요.
- **전체 화면 알림(full-screen intent)**: 잠금 화면 위로 알람 화면을 크게 띄우는 기능. 전화 수신 화면과 같은 방식이에요.

| # | 권한/설정 | 왜 필요한가 | 구글플레이 정책 주의 | 디자인에 미치는 영향 |
|---|---|---|---|---|
| 1 | **`setAlarmClock()`로 예약** | 시스템이 "가장 중요한 알람"으로 취급해 잠자기 모드 중에도 정시에 울려요. 시스템이 알람 직전에 잠자기 모드를 풀어 줘요. 상태 표시줄에 알람 아이콘도 떠요. | 없음 | 상태 표시줄 알람 아이콘을 누르면 열릴 화면(알람 편집)을 정해 둬야 해요. |
| 2 | **`USE_EXACT_ALARM`** (알람 앱 전용) | 설치하자마자 자동 허용되고, 사용자가 끌 수 없어요. | **"앱의 핵심 기능이 알람/타이머"인 앱만** 쓸 수 있어요. Play Console에서 신고해야 하고, 알람이 핵심이 아니면 거절될 수 있어요. morning-up은 해당돼요. | 권한 요청 화면이 필요 없어요. |
| 3 | **`SCHEDULE_EXACT_ALARM`** (대안/보조) | 같은 기능이지만 **사용자가 직접 켜야** 해요. 안드로이드 13 이상 새 설치에는 기본으로 꺼져 있고, 사용자나 시스템이 끌 수도 있어요. 꺼지면 예약된 정확한 알람이 전부 취소돼요. | `USE_EXACT_ALARM` 심사가 거절되면 이걸로 가야 해요. | 이 경우 "알람 및 리마인더 허용" 설정 화면으로 보내는 안내 화면이 필요해요. 권한이 다시 켜지면 알람을 다시 예약해야 해요. |
| 4 | **`USE_FULL_SCREEN_INTENT`** (잠금 화면 위 알람 화면) | 화면이 꺼져 있거나 잠겨 있을 때 미션 화면을 바로 띄우려면 필요해요. | 안드로이드 14 이상 대상 앱은 **알람 또는 전화 기능이 핵심인 앱만 자동 허용**돼요. 2024-05-31부터 Play Console 신고 필수, 2025-01-22부터 엄격 적용. 허용 안 되면 사용자가 직접 켜야 하고, 앱은 `canUseFullScreenIntent()`로 확인해야 해요. | 권한이 꺼져 있을 때를 대비한 안내 화면과, 전체 화면 대신 "알림만 뜨는 상태"의 대체 화면이 필요해요. |
| 5 | **`POST_NOTIFICATIONS`** (알림 표시 권한, 안드로이드 13+) | 알람 울림 알림, 다음 알람 미리 알림을 띄우려면 필요해요. 전체 화면 알람도 알림을 통해 뜨므로 사실상 필수예요. | 없음 (일반 실행 중 권한 요청) | 온보딩에서 "왜 알림이 필요한지" 설명 후 요청하는 화면을 넣어야 해요. |
| 6 | **포그라운드 서비스 종류 선언** (안드로이드 14+) | 알람 소리를 계속 울리고, 미션을 푸는 동안 앱이 죽지 않게 해요. 안드로이드 14부터 서비스 "종류"를 꼭 적어야 해요. | Play Console "앱 콘텐츠"에서 서비스 종류를 신고해야 해요. 정확한 알람 권한을 가진 앱은 `systemExempted` 종류를 쓸 수 있어요. `mediaPlayback`(미디어 재생) 종류는 안드로이드 15부터 재부팅 직후에는 시작할 수 없어요. `shortService`는 약 3분 제한이라 미션 알람에 부적합해요. | 울리는 동안 상단 알림의 모양(문구, 끄기 버튼 유무)을 디자인해야 해요. 미션 앱이므로 알림에서 바로 끄기 버튼은 빼는 게 맞아요. |
| 7 | **배터리 최적화 예외** | 일부 기기에서 앱을 강제로 재우는 것을 막아요. | 구글 정책상 **"핵심 기능이 망가지는 경우"에만** 직접 예외 요청 팝업(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)을 쓸 수 있어요. 공식 허용 표에 "알람 앱"은 명시돼 있지 않아요. 안전한 방법은 **배터리 설정 화면으로 이동시키는 방식**이에요. | "배터리 제한 없음" 설정 방법을 단계별로 보여 주는 안내 화면이 필요해요. |
| 8 | **재부팅 후 복구** (`RECEIVE_BOOT_COMPLETED`) | 폰을 껐다 켜면 예약된 알람이 **전부 사라져요**. 부팅 완료 신호를 받아 다시 예약해야 해요. | 없음 | 없음 (사용자에게 안 보임) |
| 9 | **잠금 해제 전 부팅 대응** (Direct Boot, `LOCKED_BOOT_COMPLETED`) | 재부팅 후 비밀번호를 아직 안 풀어도 알람이 울려야 해요. AOSP DeskClock도 이 방식을 써요. | 없음 | 잠금 해제 전에는 일부 데이터(예: 사진 미션 기준 사진)를 못 읽을 수 있어, 이때 쓸 "기본 미션"을 정해 둬야 해요. |
| 10 | **시간/시간대 변경 대응** (`TIME_SET`, `TIMEZONE_CHANGED`) | 해외 여행, 서머타임, 사용자가 시계를 바꿀 때 알람 시간이 어긋나지 않도록 다시 계산해야 해요. | 없음 | "시간대가 바뀌어 알람을 다시 맞췄어요" 같은 안내 문구를 고민할 수 있어요. |
| 11 | **앱 업데이트 후 복구** (`MY_PACKAGE_REPLACED`) | 앱이 업데이트되면 예약이 풀릴 수 있어 다시 예약해요. | 없음 | 없음 |
| 12 | **방해 금지 모드/무음 모드 우회** | 알람 소리는 "알람 음량" 채널로 울려야 무음 모드에서도 들려요. | 없음 | 설정 화면에 "알람 음량" 기준이라는 설명, 볼륨 미리듣기 필요. |

### 꼭 해야 할 테스트

| 테스트 | 방법 (개발자가 실행) | 통과 기준 |
|---|---|---|
| 잠자기 모드 | `adb shell dumpsys deviceidle force-idle` 후 알람 대기 | 정시 울림 |
| 앱 대기 상태 | `adb shell am set-inactive <패키지> true` | 정시 울림 |
| 재부팅 | 알람 예약 → 재부팅 → 잠금 해제하지 않고 대기 | 잠금 상태에서도 울림 |
| 시간대 변경 | 설정에서 시간대 바꾸기 (서울 ↔ 뉴욕) | 현지 시각 기준으로 울림 |
| 앱 강제 종료 | 최근 앱에서 밀어서 종료 / 설정에서 강제 종료 | 강제 종료 시 안드로이드 규칙상 알람이 취소될 수 있으니, 다음 실행 때 복구되는지 확인 |
| 권한 꺼짐 | 알림, 전체 화면, 정확한 알람 권한을 하나씩 끄기 | 앱이 경고 배너와 안내를 보여 줌 |
| 실제 기기 | **삼성(One UI), 샤오미(HyperOS)** 실기기에서 2~3일 방치 후 아침 알람 | 주말 지나고도 울림 |
| 긴 울림 | 미션을 안 풀고 10분 이상 방치 | 소리 계속, 앱 안 죽음 |
| 통화 중 알람 | 전화 중 알람 시각 도래 | 정해 둔 규칙대로 (작은 소리/진동 등) 동작 |

---

## 4) 제조사별 대응 표

dontkillmyapp.com 순위 (점수가 높을수록 앱을 심하게 죽임, 5점 만점): 화웨이, 샤오미, 원플러스, **삼성 5점** / 오포, 아수스 4점 / 비보, 리얼미, 모토로라 3점 / 소니 2점 / 순정 안드로이드(AOSP), 노키아 0점.

한국 사용자는 삼성 비중이 압도적이므로 **삼성 안내 화면을 1순위로** 만들어야 해요.

| 제조사 | 무엇이 문제인가 | 사용자가 해야 할 설정 | 앱에서 할 일 |
|---|---|---|---|
| **삼성 (One UI)** | 잘 안 쓰는 앱을 "절전 앱", "완전 절전 앱"으로 자동 분류해요. **주말 동안 앱을 안 열면 월요일 알람이 안 울릴 수 있다**고 dontkillmyapp이 경고해요. "사용하지 않는 앱 절전"이 켜져 있으면 며칠 뒤 다시 재워요. | 배터리 → 백그라운드 사용 제한 → 절전/완전 절전 목록에서 빼고 "절전 안 함 앱"에 추가 / "사용하지 않는 앱 절전" 끄기 / 앱 배터리 사용을 "제한 없음"으로 / 안드로이드 13에서는 "알람 및 리마인더" 허용 확인 | 삼성 기기 감지 시 전용 안내 화면, 설정 화면으로 바로 이동 버튼 |
| **샤오미 (MIUI/HyperOS)** | 백그라운드 앱을 매우 공격적으로 죽여요. 설정을 다 해도 죽었다는 개발자 보고도 있어요. | **자동 시작(Autostart) 켜기** / 배터리 절약을 "제한 없음"으로 / 최근 앱 화면에서 앱 **잠금(자물쇠)** / 화면 꺼진 후 계속 실행 허용 | 샤오미 전용 안내 (단계가 많아 이미지 위주 안내 권장). HyperOS는 메뉴 이름이 다를 수 있어 실기기 확인 필요 |
| **원플러스 / 오포 / 리얼미** (ColorOS 계열) | 배터리 최적화와 백그라운드 정리가 강해요. | 배터리 최적화 해제, 자동 실행 허용, 최근 앱 잠금 | 공통 안내 + 제조사별 설정 이동 시도 |
| **화웨이 / 아너** | 최악 등급. 한국 판매는 적어요. | 앱 실행 관리에서 "수동 관리" 후 자동 실행/백그라운드 실행 허용 | 우선순위 낮음 |
| **구글 픽셀 / 순정 안드로이드** | 거의 문제 없음 | 기본 권한만 | 기본 흐름 |

**디자인 제안**: 홈 화면 상단에 "알람이 확실히 울리려면 설정 2개가 더 필요해요" 같은 **상태 점검 카드**를 두고, 안 된 항목만 보여 주는 방식이 좋아요. 설정이 끝나면 카드가 사라지게요.

---

## 5) 미션별 구현 참고 표

| 미션 | 구현 방법 | 라이브러리 / 라이선스 | 필요한 권한 | 주의할 점 |
|---|---|---|---|---|
| **흔들기** | 안드로이드 기본 가속도 센서(`SensorManager`)로 흔든 횟수 세기 | 기본 기능(무료). 참고용 **Seismic(Square, Apache 2.0)**은 2024년 보관 처리돼 코드 수십 줄을 참고해 직접 작성 권장 | 없음 | 기기마다 센서 감도가 달라 "세게 / 보통 / 약하게" 난이도 조절이 필요해요. 진행률 표시(예: 30/50회)를 크게. |
| **수학 문제** | 앱 안에서 문제 생성 (덧셈, 곱셈, 난이도별) | 라이브러리 불필요 | 없음 | 숫자 키패드를 직접 디자인 (시스템 키보드보다 크고 오타 적게). |
| **따라 쓰기(타이핑)** | 제시 문장과 입력 문장 비교 | Compose 기본 입력칸 | 없음 | **한글은 자모 조합 중인 글자**가 있어 비교 시점 처리가 필요해요 (마지막 글자가 조합 중일 때 오답 처리하지 않기). 붙여넣기 막기 여부 결정 필요. |
| **손글씨 따라 쓰기** | 화면에 손가락으로 쓴 획을 글자로 인식 | **ML Kit Digital Ink Recognition** (구글, 무료, 기기 안에서 처리). 한국어 포함 300개 이상 언어 지원. 언어별 모델 약 20MB를 미리 내려받아야 해요. 오픈소스가 아니라 **ML Kit 이용약관**을 따라요. | 없음 (모델 다운로드 때 인터넷) | 아침에 인터넷이 없을 수 있으니 **알람 설정 시점에 모델을 미리 받아 두기**. 모델이 없으면 다른 미션으로 대체. |
| **사진 찍기 (지정 장소)** | 미리 찍어 둔 사진과 비슷한지 비교. 간단히는 ML Kit 이미지 라벨링(사물 종류 비교), 정밀하게는 이미지 특징 비교 | **CameraX** (AndroidX, Apache 2.0) + ML Kit Image Labeling (ML Kit 약관) | `CAMERA` | 어두운 아침 조명에서 인식률이 떨어져요. 실패 시 재시도 / 대체 미션 흐름 필요. |
| **바코드 / QR 찍기** | 등록해 둔 바코드(치약, 세면대에 붙인 QR 등)를 찍으면 해제 | **CameraX** + **ML Kit Barcode Scanning** (앱에 모델 포함 방식 권장, ML Kit 약관) | `CAMERA` | 인식이 빠르고 정확해서 사진 미션보다 실패가 적어요. **MVP 카메라 미션으로 추천.** |
| **걸음 수** | 기본 걸음 센서(`TYPE_STEP_DETECTOR`)로 N걸음 세기 | 기본 기능(무료) | `ACTIVITY_RECOGNITION` (안드로이드 10+, 신체 활동 권한) | 걸음 센서가 없는 기기가 있어 대체 미션 필요. 권한 요청 화면 필요. |
| **기억력 게임** | 카드 뒤집기, 순서 기억(불빛 순서 따라 누르기) | 라이브러리 불필요 (Compose 애니메이션) | 없음 | 잠이 덜 깬 상태라 터치 영역을 크게, 색만으로 구분하지 않기(색약 배려). |

**ML Kit 공통 참고**: 사진, 바코드, 손글씨 데이터 자체는 기기 안에서만 처리되고 구글 서버로 보내지 않아요. 다만 성능/사용 통계는 구글로 보내므로 **개인정보처리방침에 적어야** 해요.

---

## 6) MVP 기술 추천

| 영역 | 추천 | 이유 |
|---|---|---|
| 화면 | Kotlin + Jetpack Compose + Material 3 | 계획대로. Chronos를 참고 코드로 |
| 알람 저장 | Room (앱 안 데이터베이스) | AOSP, Fossify, Chronos 모두 이 방식 |
| 알람 예약 | `AlarmManager.setAlarmClock()` | 잠자기 모드에서도 정시 보장, 가장 안정적 |
| 정확한 알람 권한 | `USE_EXACT_ALARM` (Play Console에 "알람 앱"으로 신고). 대비책으로 `SCHEDULE_EXACT_ALARM` 흐름도 설계 | 알람이 핵심 기능이라 정책상 해당 |
| 울림 화면 | 전체 화면 알림 + 잠금 화면 위 표시 화면 + 포그라운드 서비스(소리, 진동) | AOSP DeskClock 구조 그대로 |
| 복구 | 부팅(잠금 전/후), 시간·시간대 변경, 앱 업데이트, 권한 재허용 시 모두 "전체 다시 예약" 한 함수로 처리 | 빠뜨리기 쉬운 부분을 한곳에 모음 |
| 기기 대응 | 온보딩 + 홈 상태 점검 카드 + 삼성/샤오미 전용 안내 | 실제 실패 원인 1위 |
| MVP 미션 (1차) | **흔들기, 수학, 따라 쓰기, 바코드/QR** | 외부 의존이 적고 실패 확률 낮음 |
| 2차 미션 | 걸음 수, 기억력 게임, 손글씨, 사진 | 권한, 모델 다운로드, 인식률 문제를 다듬은 뒤 |
| 라이선스 원칙 | Apache 2.0 / MIT 코드만 가져오기, GPL 코드는 보기만 | 나중에 유료화나 비공개 운영에 막힘이 없음 |
| 테스트 기기 | 삼성 갤럭시(최신 One UI) 1대 + 샤오미 1대 + 픽셀 또는 에뮬레이터 | 제조사별 차이 확인 |

**디자인 작업으로 이어지는 화면 목록 (이 조사에서 나온 것)**
1. 권한 온보딩: 알림 → 전체 화면 알람 → (필요 시) 정확한 알람 → 배터리 제한 없음 → 제조사별 추가 설정
2. 홈 상태 점검 카드 (설정 누락 시만 표시)
3. 울림 화면 (잠금 화면 위) + 울림 중 상단 알림 모양
4. 미션 실패/대체 미션 화면 (센서 없음, 카메라 인식 실패, 모델 없음)
5. 권한이 꺼졌을 때 경고 배너
6. 오픈소스 고지 화면 (설정 안)

---

## 7) 출처 링크

오픈소스
- AOSP DeskClock 원본: https://android.googlesource.com/platform/packages/apps/DeskClock/
- LineageOS DeskClock (구조, 권한 확인): https://github.com/LineageOS/android_packages_apps_DeskClock
- Simple Alarm Clock (yuriykulikov): https://github.com/yuriykulikov/AlarmClock
- Alarmio: https://github.com/fennifith/Alarmio
- Alarmio 라이선스 정보: https://alternativeto.net/software/alarmio/about
- Chronos: https://github.com/meenbeese/Chronos
- Fossify Clock: https://github.com/FossifyOrg/Clock , https://www.fossify.org/apps/clock/
- Fossify Clock 라이선스(F-Droid 계열 목록): https://apt.izzysoft.de/packages/org.fossify.clock
- Clock You: https://github.com/BlackyHawky/Clock
- Chrono: https://github.com/vicolo-dev/chrono
- Upupup: https://github.com/Purgator/Upupup
- Seismic (Square): https://github.com/square/seismic
- Apache 2.0 라이선스 원문: https://www.apache.org/licenses/LICENSE-2.0
- GPL-3.0 라이선스 원문: https://www.gnu.org/licenses/gpl-3.0.html

알람 안정성
- 알람 예약 공식 문서 (정확한 알람 권한, setAlarmClock, 재부팅): https://developer.android.com/develop/background-work/services/alarms
- 안드로이드 14 정확한 알람 기본 거부 변경: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- 구글플레이 권한 정책 (USE_EXACT_ALARM, 전체 화면 알림): https://support.google.com/googleplay/android-developer/answer/13986130
- 구글플레이 포그라운드 서비스 및 전체 화면 알림 신고: https://support.google.com/googleplay/android-developer/answer/13392821
- 전체 화면 알림 제한 (안드로이드 소스 문서): https://source.android.com/docs/core/permissions/fsi-limits
- 포그라운드 서비스 종류: https://developer.android.com/develop/background-work/services/fgs/service-types
- 포그라운드 서비스 변경 사항 (안드로이드 15 부팅 제한 등): https://developer.android.com/develop/background-work/services/fgs/changes
- 안드로이드 16 동작 변경: https://developer.android.com/about/versions/16/behavior-changes-all
- Doze와 배터리 최적화 예외, 테스트 명령: https://developer.android.com/training/monitoring-device-state/doze-standby
- 알림 권한(안드로이드 13): https://developer.android.com/develop/ui/views/notifications/notification-permission
- 암시적 브로드캐스트 예외 (부팅, 시간대 변경 신호): https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
- Direct Boot (잠금 해제 전 동작): https://developer.android.com/privacy-and-security/direct-boot
- 전체 화면 알림 변화 해설 (보조 자료): https://proandroiddev.com/full-screen-intent-fsi-notifications-in-android-14-15-what-changed-why-its-breaking-and-e5e862a75936

제조사 대응
- dontkillmyapp 순위: https://dontkillmyapp.com/
- 삼성: https://dontkillmyapp.com/samsung
- 샤오미: https://dontkillmyapp.com/xiaomi
- 샤오미 설정 후에도 죽는 사례 (개발자 포럼): https://b4x.com/android/forum/threads/how-to-prevent-a-app-from-killing-by-xiaomi-both-miui-and-hyper-os.169194

미션
- 센서 개요: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
- 걸음 센서: https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion
- 신체 활동 권한(안드로이드 10): https://android-developers.googleblog.com/2019/11/privacy-protections-for-physical.html
- CameraX: https://developer.android.com/media/camera/camerax
- ML Kit 바코드: https://developers.google.com/ml-kit/vision/barcode-scanning/android
- ML Kit 손글씨(Digital Ink): https://developers.google.com/ml-kit/vision/digital-ink-recognition/android
- ML Kit 손글씨 소개(언어 300개 이상, 모델 약 20MB): https://developers.googleblog.com/2020/08/digital-ink-recognition-in-ml-kit.html
- ML Kit 이미지 라벨링: https://developers.google.com/ml-kit/vision/image-labeling/android
- ML Kit 이용약관과 데이터 처리: https://developers.google.com/ml-kit/terms

### 확인이 더 필요한 것
- Simple Alarm Clock(yuriykulikov)은 README에 Apache 2.0이라고 적혀 있지만 GitHub가 LICENSE 파일을 인식하지 못했어요. 코드를 가져오기 전에 저장소의 라이선스 파일을 직접 확인해야 해요.
- 배터리 최적화 "직접 예외 요청 팝업"을 알람 앱이 써도 되는지는 공식 허용 표에 없어서 판단이 애매해요. MVP에서는 설정 화면으로 이동시키는 방식만 쓰는 걸 추천해요.
- 안드로이드 16에서 알람과 직접 관련된 큰 변경은 이번 조사에서 찾지 못했어요. 작업 예약(JobScheduler) 할당량 변경은 있지만 알람 울림에는 영향이 적어요. 개발 시작 시점에 최신 문서를 한 번 더 확인하세요.
- 샤오미 HyperOS의 실제 메뉴 이름은 dontkillmyapp에 반영이 덜 돼 있어 실기기 확인이 필요해요.

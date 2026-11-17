# 3단계 작업표: 홈과 알람 편집

> 자동 실행 기록: 보여 주고 기다리지 않고 바로 진행했어요. 질문 단계는 건너뛰고 기획서 추천안을 따랐어요.

**목표:** 알람 탭 홈(H0, H1)에서 알람을 보고, 켜고 끄고, 지우고(H5, H7), 편집 화면(S0 두 상태, S0-1)에서 만들고 고쳐 저장(H6)해요. 저장, 토글, 삭제 뒤에는 늘 예약이 따라와요.

**구조:**
- `:core:model` `engine/TimeUntil`(몇 시간 몇 분 후), `engine/NextAlarm`(다음에 울릴 알람), `RepeatPattern`(매일, 평일, 주말), `AlarmBook`(화면이 쓰는 알람 창구 인터페이스).
- `:core:alarm` `SchedulingAlarmBook`(저장소 + `AlarmRescheduler` + 예약 취소를 한 번에. feature는 이것만 불러요), `MinuteTicker`(분이 바뀔 때마다 알림), 상태 표시줄 알람 아이콘의 show intent에 알람 ID 싣기.
- `:core:ui` (새 모듈) 알람 글자 만들기: '오전 6:30', '7시간 20분 후', 요일, 미션 이름.
- `:core:designsystem` 공통 부품: Setting Row, Select Mark, Day Chip, Top Bar, Icon Button, 설정 카드, Preview Button, Dialog, Toast, Snackbar, 바텀시트 몸체, 시간 드럼롤(Wheel Picker), 굴러 바뀌는 글자(Rolling Text), 버튼 스타일(보조, 위험).
- `:feature:home` H0, H1, H5, H6, H7. `:feature:edit` S0, S0-1. 둘 다 `core`만 써요.
- `:app` Navigation(타입 안전 경로 `MainRoute`, `EditRoute(alarmId)`), 상태 표시줄 아이콘 → S0.

**근거:** `docs/dev-plan.md` 3단계와 '모션과 인터랙션', `docs/dev-readiness.md` 3번 흐름 표 1, 4-1, F9, `docs/design-system.md` '부품', '부품별 규칙', '홈 (A)', '홈 하늘', '알람 편집 (B)', 나머지 화면의 Dialog 문구. 피그마 `화면` 페이지 H0(`32:13`), H1(`138:16`), H5(`138:318`), H6(`138:358`), H7(`138:382`), S0(`34:32`), S0 미션 없음(`402:10951`), S0-1(`139:2482`).

## 작업

### 작업 1: 순수 계산 (TDD)
- `TimeUntil.between(now, at)`: 분 단위 올림, 최소 1분, 하루 넘으면 일 단위
- `NextAlarm.find(alarms, now, holidays)`: 켜진 알람 중 가장 먼저 울릴 것
- `RepeatPattern.of(days)`: 한 번, 매일, 평일, 주말, 직접 고름
- `WakeCheck` 처음 값을 기획서 F9(1분 후, 1분)로

### 작업 2: 알람 창구와 예약
- `AlarmBook`(alarms, get, save, setEnabled, delete, restore)과 `SchedulingAlarmBook`
- 테스트: Robolectric + 메모리 Room으로 저장 → 예약, 끄기 → 예약 취소, 삭제 → 예약 취소, 되돌리기 → 다시 예약
- `ExactAlarms.setAlarmClock`의 show intent에 알람 ID, 알람마다 다른 요청 코드

### 작업 3: 공통 부품
- 위 부품들과 Robolectric 테스트(Setting Row 비활성, Dialog 버튼, Snackbar 되돌리기)

### 작업 4: 홈
- `HomeViewModel`(알람 목록, 다음 알람, 분마다 다시 계산, 토글, 삭제 확인, 되돌리기, 저장 토스트)
- 화면: 하늘 자리(토큰 그라데이션), 상단 문구(굴러 바뀜), +와 더보기, 카드 목록(추가와 삭제 움직임), 빈 홈, 다이얼로그, 토스트, 스낵바
- 테스트: Turbine ViewModel, Robolectric 빈 홈과 카드

### 작업 5: 편집
- `EditViewModel`(새 알람 처음 값, 불러오기, 바뀜 감지, 저장, 나가기 확인, 미션 없음 상태)
- 화면: Top Bar, 안내 글, 드럼롤, 반복 카드, 기상 미션 카드, 울림 화면과 소리 카드, 하단 고정 저장
- 테스트: Turbine ViewModel, Robolectric 미션 없음 상태

### 작업 6: 앱 연결
- NavHost, 알람 탭 → 홈, 카드와 + → 편집, 저장 → 홈 + 토스트, 상태 표시줄 아이콘 → 편집
- 디버그 견본은 설정 탭 자리에 그대로

## 미룬 것
- 하늘 장면(해, 달, 궤도, 날씨): 7단계
- 미션 고르기 S1-2와 '미리 해보기': 5단계. 다시 잠들기 방지 S4: 4단계. 다시 울림 S3, 알람음 S5, 볼륨 S6, 부드럽게 깨우기 S7, 배경 S8: 7단계(지금은 줄만 있고 눌러도 아무 일 없음)
- 더보기 메뉴(H2), 선택 모드(H3), 이번만 쉬기(H8): 다음 단계
- 전원 종료 방지 동의(P6): 권한 단계

## 진행 기록
- 작업 1~6 모두 마쳤어요. 공유 요소 전환은 넣지 않고 슬라이드 + 페이드(spatialSlow)로 두었어요(ADR 0003).
- 편집 화면 composable 이름은 경로 클래스 `EditRoute`와 겹치지 않게 `AlarmEditRoute`로 했어요.

## 끝 기준
`./gradlew build` 통과, 커밋, push. 폰 확인은 보고서의 확인 목록으로.

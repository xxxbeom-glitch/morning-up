# ADR 0003: 알람 창구(AlarmBook)와 화면 길

- 날짜: 2026-11-17 (자동 실행 중 결정, 사용자 확인 필요)
- 상태: 채택

## 배경
3단계에서 홈과 알람 편집 화면이 알람을 읽고, 저장하고, 켜고 끄고, 지워요. 저장할 때마다 예약도 같이 맞아야 해요.
화면 모듈(`:feature:*`)이 저장소와 예약기를 따로 부르면 '저장은 됐는데 예약을 빠뜨림' 같은 실수가 생겨요.

## 결정

### 1. 화면은 `AlarmBook` 하나만 불러요
- 인터페이스 `AlarmBook`은 `:core:model`에 둬요(`alarms`, `get`, `save`, `setEnabled`, `delete`, `restore`).
- 구현 `SchedulingAlarmBook`은 `:core:alarm`에 두고 Hilt로 묶어요. 저장소에 쓰고 바로 `AlarmRescheduler`로 다시 예약해요. 삭제는 예약을 먼저 지우고 저장소에서 지워요.
- 테스트는 `:core:testing`의 `FakeAlarmBook`으로 해요. 무엇을 불렀는지 `calls`에 남아요.

### 2. '분이 바뀜' 신호
- `MinuteTicker`(core:alarm)가 매 분 정각 직후에 신호를 줘요. 홈의 '몇 시간 몇 분 후'와 편집 화면 안내 글이 이걸로 다시 계산해요.
- 몇 분 후는 올림으로 세요(6:29:30에 6:30 알람이면 '1분 후').

### 3. 화면 길
- `:app`의 `MorningNavHost`에 타입 안전 경로 두 개를 둬요. `MainRoute`(탭 세 개)와 `EditRoute(alarmId)`(0이면 새 알람).
- 편집 화면은 저장한 id를 앞 화면 `savedStateHandle`에 남기고 돌아가요. 앱이 이 값을 `HomeRoute(savedAlarmId)`로 넘겨 H6 토스트를 띄워요.
  ViewModel의 `SavedStateHandle`은 NavBackStackEntry 쪽과 다른 객체라서 직접 받지 않아요.
- 상태 표시줄 알람 아이콘은 `EXTRA_EDIT_ALARM_ID`를 담은 Intent로 `MainActivity`를 열어요. `onCreate`, `onNewIntent`에서 읽어 `EditRoute(id)`로 가요.
- 화면 넘김은 오른쪽에서 밀려 들어오는 슬라이드(spatialSlow) + 페이드예요. '애니메이션 줄이기'면 페이드만 해요.
  카드에서 편집 화면으로 이어지는 공유 요소 전환은 탭 껍데기와 NavHost가 겹쳐 있어 복잡해서 이번에는 넣지 않았어요.

## 결과
- 화면 모듈은 저장소, 예약기, AlarmManager를 몰라요. 예약 규칙이 바뀌어도 `SchedulingAlarmBook`만 고치면 돼요.
- Room 구조는 바꾸지 않았어요.

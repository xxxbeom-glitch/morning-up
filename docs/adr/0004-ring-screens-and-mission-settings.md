# ADR 0004: 울림 화면 연결(RingLink, 미션 자리)과 미션 설정 저장

- 날짜: 2026-11-17 (자동 실행 중 결정, 사용자 확인 필요)
- 상태: 채택

## 배경
4단계에서 울림 화면(D0, W1, W2)과 미션 화면(M3 Quick Math)이 생겨요. 두 화면은 각각 `:feature:ring`, `:feature:mission`에 있고
feature끼리는 서로 부르지 않아요. 미션마다 단계와 개수가 있어서 알람에 함께 저장해야 해요. 통화 중에 알람이 울리는 경우도 정해야 해요.

## 결정

### 1. 화면은 `RingLink` 하나로 울림 엔진과 이야기해요
- `RingLink`(core:alarm)는 `session`, `screen` 두 상태와 `send(event)`, `closeWakeUp()`만 있어요.
- 구현 `ServiceRingLink`는 버튼 신호를 `RingService.send`로 보내요. 서비스가 앞에 떠 있어야 소리와 타이머가 이어지기 때문이에요.
- 테스트는 `:core:testing`의 `FakeRingLink`, `testRingSession()`을 써요.

### 2. 미션 화면은 슬롯으로 꽂아요
- `:feature:ring`의 `RingRoute(missionContent = { index -> ... })`가 미션 자리만 비워 두고, `:app`의 `RingActivity`가 그 자리에
  `:feature:mission`의 화면을 넣어요. 그래서 두 feature가 서로를 몰라요.
- D0에서 미션으로 갈 때 `RingRoute`가 D0 버튼 중심에서 원형으로 잘라 넓히며 미션 화면을 보여 줘요(`spatialSlow`).
  '애니메이션 줄이기'면 짧은 페이드예요.
- 4단계에서는 미션 종류와 상관없이 Quick Math를 꽂아요. 나머지 미션은 5단계에서 종류별로 나눠요.

### 3. 미션 설정은 같은 `missions` 칸에 JSON으로
- `Alarm.missions`는 `List<MissionSetting>`(종류, 단계, 개수, 속도)이에요. 종류 목록만 필요하면 `missionTypes`를 써요.
- Room은 칸을 새로 만들지 않고 v1→v2 Migration에서 기존 `missions` 글자('QuickMath,DropWords')를 JSON 배열로 바꿔요.
  표 구조는 그대로여서 진실이 한 곳에만 있어요. 읽을 때 깨진 원소는 건너뛰고 범위 밖 값은 범위 안으로 맞춰요.
- 울림 세션(`RingPlan`)에는 `missionSettings`를 더했고 처음 값은 빈 목록이에요. 그 전에 저장된 세션도 읽히고,
  비어 있으면 `missionSetting(index)`가 그 미션의 처음 값을 줘요.

### 4. 59초 규칙의 조작 신호는 묶어서 보내요
- 키를 누를 때마다 서비스로 보내지 않고, 처음 한 번은 바로, 2초 안의 나머지는 창 끝에 한 번으로 묶어요(`InteractionSignal`).
- 화면 고리는 누르는 순간 바로 다시 차요. 엔진의 마감은 늘 마지막 조작보다 같거나 늦어서 화면보다 먼저 끝나지 않아요.

### 5. 통화 중 알람
- 전화 권한 없이 `AudioManager` 모드(IN_CALL, IN_COMMUNICATION, RINGTONE)로 통화 중인지 알아요(`CallMonitor`).
- 통화 중이면 소리는 최대 10%, 진동은 켜요(`CallPolicy`). 통화가 끝나면 원래 소리로 돌아와요.

## 결과
- 울림 화면 모듈은 서비스, 저장소, 예약기를 몰라요. 미션이 늘어도 `RingActivity`의 슬롯만 고치면 돼요.
- 옛 알람과 옛 세션 둘 다 그대로 읽혀요(테스트: `MigrationTest`, `RingSessionSerializationTest`).

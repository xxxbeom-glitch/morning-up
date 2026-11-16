# 2단계 작업표: 알람 엔진

> 자동 실행 기록: 보여 주고 기다리지 않고 바로 진행했어요. 질문 단계는 건너뛰고 기획서 추천안을 따랐어요.

**목표:** 화면 없이도 알람이 확실히 울리는 부분 전체. 다음 울릴 시각, 울림 상태표, 다시 예약 길을 테스트로 촘촘히 막아요.

**구조:** 판단은 모두 순수 함수(안드로이드 없이 도는 코드)로 `:core:model`의 `engine` 패키지에 둬요. 안드로이드 쪽(`:core:data`, `:core:alarm`, `:app`)은 그 판단 결과(효과)를 실행만 해요. 설계 근거는 `docs/adr/0002-alarm-engine.md`.

**근거:** `docs/dev-plan.md` 2단계, 검수 초점 1~4, 6, 체크리스트 A, B.

## 작업

### 작업 1: 모델과 저장소
- `:core:model` `Alarm`(시각, 반복 요일, 공휴일 끄기, 켜짐, 미션 목록, 다시 울림, 다시 잠들기 방지, 전원 종료 방지, 알람음 ID, 볼륨, 진동, 부드럽게 깨우기, 배경, 이번만 쉬기 날짜, `nextTriggerAt`), `RingAgain(intervalMinutes 3/5/10/15, count 3/5/null=끌 때까지)`, `WakeCheck`
- `:core:data` Room `AlarmEntity`, `AlarmDao`, `MorningDatabase`(v1, 구조 파일 내보냄), `DiagnosticLogEntity`(최대 500줄). 기기 보호 저장소에 만들어요
- `RingSessionStore`: 울리는 중 상태를 기기 보호 DataStore에 JSON으로 저장
- 테스트: Robolectric으로 DAO 저장과 읽기, 진단 기록 500줄 자르기, 울림 상태 저장과 복원, 구조 파일 v1 열기

### 작업 2: 공휴일과 다음 울릴 시각 (TDD)
- `KoreanHolidays`: 2026~2027 공휴일과 대체공휴일. `covers(year)`
- `NextRingCalculator.next(alarm, now: ZonedDateTime, holidays): ZonedDateTime?`
- 테스트: 요일 반복, 자정(23:59, 00:00), 같은 분 경계, 한 번 알람, 공휴일 끄기(설 연휴, 대체공휴일), 이번만 쉬기(그날만 건너뜀, 지난 날짜는 무시), 공휴일과 이번만 쉬기 겹침, 꺼진 알람, 요일 없음과 공휴일 끄기, 목록 밖 연도, 서울과 뉴욕 시간대, 서머타임 없는 시각(2:30 → 3:30), 두 번 생기는 시각(앞 시각)

### 작업 3: 울림 상태표 (TDD)
- `RingStateMachine.reduce(state, event, now): Transition(state, effects)`
- 테스트: 상태표의 모든 줄(ADR 0002 표)

### 작업 4: 다시 예약과 꺼져 있던 동안 놓친 알람 (TDD)
- `BootCatchUpPolicy`(10분 안이면 바로 울림, 넘으면 놓친 알람 알림)
- `AlarmRescheduler.rescheduleAll(now)`: 켜진 알람마다 놓침 판단, 다음 시각 계산, 예약, `nextTriggerAt` 저장. 꺼진 알람은 예약 취소
- 테스트: 가짜 저장소와 가짜 예약기로 JVM 테스트

### 작업 5: 안드로이드 연결
- `AndroidAlarmScheduler`(`setAlarmClock`, 권한 없으면 정확하지 않은 예약으로 대신, 타이머는 `setExactAndAllowWhileIdle`)
- `SystemEventReceiver`(LOCKED_BOOT_COMPLETED, BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED, SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) → 전체 다시 예약
- `AlarmFireReceiver` → 울림 서비스
- `RingService`(포그라운드 `systemExempted`, MediaPlayer USAGE_ALARM로 앱 기본음, 진동, 전체 화면 알림, 상태표 실행)
- 임시 D0(`RingActivity`, 시각과 버튼 하나, 잠금 화면 위)
- 앱 기본 알람음 `res/raw/alarm_default.wav`(직접 만든 소리라 라이선스 걱정 없음)
- 매니페스트 권한과 `directBootAware`
- 테스트: Robolectric으로 예약(ShadowAlarmManager), 시스템 신호 → 다시 예약, 권한 없을 때 대신 예약

## 미룬 것
- 통화 중 작게 울리기, 블루투스 이어폰 처리, 부드럽게 깨우기: 4단계 울림 화면과 같이
- 자동 백업 복원 뒤 알림: 6단계 설정과 같이
- 실제 미션 화면: 4단계

## 끝 기준
`./gradlew build` 통과, 커밋, push. 폰 확인 관문(dev-plan 2단계)은 아침 확인 목록으로.

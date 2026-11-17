# 4단계 작업표: 울림에서 해제까지 한 바퀴 (Quick Math 하나로)

> 자동 실행 기록: 보여 주고 기다리지 않고 바로 진행했어요. 질문 단계는 건너뛰고 기획서 추천안을 따랐어요.

**목표:** 진짜 알람이 울리면 D0(아침, 밤, 미션 있음/없음) → M3 Quick Math → W1 해제 완료 → (켜져 있으면) W2 깨어 있나요까지 한 바퀴 돌아요. 59초 '손을 놓은 시간', 미션 중 소리 20%, 뒤로가기 막기, 통화 중 작게 울리기를 지켜요. 편집 S0에서 S4 다시 잠들기 방지 화면을 열어요.

**구조:**
- `:core:model` `MissionSetting`(미션마다 단계, 개수, 속도. mission-levels.md 2번 표 범위와 처음 값), `Alarm.missions: List<MissionSetting>`, `RingPlan.missionSettings`(옛 세션 JSON은 처음 값으로 채움), `mission/QuickMath`(문제 만들기, 채점, 키패드 섞기. 난수 주입), `engine/CallPolicy`(통화 중 소리와 진동), `DayPart`(아침, 밤).
- `:core:data` Room v2: `alarms.missions` 칸을 쉼표 이름 목록에서 JSON(미션 설정 목록)으로 바꾸는 Migration 1→2, 구조 파일 2.json, Migration 테스트.
- `:core:alarm` `RingLink`(화면이 보는 울림 창구: session, screen, send, closeWakeUp)와 서비스 구현, `CallMonitor`(통화 알아채기, 권한 없이 AudioManager 모드), 소리 쪽에 통화 정책 적용, W2 알림 제목.
- `:core:designsystem` `MissionColors`(`mission/…` 색 묶음), `MorningRadioRow`, `MorningHaptic.KeyTap`, 날짜 글자.
- `:feature:ring` (새) D0, W1, W2 화면과 `RingViewModel`.
- `:feature:mission` (새) 미션 화면 틀(바탕, 위 줄, 59초 고리), M3 Quick Math 화면과 `QuickMathViewModel`, 조작 신호 묶기.
- `:feature:edit` S4 다시 잠들기 방지(편집 안 전체 화면 페이지), 디버그 빌드만 미션 토글로 Quick Math 켜기.
- `:app` `RingActivity`가 feature 화면을 바꿔 그리고(D0 → 미션은 버튼 자리에서 원형으로 퍼짐), W1 '확인' 뒤 잠금 여부로 닫기 또는 홈, 디버그 '1분 뒤 미션 알람'은 Quick Math.

**근거:** `docs/dev-plan.md` 4단계, '모션과 인터랙션'의 알람 울림과 미션 표, 체크리스트 C. `docs/dev-readiness.md` F9, F14, 4-1, 3번 흐름 표. `docs/mission-levels.md` Quick Math, 2번 표. `docs/design-system.md` '알람 울림 (D)', '나머지 화면'의 W1, W2, '미션 플레이 화면 (M)'. 피그마 D0(`34:129`), D0 밤(`217:2765`), M3(`351:10565`), W1(`140:1211`), W2(`140:1263`), S4(`170:1672`).

## 작업

### 작업 1: 미션 설정과 Quick Math (TDD)
- `MissionSetting` 범위와 처음 값, 고른 카드 설명에 필요한 값
- `QuickMath.problem(level, random)`: 7단계 규칙(답은 늘 양수, ×1 ×10 +0 같은 쉬운 수 제외, 같은 수 겹침 제외, 답 자릿수)
- `QuickMathGame`: 숫자 입력, 지우기, 확인. 연속 정답 N개면 끝, 틀리면 새 문제로 처음부터, 정답마다 키패드 다시 섞기(지우기와 확인은 고정)
- `RingPlan`에 미션 설정 싣기, 옛 JSON 읽기 테스트
- `CallPolicy`, `DayPart` 테스트

### 작업 2: 저장소 v2
- Migration 1→2(쉼표 이름 → JSON), 구조 파일 내보내기, 테스트(옛 알람이 미션과 함께 그대로 남는지)

### 작업 3: 울림 창구와 통화 중
- `RingLink`, `FakeRingLink`(core:testing)
- `CallMonitor` + `AlarmSoundPlayer`가 통화 중엔 작은 소리와 진동, 끝나면 원래대로
- W2 알림: 이미 전체 화면 알림 길(ID 1, 전체 화면 Intent). 제목 글을 W2용으로

### 작업 4: 화면
- designsystem: 미션 색, Radio Row, 키 진동
- `:feature:ring` D0(아침/밤 × 미션 있음/없음), W1(Wake Dialog, 날씨 없이 짧은 인사), W2(남은 시간, '깼어요')
- `:feature:mission` 미션 틀, 59초 고리(마지막 10초 숨쉬기, 마지막 5초 틱, 조작하면 스프링으로 다시 참), M3
- `:feature:edit` S4, 디버그 Quick Math 임시 길
- `:app` RingActivity 연결, 원형 퍼짐 전환, W1 뒤 잠금 화면 또는 홈

### 작업 5: 테스트
- ViewModel(Turbine): D0 버튼 글, Quick Math 정답/오답, 키패드 섞임, 조작 신호 묶기, 미션 다시 시작
- Compose UI(Robolectric): D0 버튼 글, M3 키 누르기와 확인, W2 버튼, S4 고르기

## 미룬 것
- 부드럽게 깨우기, 블루투스 이어폰: 7단계
- 진짜 하늘 장면(해, 달, 날씨)과 W1 뒤 실제 홈 화면: 7단계(지금은 토큰 그라데이션 위 딤)
- 미션 고르기 S1-2, 나머지 미션 화면: 5단계

## 끝 기준
`./gradlew build` 통과, 커밋, push. 폰 확인 관문은 보고서의 폰 확인 목록으로.

# ADR 0005: 미션 플레이어 공통 구조, 미리보기 길, 문제 데이터 파일

- 날짜: 2026-11-17 (자동 실행 중 결정, 사용자 확인 필요)
- 상태: 채택

## 배경
5단계에서 미션이 네 개(Quick Math, Pop Quiz, Drop Words, Shake Shake)가 되고, 알람 하나에 최대 세 개를 고른 순서대로 해요.
같은 미션을 울릴 때와 편집 화면 '미리 해보기', 시트 '미리보기'에서도 해요. 미리보기는 소리와 기록이 없고
59초가 끝나도 다시 울리지 않아요. 문제와 단어는 10단계에서 실제 목록으로 바뀌어요.

## 결정

### 1. 미션 규칙은 `:core:model`, 화면 상태는 `MissionRunner` 하나
- 미션마다 순수 규칙 클래스(`PopQuizGame`, `DropWordsGame`, `ShakeCounter` 등)를 `:core:model`에 두고 난수를 주입해 테스트해요.
- `:feature:mission`의 `MissionRunner`가 설정 하나를 받아 알맞은 규칙을 열고, 조작(`mathKey`, `quizAnswer`, `dropKey`,
  `shake`, `frame`)을 받아 `MissionPlay`(화면 상태)와 `Outcome`(무시, 조작, 끝)을 돌려줘요.

### 2. 뷰모델은 공통 뼈대 하나에 울림용과 미리보기용 두 개
- `MissionHostViewModel`(추상)이 지금 미션, 순서, 59초 끝 시각, '다음 미션 n / total' 넘어가기(0.8초)를 가져요.
- `RingMissionViewModel`은 `RingLink` 세션을 따라가요. 조작하면 엔진에 `MissionInteraction`을 2초 창으로 묶어 보내고,
  미션을 끝내면 바로 `MissionCompleted`를 보내요. 엔진이 다음 순서로 넘기면 넘어가기를 보여 준 뒤 59초를 새로 시작해요.
  다시 울린 뒤 돌아오면 넘어가기 없이 하던 미션부터 처음부터 해요.
- `MissionPreviewViewModel`은 엔진에 아무것도 보내지 않아요. 59초가 끝나거나 닫기를 누르면 닫히고,
  다 끝내면 '잘했어요'를 1.2초 보여 주고 닫혀요. 진짜 알람 세션이 생기면 바로 닫혀요.
- 뷰모델은 타이머를 돌리지 않아요. 화면이 매 장면 `onFrame()`을 부르고 시각은 `NowProvider`로 읽어요.
  테스트에서 가짜 시계로 시간을 정확히 정할 수 있어서예요.

### 3. 미리보기는 `:app` Navigation의 별도 길
- `MissionPreviewRoute(settingsJson)`이 고른 설정 목록을 JSON 글자로 들고 가요. 편집 화면은 `onPreview(List<MissionSetting>)`만
  부르고 `:app`이 길을 열어요. feature끼리 서로 부르지 않는 규칙을 지켜요.

### 4. 문제와 단어는 `res/raw/mission_content.json`
- 단계마다 임시 10개예요. 10단계에서 이 파일만 바꾸면 돼요. `MissionContentSource`로 주입해 테스트는 작은 묶음을 써요.

### 5. 미션 고르기는 편집 화면 안 페이지와 시트
- S1-2는 `EditDetail.Mission` 페이지, 설정 시트는 `MorningModalSheet`예요. 상태는 `EditViewModel` 하나가 가져요
  (화면 하나에 뷰모델 하나). 안 고른 미션의 시트 값도 기억해 두고, 고른 미션의 값은 바로 저장해요.
- 4단계의 디버그 임시 길(토글을 켜면 Quick Math)과 `buildConfig`는 지웠어요.

## 결과
- 새 미션을 더하려면 규칙 클래스, `MissionPlay` 한 갈래, 화면 내용 하나만 더하면 돼요.
- 미리보기와 울림이 같은 화면과 규칙을 써서 '미리 해 본 것과 아침에 하는 것'이 달라지지 않아요.

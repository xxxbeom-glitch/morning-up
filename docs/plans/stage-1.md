# 1단계 작업표: 디자인 토큰, 움직임 토큰, 하단 메뉴 빈 탭

> 자동 실행 기록: 보여 주고 기다리지 않고 바로 진행했어요.

**목표:** 피그마 변수와 똑같은 코드 토큰, 스프링 움직임 토큰(`MorningMotion`), 누름 반응과 진동 도우미, 하단 메뉴(알람, 잠들기, 설정) 빈 탭, 견본 화면.

**구조:** 피그마 값은 `MorningTokens.kt`의 표(피그마 이름 → 값) 한곳에만 적어요. `MorningColors`, `MorningDimens`, `MorningTypography`, `MorningShadows`는 이 표에서 이름으로 꺼내 만들어요. 테스트는 피그마에서 읽어 저장한 `figma-tokens.json`과 표를 비교해요.

**근거:** `docs/dev-plan.md` 1단계와 '모션과 인터랙션', `docs/design-system.md` 색, 크기, 글자, 그림자, 부품 규칙. 피그마 `디자인 시스템` 페이지(`137:704`) Bottom Nav `285:7033`, Toggle, Button.

## 작업

### 작업 1: 토큰 표와 비교 테스트
- 만들 것: `core/designsystem/.../theme/MorningTokens.kt` (`internal val LightColorTokens: Map<String, Long>`, `DarkColorTokens`, `DimensionTokens: Map<String, Int>`, `TextStyleTokens: Map<String, TextStyleToken>`, `ShadowTokens: Map<String, ShadowToken>`)
- 테스트 `FigmaTokenParityTest`: 표의 이름 집합과 값이 JSON과 같음 (색 라이트, 다크, 크기, 글자 13개, 그림자 3개)

### 작업 2: 토큰 객체와 테마
- `MorningColors`(피그마 이름의 낙타 표기: `background/base` → `backgroundBase`), `MorningDimens`, `MorningTypography`(Pretendard 4굵기), `MorningShadows`, `MorningTheme`(다크는 기기 설정, F18)
- 테스트 `MorningColorsTest`: `lightMorningColors().backgroundBase == Color(0xFFF2F4F9)`, 다크 `primaryDefault == Color(0xFF8AB8FB)`

### 작업 3: 움직임과 진동
- `MorningMotion`: `spatialFast`(0.6, 1400), `spatialDefault`(0.8, 700), `spatialSlow`(0.85, 300), `effectsFast`(1.0, 1600), `effectsDefault`(1.0, 700). '애니메이션 줄이기'면 위치, 크기 움직임은 150ms 페이드용 tween으로 바꿔요
- `Modifier.pressScale()` 0.96배, 물결 효과 없음. `Modifier.morningClickable(haptic)`
- `MorningHaptic` enum과 `toFeedbackConstant(sdkInt)` (지원 안 하는 폰 대체 값). 기기 '터치할 때 진동'은 `View.performHapticFeedback`이 알아서 따라요
- 테스트 `MorningMotionTest`(감쇠, 강도 값), `MorningHapticTest`(API 29에서 CONFIRM → VIRTUAL_KEY, API 34에서 TOGGLE_ON 그대로)

### 작업 4: 공통 부품
- `MorningToggle`(Default 48x28, Small 40x24, 손잡이 `control/knob` + `shadow/knob`, 스프링 미끄러짐, 켤 때 손잡이 늘어남, TOGGLE 진동)
- `MorningButton`(Large 56 `Title/Medium`, Medium 48 `Body/Medium Strong`, Circle 112), 눌림 칠 `primary/pressed`
- `MorningBottomNav`(176x64 알약, `surface/card` + `shadow/float`, 48 아이콘 버튼, 고른 탭 `primary/subtle`)
- Robolectric Compose 테스트 `MorningToggleTest`: 누르면 `onCheckedChange(true)`

### 작업 5: 앱 껍데기
- `:app` `MorningUpApp`(하단 메뉴 3탭, 빈 탭), 디버그 빌드에만 설정 탭에 '견본' 화면(색, 글자, 버튼, 토글)
- 문구는 `strings.xml`

## 미룬 것
- dev-plan 1단계의 'Room, DataStore 틀'은 저장할 모양이 2단계에서 정해져서 2단계 작업 1로 옮겨요.

## 끝 기준
`./gradlew build` 통과, 커밋, push. 폰에서 견본 화면 확인은 아침 확인 목록으로.

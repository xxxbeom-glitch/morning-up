# Q. 알라미 인터랙션 애니메이션 조사

- 대상: 알라미 `droom.sleepIfUCan` 26.43.0 (base.apk와 split 3개, dex 19개)
- 조사일: 2026-11-27
- 방법: `aapt2 dump`로 리소스 목록과 XML 내용, 파이썬으로 Lottie JSON 키프레임 읽기, `ffprobe`로 영상 길이, `dexdump`로 코드 안의 곡선 숫자 찾기
- 그림, 코드, 파일은 우리 프로젝트에 복사하지 않았어요. 방식, 길이, 곡선만 적었어요.

## 결론 5줄

1. 알라미 화면은 거의 다 Compose로 만들어졌고, 움직임은 대부분 **Compose 애니메이션(tween, spring, AnimatedContent)과 Lottie(lottie-compose)** 두 가지로 해요. Rive, MotionLayout 장면 파일, 물리 기반 커스텀 엔진은 쓰지 않아요(MotionLayout, SpringAnimation은 라이브러리에 딸려 들어왔을 뿐 화면 XML에서 쓰는 곳이 없어요).
2. 자체 XML 애니메이션(res/anim)은 15개 정도로 적고 단순해요. 대부분 **300ms 슬라이드, 500ms 페이드, 100ms 눌림/펼침**이에요. 나머지 100개 가까이는 Material, 광고, 동의창 같은 외부 라이브러리 것이에요.
3. 감정을 주는 순간(미션 성공과 실패, 미니게임 START/CLEAR/GAME OVER, 축하 꽃가루, 캐릭터 라미)은 전부 **Lottie 60fps, 1~3초**로 처리해요. Lottie 파일은 약 80개예요.
4. 미션 고르기 화면의 미리보기는 Lottie가 아니라 **짧은 webm 영상(약 3~6초) 12개**로 보여 줘요(수학, 기억, 흔들기, 걸음, 타이핑, 탭탭, 사진, 바코드, 이미지 인식, 스쿼트, 워드비트).
5. 코드에서 가장 많이 보인 곡선은 **cubic-bezier(0.4, 0, 0.2, 1)**(Material 기본), **(0.5, 0, 0, 1)**(강한 감속, 팝 등장), **(0.42, 0, 0.58, 1)**(부드러운 오가기)이고, 진동은 `VibrationEffect.createOneShot`(13곳), `createWaveform`(11곳)과 Compose `HapticFeedback`을 함께 써요.

## 1. 쓰인 애니메이션 라이브러리

| 라이브러리 | 있음 | 메모 |
|---|---|---|
| Lottie (`com.airbnb.lottie`) | 있음 | lottie-compose도 있음. 화면 XML에서 `LottieAnimationView`는 메인 화면 1곳뿐, 나머지는 Compose에서 띄움 |
| dotLottie(.lottie 파일) | 있음 | 파일 형식만 .lottie(압축 묶음). Lottie 라이브러리가 직접 읽음 |
| Rive | 없음 | |
| Compose animation (tween, spring, keyframes, infiniteRepeatable, AnimatedContent) | 있음 | 주력 |
| Compose 공유 요소 전환(SharedTransitionScope) | 있음 | 라이브러리 수준 확인. 쓰는 화면은 코드가 난독화돼 특정 못 함 |
| Compose Pager, Material3 ModalBottomSheet | 있음 | 시트는 M3 기본 동작 |
| MotionLayout (View, Compose) | 클래스만 있음 | res/xml에 MotionScene 없음, 화면 XML에서 안 씀 |
| SpringAnimation, FlingAnimation (dynamicanimation) | 클래스만 있음 | 주 사용처 확인 안 됨 |
| ValueAnimator, ObjectAnimator, ViewPropertyAnimator | 있음 | 옛 View 화면과 광고 쪽 |
| OvershootInterpolator, BounceInterpolator | 있음 | 툴팁(Balloon 라이브러리)에서 사용 |
| RenderEffect(흐림), RuntimeShader(AGSL) | 있음 | 흐림 배경 가능성 |
| Konfetti, Shimmer, Rebound | 없음 | 꽃가루는 Lottie로 처리 |
| ExoPlayer/Media3 | 있음 | 미션 미리보기 webm 재생 |

## 2. 화면별 정리 표

| 화면이나 상황 | 방식 | 길이 | 곡선 | 파일 이름 |
|---|---|---|---|---|
| 알람 울림 배경 | Lottie 반복. 작은 원 29개가 0에서 53~66% 크기로 커졌다 사라지며 35px 위로 떠오름, 투명도 0→100→0 | 30초 한 바퀴, 원 하나 약 3.5초 | 기본 ease(0.17,0.17,0.83,0.83) | `res/raw/anim_dismiss_alarm_background.json` |
| 알람 울림, 아침 인사 | Lottie, 캐릭터 장면 | 1.17초 / 1.4초 | Lottie 내장 | `goodmorning_larmy.lottie`, `goodevening_larmy.lottie`, `morning.lottie`(6.3초) |
| 미션 성공 피드백 | Lottie. 원 배경 0→95%로 튀어나와 85%로 안착(117ms), 체크 75→90→75% 한 번 통통, 작은 입자 6개가 0→150~200%→0으로 퍼지며 180도 회전(500ms), 빛 고리 95→115% 퍼지며 사라짐 | 1.0초 | 원: (0.33,0,0.67,1), 나머지 기본 ease | `mission_feedback_success.lottie` |
| 미션 실패 피드백 | Lottie. X와 원이 80↔70%, 95↔85%로 약 200ms 간격 두 번 움찔, 빛 고리 85→130% | 2.0초 | (0.33,0,0.67,1) | `mission_feedback_failed.lottie` |
| 미니게임 시작/클리어/게임오버 문구 | Lottie(글자 그림). 크기 30%→100%로 팝 등장, 투명도 0→100 직선, 뒤에 96%로 살짝 수축 | 0.85~1.1초(등장 450ms) | **(0.5, 0, 0, 1)** 강한 감속 | `lottie_start.json`, `lottie_clear.json`, `lottie_gameover.lottie` |
| 타일/칸 맞춤 성공 | Lottie. 반짝이 별, 원이 좌우로 튀며 사라짐 | 1.18초(실제 움직임 약 300ms) | (0.33,0,0.67,1) | `tile_success.lottie` |
| 연속 기록 불꽃 | Lottie 반복 | 3~4초 | 내장 | `mission_fire.lottie`, `anim_fire.json` |
| 탭 미션 비상 탭, 레벨업 폭발 | Lottie 폭발 입자 | 0.9초 | 내장 | `emergency_tap.lottie`, `character_level_up_dialog_explosions_effect.lottie` |
| 미션 설정 예제(수학) | Lottie. 손가락 커서가 숫자를 누르는 시연, 밤 테마 따로 있음 | 3.0초 반복 | 내장 | `mission_math_sample.lottie`(일반, `raw-night-v8`) |
| 흔들기 미션 안내 | Lottie. 폰 든 손이 +40°, -20°로 약 220ms마다 흔들림 | 1.75초 | (0.33,0,0.67,1) | `mission_shake_guide.lottie` |
| 미션 미리보기(미션 고르기) | webm 영상 반복 | 2.8~6.0초 | 영상 | `math.webm` 4.3초, `memorize.webm` 4.4초, `shake.webm` 4.3초, `step.webm` 2.8초, `typing.webm` 3.6초, `taptap.webm` 4.3초, `photo.webm` 3.9초, `barcode.webm` 6.0초, `image_recognition.webm` 4.1초, `mission_squat_onboarding.webm` 4.5초, `wordbeat_preview.webm` 4.1초 |
| AI 사진 미션 | Lottie. 초점 틀, 스캔 줄, 플래시 안내 | 1.5초 / 2.3초 / 4.2초 | 내장 | `mission_ai_scan.json`, `mission_ai_foucs.json`, `mission_ai_flash_nudge.json` |
| 바코드 인식 효과 | Lottie | 1.17초 | 내장 | `barcode_effect.json` |
| 탭 유도 손가락 | Lottie. 손가락이 106%x94%로 눌렸다(250ms) 돌아옴(200ms), 3° 기울어짐, 1초마다 반복 | 2.0초 | 눌림 (0.32,0,0.67,0) 가속, 복귀 (0.33,1,0.68,1) 감속 | `finger_touch.lottie`, `lottie_finger_touch.lottie` |
| 위로 가리키는 손 | Lottie. 22px 위아래 콩콩 두 번 | 0.31초 | 직선 | `lottie_hand_pointing_up.json` |
| 스크롤/스와이프 유도 화살표 | Lottie. 아래로 22px 이동, 좌우 왕복 | 2.5초 / 2.7초 | (0.12,0,0.25,1) 등 감속 | `arrow_down.lottie`, `up_arrow.lottie` |
| 온보딩 "알람 끄기" 설명 | Lottie(그림 5장 포함) | 6.0초 | 내장 | `assets/onboarding_turn-off-alarm.lottie` |
| 온보딩 완료, 무료체험 완료 | Lottie 꽃가루(가로 전체) | 3.0초 | 내장 | `lottie_free_trial_onboarding_done.json`, `anim_congratulations_reward.json`(같은 내용) |
| 축하 시작, 박수 | Lottie 꽃가루 / 박수와 입자 8개 | 4.0초 / 1.0초 | 박수 입자 (0.17,0.17,0.14,1) 강한 감속 | `celebrations_begin.lottie`, `lottie_celebrations_begin.json`, `clap.lottie` |
| 축하, 보상 | Lottie 불꽃놀이, 트로피, 동전 뒤집기, 3D 돈, 별빛 | 2.0~5.0초 | 내장 | `lottie_fireworks.json` 4초, `lottie_trophy.lottie` 2.3초, `lottie_coin_flip.json` 2초, `lottie_3d_money.json` 2.7초, `anim_spotlight_reward.json` 5초 |
| 프리미엄 알람 완료 | Lottie 터지는 효과 | 1.67초 | 내장 | `lottie_premium_alarm_done.json` |
| 캐릭터 라미 단계 1~6, 퀘스트 먹이주기 | Lottie 캐릭터 반복 | 1.0~4.5초 | 내장 | `character_larmy_stage_1~6.lottie`, `ani_feeding_lv1/2.lottie`, `quest_character_*.lottie`, `level1base.lottie` 등 |
| 라미 스토리(전체 화면 이야기) 열고 닫기 | XML 이동. 아래 100%에서 0으로 올라옴 / 내려감 | 300ms | 올라올 때 감속(decelerate), 내려갈 때 가속(accelerate) | `res/anim/larmy_story_slide_in_up.xml`, `larmy_story_slide_out_down.xml`, `larmy_story_hold.xml` |
| 결제 유도 배너 등장/퇴장 | XML 이동. 화면 아래에서 올라옴 | 500ms | 기본 | `res/anim/store_promo_appear_anim.xml`, `store_promo_disappear_anim.xml` |
| 작은 팝업(드롭다운류) 펼침/접힘 | XML. 아래 기준 세로 크기 0→1 + 투명도 0.5→1 / 반대로 1→0 | 100ms | 펼침 감속, 접힘 가속 | `res/anim/slide_up_and_fade_in.xml`, `slide_down_and_fade_out.xml` |
| 작은 요소 튀어나옴 | XML. 가운데 기준 크기 0→1 | 100ms | 직선 | `res/anim/scale_up.xml` |
| 화면 좌우 전환(옛 화면) | XML 이동. 좌우 100% | 300ms | 직선 | `res/anim/slide_in_left/right.xml`, `slide_out_left/right.xml` |
| 전면 팝업 광고 등장 | XML 페이드 0→1 | 500ms | 감속 | `res/anim/light_popup_ad_dialog_enter.xml` |
| 서서히 나타남 | XML 페이드 0.1→1 | 500ms | 가속 후 감속 | `res/anim/fade_in.xml` |
| 내려가며 사라짐 | XML. 40% 아래로 + 투명도 1→0 | 500ms | 감속 | `res/anim/slide_out_down.xml` |
| 펼침 화살표(셰브런) | AnimatedVector 회전 | Material 기본 | Material 기본 | `res/animator/chevron_checked_unchecked.xml`, `chevron_unchecked_checked.xml` |
| 말풍선 툴팁 | Balloon 라이브러리. 등장 50→100% 크기(250ms, 튕김 또는 넘침), 강조는 100↔90% 반복(800ms), 흔들기 650ms 반복 | 250~800ms | Bounce, Overshoot, 직선 | `res/anim/balloon_*.xml` |
| 바텀시트 | Compose M3 `ModalBottomSheet`가 주력(기본 spring). 옛 View 시트는 20% 아래에서 올라오며 페이드 | View 시트 400ms 등장, 350ms 퇴장 | M3 emphasized | `m3_bottom_sheet_slide_in/out.xml`(라이브러리) |
| 수면 화면 | Lottie 배경, 소리 막대, 추적 파형 | 12초 / 2초 / 20초 | 내장 | `anim_sleep_mode_background.json`, `anim_sound_bar_*.json`, `anim_sleep_tracking.json` |
| 로딩 | Lottie 작은 원형 | 1.3초 | 내장 | `anim_spinner.json`, `ani_spinningvolt.lottie` |
| 리포트 카드 설명 | Lottie(한국어/영어 따로) | 4.0~7.0초 | 내장 | `report_sleep_*`, `report_wakeup_*` |
| 결제 화면 배지, 로고 | Lottie 작은 반복 | 2~3초 | 내장 | `assets/badge_*.json`, `subs_*.json`, `subs_sheet_multiple.lottie` |

### 토글, 드럼롤, 탭 전환, 버튼 눌림

- 별도 XML이나 Lottie가 없어요. Compose 코드 안에서 처리하는 것으로 보여요. 코드 이름이 난독화돼 어느 화면 값인지는 특정하지 못했어요.
- 대신 앱 코드(라이브러리 밖으로 추정)에서 찾은 숫자는 아래와 같아요. 참고값으로만 보세요.

| 종류 | 값 |
|---|---|
| tween 길이와 지연 | 200ms, 250ms(지연 100), 300ms(지연 300), 400ms(지연 200), 800ms(지연 200), 1000ms, 1500ms |
| CubicBezier | (0.4,0,0.2,1) 가장 많음, (0.5,0,0,1), (0.42,0,0.58,1), (0.42,0,1,1), (0.5,0,1,1), (0.65,0,0.35,1), 넘침 곡선 (0.45,1.45,0.8,1) 1곳 |
| PathInterpolator(옛 View 쪽) | (0.2,0,0.3,1) 5곳으로 가장 많음, (0.2,0,0.4,1), (0,0.2,0.3,1) |
| 리소스 기본값 | 화면 전환 150ms, 시트 슬라이드 150ms, 툴팁 150ms |

## 3. MotionLayout, MotionScene

- `res/xml`에 MotionScene 파일이 없어요(16개 모두 설정, 위젯, 보안 파일).
- 화면 XML 731개 중 MotionLayout을 쓰는 곳도 없어요. dex에 클래스만 들어 있어요.

## 4. 진동(햅틱)

| 단서 | 내용 |
|---|---|
| `VibrationEffect.createOneShot` | 13곳. 한 번 짧게 울리는 진동 |
| `VibrationEffect.createWaveform` | 11곳. 패턴 진동(알람 진동 패턴으로 추정) |
| Compose `HapticFeedback`, `LocalHapticFeedback` | 있음. 누를 때 짧은 진동 |
| `performHapticFeedback`, `HapticFeedbackConstantsCompat` | 있음 |
| 문자열 | `PerformHaptic(type=`, `haptic_error`, `hapticFeedbackEnabled`, `Vibrate(period=100)`, `Vibrating with intensity:` |
| 디자인 시스템 | `com.alarmy.design.ui.haptic.AdsHapticType` 종류 구분이 있음. 웹 화면 쪽 연결 코드에서 `perform(AdsHapticType)`으로 부름 |
| 설정 이름 | `isVibrateOn`, `onVibrationToggle`, `TapVibrationCheckBox`, `vibration_only` |

- 정리하면, 알라미는 진동을 디자인 시스템 수준에서 종류별로 나눠 두고(`AdsHapticType`), 실패를 따로 구분해요(`haptic_error`). 웹 화면도 같은 진동을 부를 수 있어요.

## 우리 앱에 참고할 만한 것 (추천 순서)

1. **미션 성공 피드백 구성**: 원이 95%까지 튀었다 85%로 안착(약 120ms), 체크가 75→90→75%로 한 번 통통, 작은 입자 6개가 500ms 동안 퍼지며 사라짐, 전체 1초. 우리 미션 완료 화면의 "짧고 확실한 보상" 길이 기준으로 좋아요.
2. **팝 등장 곡선 (0.5, 0, 0, 1), 450ms, 크기 30%→100%**: START, CLEAR 문구에 쓰여요. 미션 시작 안내나 "깼어요" 확인 문구 등장에 그대로 맞아요.
3. **실패는 작게 두 번 움찔**: 80↔70% 크기로 약 200ms 간격 두 번. 틀린 답에 화면 흔들기보다 덜 공격적이고 진동(`haptic_error` 같은 실패용 진동)과 짝을 짓기 좋아요.
4. **울림 배경은 아주 느린 반짝임**: 작은 점이 3.5초에 걸쳐 커졌다 사라지며 떠오르고, 30초 한 바퀴로 반복. 우리 하늘 배경(Ring Sky)에 별이나 빛 입자를 넣을 때 속도 기준으로 써요. 울림 화면을 단순하게 두는 우리 규칙과도 맞아요.
5. **누름 유도 손가락의 눌림 크기 106%x94%**: 눌릴 때 가속 250ms, 돌아올 때 감속 200ms. 버튼 눌림 크기는 이보다 작게(예: 96~97%) 하되 "누를 때 가속, 뗄 때 감속" 짝은 그대로 참고해요.
6. **전체 화면 시트 올라오기 300ms, 감속 / 내려가기 300ms, 가속**: 라미 스토리 방식. 우리 미션 시트나 설정 페이지 여닫기 기본값으로 무난해요. 작은 바텀시트는 Compose M3 기본 spring을 그대로 쓰는 것이 알라미와 같아요.
7. **미션 미리보기는 짧은 반복 영상(3~5초)**: 알라미는 Lottie 대신 webm을 써요. 우리 미션 시트 미리보기를 나중에 움직이게 할 때, 실제 화면 녹화 3~5초 반복이 만들기 쉽고 이해도 빨라요.
8. **작은 팝업 펼침 100ms**: 아래 기준 세로 0→1 + 투명도 0.5→1. 드롭다운이나 작은 메뉴는 이 정도로 짧게 해야 답답하지 않아요.
9. **기본 곡선 하나로 통일**: 알라미 코드에서도 (0.4, 0, 0.2, 1)이 가장 많아요. 우리도 기본은 이 곡선, 강조 등장만 (0.5, 0, 0, 1)로 두 가지만 쓰면 일관돼요.
10. **진동을 종류별 이름으로 정리**: 알라미처럼 "누름, 성공, 실패" 같은 진동 종류를 디자인 규칙에 이름으로 정해 두면 화면마다 진동이 들쭉날쭉하지 않아요.

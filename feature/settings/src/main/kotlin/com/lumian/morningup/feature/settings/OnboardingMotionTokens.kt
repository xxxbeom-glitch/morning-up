package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color

/**
 * 온보딩 그림(O2-1~O2-4, O3)의 피그마 값. 모두 피그마 dp 그대로예요(11-20 사용자 지시 "원래 설정된 화면의 애니메이션도 같이 반영").
 * 출처: 피그마 `MazZLYfaQndMh9AxlRcz2v`의 `Motion Waves 4`(643:6974), `Motion Icon Shake`(625:7608),
 * `Motion Fullscreen`(632:7251), `Motion Battery Dialog`(853:7426),
 * `Motion Overlay Toggle`(853:7119), `Motion Weather Cloud`(876:7135), `Motion Weather Night`(877:7151),
 * 화면 O2-1(743:23090), O2-2(743:23109), O2-3(743:23128), O2-4(755:25058), O3(756:13398).
 */
internal data class FigmaRect(val x: Float, val y: Float, val w: Float, val h: Float)

internal object OnboardingMotionTokens {
    // 그림 칸: 좌우 16, 328x280. 그림 칸과 글 묶음은 한 덩어리(408)로 화면 세로 가운데에 놓여요. 움직임 부품은 328x360이고 칸보다 40 위에서 시작해요.
    const val SLOT_WIDTH = 328f
    const val SLOT_HEIGHT = 280f
    val Motion = FigmaRect(0f, -40f, 328f, 360f)

    // 피그마 이징. Ease out, Ease in and out, Gentle 스프링(질량 1, 강성 100, 감쇠 15).
    val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    const val GENTLE_STIFFNESS = 100f
    const val GENTLE_DAMPING_RATIO = 0.75f

    // ---- Motion Waves 4: 원 4개. 가운데 (164, 180). 숨은 원은 80% 크기, 진하기 0.
    const val WAVE_CENTER_X = 164f
    const val WAVE_CENTER_Y = 180f
    val WaveSizes = listOf(200f, 280f, 360f, 440f)
    val WaveAlphas = listOf(0.2f, 0.12f, 0.07f, 0.035f)
    const val WAVE_HIDDEN_SCALE = 0.8f
    const val WAVE_START_WAIT = 100L
    const val WAVE_GROW = 450
    const val WAVE_GAP = 10L
    const val WAVE_HOLD = 300L
    const val WAVE_FADE = 600

    // ---- Motion Icon Shake(Type=알림 앱아이콘, 956:7343~7399, 11-21 확정): 앱 아이콘 130(스쿼클 모서리 30, 부드럽게 60%)
    // + 배지 24가 아이콘 오른쪽 위에 걸쳐요. 칸 전체가 (164, 180)을 축으로 ±8도 흔들려요.
    // 안드로이드엔 부드러운 모서리(smoothing)가 없어서 가장 가까운 둥근 모서리 30으로 잘라요.
    val IconCard = FigmaRect(99f, 115f, 130f, 130f)
    const val ICON_CARD_RADIUS = 30f
    val IconBadge = FigmaRect(209f, 109f, 24f, 24f)
    // 배지 색은 톤다운(11-21): 라이트 #BE393D, 다크 #F26262.
    val IconBadgeLight = Color(0xFFBE393D)
    val IconBadgeDark = Color(0xFFF26262)
    const val SHAKE_WAIT = 450L
    const val SHAKE_STEP = 60
    const val SHAKE_GAP = 10L
    const val SHAKE_DEGREES = 8f
    const val SHAKE_COUNT = 8

    // ---- Motion Fullscreen: O2-2에서는 75%(246x270)로 칸의 (41, 5)에 놓여요.
    val FullscreenPlace = FigmaRect(41f, 5f, 246f, 270f)
    const val FULLSCREEN_SCALE = 0.75f
    val Phone = FigmaRect(76f, 20f, 176f, 320f)
    const val PHONE_RADIUS = 32f
    val PhoneBezel = FigmaRect(3f, 3f, 170f, 314f)
    const val PHONE_BEZEL_RADIUS = 29f
    val PhoneScreen = FigmaRect(9f, 9f, 158f, 302f)
    const val PHONE_SCREEN_RADIUS = 23f
    val PhoneIsland = FigmaRect(63f, 16f, 50f, 15f)
    val PhonePower = FigmaRect(174f, 78f, 5f, 46f)
    val PhoneVolUp = FigmaRect(-3f, 92f, 5f, 30f)
    val PhoneVolDown = FigmaRect(-3f, 130f, 5f, 30f)
    const val PHONE_BUTTON_RADIUS = 2f
    // 화면 안(화면 기준 좌표).
    val PhoneLock = FigmaRect(72f, 48f, 14f, 14f)
    val PhoneClock = FigmaRect(0f, 68f, 158f, 43f)
    const val PHONE_CLOCK_SIZE = 36f
    const val ALARM_FROM_Y = 48f
    val AlarmDate = FigmaRect(0f, 80f, 158f, 12f)
    const val ALARM_DATE_SIZE = 10f
    val AlarmTime = FigmaRect(0f, 96f, 158f, 48f)
    const val ALARM_TIME_SIZE = 40f
    val AlarmButton = FigmaRect(47f, 158f, 64f, 64f)
    const val ALARM_BUTTON_TEXT_SIZE = 11f
    const val PHONE_SHAKE_PX = 3f
    const val PHONE_SHAKE_COUNT = 9
    const val FULL_LOCK_HOLD = 1_200L
    const val FULL_ALARM_WAIT = 350L
    const val FULL_ALARM_RISE = 150
    const val FULL_SHAKE_WAIT = 250L
    const val FULL_SHAKE_STEP = 40
    const val FULL_SHAKE_GAP = 10L
    const val FULL_SHAKE_PAUSE = 450L
    const val FULL_END_HOLD = 1_200L
    const val FULL_DISSOLVE = 400

    // 피그마에 원색으로 칠한 폰 부품과 알람 화면(변수에 연결되지 않은 값).
    val PhoneBezelColor = Color(0xFFC6CBD4)
    val PhoneButtonColor = Color(0xFFB4BAC6)
    val PhoneScreenColor = Color(0xFF09090B)
    val PhoneIslandColor = Color(0xFF000000)
    val AlarmTop = Color(0xFFF2555A)
    val AlarmBottom = Color(0xFFB3242A)

    // ---- Motion Battery Dialog(O2-3): 온보딩 전용 배터리 최적화 해제 대화 상자. 부품(328x360) 기준 좌표예요.
    // 등장: 16 아래(y 107), 투명 0 → 0.2초 뒤 GENTLE 0.6초. 반복: 0.6초 → '허용' 누름 EASE_OUT 0.15초 → 0.1초 →
    // 닫힘(12 아래, 투명) GENTLE 0.5초 → 0.8초 → 다시 뜸 GENTLE 0.5초.
    const val DIALOG_X = 24f
    const val DIALOG_Y = 91f
    const val DIALOG_ENTER_Y = 107f
    const val DIALOG_CLOSED_Y = 103f
    const val DIALOG_WIDTH = 280f
    const val DIALOG_HEIGHT = 180f
    const val DIALOG_RADIUS = 20f
    // 대화 상자 안(대화 상자 기준 좌표).
    val DialogTitle = FigmaRect(20f, 24f, 240f, 24f)
    const val DIALOG_TITLE_SIZE = 16f
    val DialogBody = FigmaRect(20f, 54f, 240f, 54f)
    const val DIALOG_BODY_SIZE = 13f
    const val DIALOG_BODY_LINE = 18f
    val DialogDeny = FigmaRect(0f, 133f, 139.5f, 47f)
    val DialogAllow = FigmaRect(140.5f, 133f, 139.5f, 47f)
    const val DIALOG_BUTTON_SIZE = 14f
    val DialogDividerH = FigmaRect(0f, 132f, 280f, 1f)
    val DialogDividerV = FigmaRect(139.5f, 133f, 1f, 47f)
    const val DIALOG_TAP_CENTER_X = 210.3f
    const val DIALOG_TAP_CENTER_Y = 156.5f
    const val DIALOG_START_WAIT = 600L
    const val DIALOG_PRESS = 150
    const val DIALOG_PRESS_HOLD = 100L
    const val DIALOG_CLOSE = 500L
    const val DIALOG_CLOSED_HOLD = 800L
    const val DIALOG_REOPEN = 500L

    // O2-3, O2-4 등장: 0.2초 쉬고 GENTLE 0.6초로 16 아래에서 떠오르며 나타나요.
    const val ENTER_WAIT = 200L
    const val ENTER_RISE = 600L
    const val ENTER_FROM = 16f

    // 누름 원(O2-3, O2-4 같음): 36에서 44로 커지며 사라져요. 진하기 18%(text/primary).
    const val TAP_FROM = 36f
    const val TAP_TO = 44f
    const val TAP_ALPHA = 0.18f

    // ---- Motion Overlay Toggle(O2-4): 앱 이름 카드 280x80 + 스위치. 부품(328x360) 기준 좌표예요.
    // 반복: 0.6초 → 누름 EASE_OUT 0.15초 → 0.1초 → 켜짐 GENTLE 0.5초 → 1.6초 → 처음 장면으로 DISSOLVE 0.4초.
    val ToggleCard = FigmaRect(24f, 140f, 280f, 80f)
    const val TOGGLE_CARD_RADIUS = 20f
    val ToggleIcon = FigmaRect(40f, 162f, 36f, 36f)
    const val TOGGLE_ICON_RADIUS = 10f
    const val TOGGLE_ICON_ALPHA = 0.45f
    val ToggleLabel = FigmaRect(88f, 161f, 150f, 20f)
    const val TOGGLE_LABEL_SIZE = 14f
    const val TOGGLE_SUB_X = 88f
    const val TOGGLE_SUB_W = 150f
    const val TOGGLE_SUB_H = 18f
    const val TOGGLE_SUB_OFF_Y = 182f
    const val TOGGLE_SUB_ON_Y = 186f
    const val TOGGLE_SUB_SHIFT = 4f
    const val TOGGLE_SUB_SIZE = 13f
    val ToggleTrack = FigmaRect(248f, 168f, 40f, 24f)
    const val TOGGLE_TRACK_RADIUS = 12f
    const val TOGGLE_KNOB_Y = 171f
    const val TOGGLE_KNOB_OFF_X = 251f
    const val TOGGLE_KNOB_ON_X = 267f
    const val TOGGLE_KNOB_SIZE = 18f
    const val TOGGLE_TAP_CENTER_X = 268f
    const val TOGGLE_TAP_CENTER_Y = 180f
    const val TOGGLE_START_WAIT = 600L
    const val TOGGLE_PRESS = 150
    const val TOGGLE_PRESS_HOLD = 100L
    const val TOGGLE_SWITCH = 500L
    const val TOGGLE_END_HOLD = 1_600L
    const val TOGGLE_DISSOLVE = 400

    // ---- Motion Weather Cloud(O3 라이트): 그림 칸(328x280) 기준 좌표예요. 카드 없이 배경 위에 그려요.
    // 등장(앱은 피그마의 절반, 11-20): 구름 16 아래 투명 → 0.1초 뒤 0.3초 → 0.05초 뒤 해와 빛이 40 아래에서 EASE_OUT 0.4초.
    // 반복: 구름만 y 92 ↔ 86, EASE_IN_AND_OUT 1.8초씩.
    const val CLOUD_Y = 92f
    const val CLOUD_UP_Y = 86f
    const val CLOUD_ENTER_Y = 108f
    val CloudBase = FigmaRect(89f, 48f, 150f, 50f)
    const val CLOUD_BASE_RADIUS = 25f
    val CloudPuffSmall = FigmaRect(107f, 20f, 64f, 64f)
    val CloudPuffBig = FigmaRect(143f, 0f, 84f, 84f)
    const val SUN_CENTER_X = 210f
    const val SUN_CENTER_Y = 100f
    const val SUN_RISE = 40f
    const val SUN_SIZE = 64f
    const val SUN_HALO_SIZE = 160f
    const val SUN_HALO_BLUR = 40f
    const val SUN_HALO_ALPHA = 0.35f
    const val SUN_INNER_SIZE = 100f
    const val SUN_INNER_BLUR = 18f
    const val SUN_INNER_ALPHA = 0.5f
    const val CLOUD_ENTER_WAIT = 100L
    const val CLOUD_ENTER = 300L
    const val SUN_WAIT = 50L
    const val SUN_ENTER = 400
    const val FLOAT_STEP = 1_800
    const val FLOAT_GAP = 10L

    // 피그마 선형 그라데이션(왼쪽 위 → 오른쪽 아래)의 시작과 끝(크기 대비).
    const val GRADIENT_FROM = 0.15f
    const val GRADIENT_TO = 0.85f

    // ---- Motion Weather Night(O3 다크): 구름 없이 별 6개와 노란 초승달 108. 그림 칸 기준 좌표예요.
    // 등장(앱은 피그마의 절반, 11-20): 0.1초 뒤 별 1, 3, 5 0.25초 → 0.025초 뒤 별 2, 4, 6 0.25초 → 0.05초 뒤 달이 30 아래에서 EASE_OUT 0.4초.
    // 반복: 달이 가운데를 축으로 +8도 ↔ -8도(피그마 -8 ↔ +8), 별은 반짝임, EASE_IN_AND_OUT 1.8초씩.
    val Stars = listOf(
        FigmaRect(65f, 65f, 10f, 10f),
        FigmaRect(258f, 60f, 8f, 8f),
        FigmaRect(97f, 37f, 6f, 6f),
        FigmaRect(282.5f, 146.5f, 7f, 7f),
        FigmaRect(45f, 157f, 6f, 6f),
        FigmaRect(237f, 207f, 6f, 6f),
    )
    val StarAlphaDown = listOf(0.9f, 0.4f, 0.7f, 0.3f, 0.8f, 0.35f)
    val StarAlphaUp = listOf(0.35f, 0.9f, 0.3f, 0.8f, 0.4f, 0.75f)
    const val STAR_INNER = 0.38f
    const val STAR_POINTS = 4
    const val MOON_CENTER_X = 164f
    const val MOON_CENTER_Y = 140f
    const val MOON_SIZE = 108f
    const val MOON_RISE = 30f
    // 초승달: 지름 108 원에서 (33.4, -15.4)의 지름 97.7 원을 빼요.
    const val MOON_CUT_X = 33.4f
    const val MOON_CUT_Y = -15.4f
    const val MOON_CUT_SIZE = 97.7f
    const val MOON_SWING = 8f
    const val STAR_ENTER_WAIT = 100L
    const val STAR_ENTER = 250L
    const val STAR_GAP = 25L
    const val MOON_WAIT = 50L
    const val MOON_ENTER = 400

    // O3 등장만 빠르게(11-20 사용자 요청 "더 빨리"): 등장 시간을 피그마의 절반으로 줄였어요.
    // GENTLE 용수철은 단단함을 4배로 하면 걸리는 시간이 절반이 돼요(0.6초 → 0.3초, 0.5초 → 0.25초).
    const val WEATHER_ENTER_STIFFNESS = 400f
}

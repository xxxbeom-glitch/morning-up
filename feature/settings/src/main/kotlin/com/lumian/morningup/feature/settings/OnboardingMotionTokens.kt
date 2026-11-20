package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color

/**
 * 온보딩 그림(O2-1~O2-3)의 피그마 값. 모두 피그마 dp 그대로예요(11-20 사용자 지시 "원래 설정된 화면의 애니메이션도 같이 반영").
 * 출처: 피그마 `MazZLYfaQndMh9AxlRcz2v`의 `Motion Waves 4`(643:6974), `Motion Icon Shake`(625:7608),
 * `Motion Fullscreen`(632:7251), `Motion Battery Path`(666:7746), 화면 O2-1(743:23090), O2-2(743:23109), O2-3(743:23128).
 */
internal data class FigmaRect(val x: Float, val y: Float, val w: Float, val h: Float)

internal object OnboardingMotionTokens {
    // 그림 칸: 상태 바 아래 20(피그마 y72 - 52), 좌우 16, 328x280. 움직임 부품은 328x360이고 칸보다 40 위에서 시작해요.
    const val SLOT_TOP = 20f
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

    // ---- Motion Icon Shake: 파란 앱 칸 104(모서리 26) + 빨간 배지 24. 칸 전체가 (164, 180)을 축으로 ±8도 흔들려요.
    val IconCard = FigmaRect(112f, 128f, 104f, 104f)
    const val ICON_CARD_RADIUS = 26f
    val IconBadge = FigmaRect(196f, 122f, 24f, 24f)
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
    val AlarmButtonText = Color(0xFFB3242A)

    // ---- Motion Battery Path: 설정 '제한 없음' 카드 한 줄. 부품(328x360, 자르기 켬) 기준 좌표예요.
    val BatteryCard = FigmaRect(51f, 146f, 225.8f, 68f)
    const val BATTERY_CARD_RADIUS = 20f
    val BatteryIcon = FigmaRect(67f, 162f, 36f, 36f)
    const val BATTERY_ICON_RADIUS = 10f
    const val BATTERY_ICON_ALPHA = 0.45f
    val BatteryLabel = FigmaRect(115f, 161f, 100f, 20f)
    const val BATTERY_LABEL_SIZE = 14f
    val BatterySub = FigmaRect(115f, 182f, 100f, 18f)
    const val BATTERY_SUB_SIZE = 13f
    val BatteryTrack = FigmaRect(221f, 168f, 40f, 24f)
    const val BATTERY_TRACK_RADIUS = 12f
    const val BATTERY_KNOB_Y = 171f
    const val BATTERY_KNOB_OFF_X = 224f
    const val BATTERY_KNOB_ON_X = 240f
    const val BATTERY_KNOB_SIZE = 18f
    const val BATTERY_TAP_CENTER_X = 241f
    const val BATTERY_TAP_CENTER_Y = 180f
    const val BATTERY_TAP_FROM = 36f
    const val BATTERY_TAP_TO = 44f
    const val BATTERY_TAP_ALPHA = 0.18f
    const val BATTERY_START_WAIT = 600L
    const val BATTERY_PRESS = 150
    const val BATTERY_PRESS_HOLD = 100L
    const val BATTERY_SWITCH = 500L
    const val BATTERY_END_HOLD = 1_600L
    const val BATTERY_DISSOLVE = 400
}

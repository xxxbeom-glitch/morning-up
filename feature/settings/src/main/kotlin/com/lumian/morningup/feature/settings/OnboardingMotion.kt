package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.theme.LocalMorningColors
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.Pretendard
import com.lumian.morningup.core.designsystem.theme.lightMorningColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lumian.morningup.feature.settings.OnboardingMotionTokens as T

/** 시험에서 그림 칸을 찾는 이름. */
internal const val PICTURE_TAG_PREFIX = "onboarding-picture-"

/**
 * 온보딩 권한 장 위의 그림 칸(328x280). 피그마에 정해 둔 그림과 움직임을 그대로 그려요.
 * O2-1 알림: 퍼지는 원 + 흔들리는 앱 칸. O2-2 전체 화면 알림: 퍼지는 원 + 잠금 화면 위로 알람이 올라오는 폰.
 * O2-3 배터리: 온보딩 전용 대화 상자에서 '허용'을 눌러요. O2-4 다른 앱 위에 표시: 앱 이름 카드의 스위치를 켜요.
 * O3 날씨: 라이트는 구름과 해, 다크는 별과 초승달(카드 없이 배경 위에).
 * 원은 칸 밖으로 나가도 잘리지 않아요(피그마 자르기 꺼짐). 폰의 '애니메이션 줄이기'가 켜져 있으면 끝 장면에서 멈춰 있어요.
 */
@Composable
internal fun OnboardingPicture(step: OnboardingStep, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(T.SLOT_WIDTH.dp, T.SLOT_HEIGHT.dp)
            .testTag(PICTURE_TAG_PREFIX + step.name),
    ) {
        when (step) {
            OnboardingStep.Notifications -> Box(Modifier.figma(T.Motion)) {
                MotionWaves(Modifier.figma(FullMotion))
                MotionIconShake(Modifier.figma(FullMotion))
            }
            OnboardingStep.FullScreen -> {
                MotionWaves(Modifier.figma(T.Motion))
                // 부품(328x360)을 75%로 줄여 칸의 (41, 5)에 246x270으로 놓아요.
                // 폰 목업은 다크에서도 라이트 색이에요(피그마 다크 O2-2 폰은 Light 모드 고정, 11-20).
                LightModal {
                    Box(Modifier.figma(T.FullscreenPlace)) {
                        MotionFullscreen(
                            Modifier
                                .figma(ScaledFullscreen)
                                .graphicsLayer {
                                    scaleX = T.FULLSCREEN_SCALE
                                    scaleY = T.FULLSCREEN_SCALE
                                },
                        )
                    }
                }
            }
            // O2-3, O2-4는 다크에서도 라이트 모달 색으로 그려요(11-20 사용자 의견 "눈에 잘 안 들어온다").
            OnboardingStep.Battery -> LightModal { MotionBatteryDialog(Modifier.figma(T.Motion)) }
            OnboardingStep.Overlay -> LightModal { MotionOverlayToggle(Modifier.figma(T.Motion)) }
            OnboardingStep.Location -> MotionWeather(Modifier.matchParentSize())
        }
    }
}

/** 다크 테마에서도 라이트 색으로 그려요(O2-2 폰, O2-3 대화 상자, O2-4 토글 카드). */
@Composable
private fun LightModal(content: @Composable () -> Unit) {
    val light = remember { lightMorningColors() }
    CompositionLocalProvider(LocalMorningColors provides light, content = content)
}

private val FullMotion = FigmaRect(0f, 0f, T.Motion.w, T.Motion.h)

/** 줄이기 전 부품을 246x270 칸 가운데에 놓으면 (41, 45)만큼 밖으로 나가요. */
private val ScaledFullscreen = FigmaRect(
    x = (T.FullscreenPlace.w - T.Motion.w) / 2,
    y = (T.FullscreenPlace.h - T.Motion.h) / 2,
    w = T.Motion.w,
    h = T.Motion.h,
)

/**
 * 부모 안 (x, y)에 w x h로 놓아요(피그마 좌표 그대로). 부모보다 커도 줄이지 않고, 넘치는 부분은 자르지 않아요.
 */
internal fun Modifier.figma(r: FigmaRect): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints.fixed(r.w.dp.roundToPx(), r.h.dp.roundToPx()))
    layout(constraints.constrainWidth(placeable.width), constraints.constrainHeight(placeable.height)) {
        placeable.place(r.x.dp.roundToPx(), r.y.dp.roundToPx())
    }
}

/** 피그마 글자 크기(dp)를 폰 글자 크기 설정과 상관없이 그대로 써요. 그림 속 글자라 커지면 폰 밖으로 나가요. */
@Composable
internal fun fixedSp(size: Float): TextUnit = with(LocalDensity.current) { size.dp.toSp() }

@Composable
internal fun FigmaText(
    text: String,
    rect: FigmaRect,
    size: Float,
    weight: FontWeight,
    color: Color,
    alignment: Alignment = Alignment.Center,
    lineHeight: Float? = null,
) {
    Box(Modifier.figma(rect), contentAlignment = alignment) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Pretendard,
                fontWeight = weight,
                fontSize = fixedSp(size),
                lineHeight = lineHeight?.let { fixedSp(it) } ?: TextUnit.Unspecified,
                color = color,
            ),
            textAlign = if (alignment == Alignment.Center) TextAlign.Center else TextAlign.Start,
            // 줄은 문구 안 줄바꿈으로만 나눠요(피그마처럼 낱말 단위로 정해 둔 줄).
            softWrap = false,
        )
    }
}

// ---------------------------------------------------------------- Motion Waves 4

/**
 * 원 4개가 안쪽부터 하나씩 커지며 나타나고(0.1초 기다림, 원마다 0.45초 Ease out, 사이 0.01초),
 * 0.3초 쉰 뒤 0.6초 동안 함께 80% 크기로 줄며 사라져요. 한 바퀴 약 2.8초.
 */
@Composable
private fun MotionWaves(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val rings = remember { List(T.WaveSizes.size) { Animatable(0f) } }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            rings.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        rings.forEach { it.snapTo(0f) }
        while (true) {
            delay(T.WAVE_START_WAIT)
            rings.forEachIndexed { index, ring ->
                if (index > 0) delay(T.WAVE_GAP)
                ring.animateTo(1f, tween(T.WAVE_GROW, easing = T.EaseOut))
            }
            delay(T.WAVE_HOLD)
            coroutineScope {
                rings.forEach { ring -> launch { ring.animateTo(0f, tween(T.WAVE_FADE, easing = T.EaseInOut)) } }
            }
        }
    }
    val color = MorningTheme.colors.primaryDefault
    Canvas(modifier) {
        val center = Offset(T.WAVE_CENTER_X.dp.toPx(), T.WAVE_CENTER_Y.dp.toPx())
        // 피그마 순서대로 큰 원이 아래에 깔려요.
        for (index in rings.indices.reversed()) {
            val shown = rings[index].value
            if (shown <= 0f) continue
            val diameter = T.WaveSizes[index] * lerp(T.WAVE_HIDDEN_SCALE, 1f, shown)
            drawCircle(
                color = color.copy(alpha = T.WaveAlphas[index] * shown),
                radius = (diameter / 2).dp.toPx(),
                center = center,
            )
        }
    }
}

// ---------------------------------------------------------------- Motion Icon Shake

/** 앱 아이콘(시계)과 빨간 배지. 0.45초 쉬고, 0.06초씩 ±8도로 8번 흔든 뒤 0.06초 만에 제자리로 와요. */
@Composable
private fun MotionIconShake(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val angle = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        angle.snapTo(0f)
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(T.SHAKE_WAIT)
            repeat(T.SHAKE_COUNT) { index ->
                if (index > 0) delay(T.SHAKE_GAP)
                // 피그마는 -8도(시계 방향)부터 시작해요. Compose 회전은 시계 방향이 +예요.
                val target = if (index % 2 == 0) T.SHAKE_DEGREES else -T.SHAKE_DEGREES
                angle.animateTo(target, tween(T.SHAKE_STEP, easing = T.EaseInOut))
            }
            delay(T.SHAKE_GAP)
            angle.animateTo(0f, tween(T.SHAKE_STEP, easing = T.EaseOut))
        }
    }
    val colors = MorningTheme.colors
    Box(modifier.graphicsLayer { rotationZ = angle.value }) {
        // 확정 앱 아이콘 그림(258beaf의 Clock Day 원본)을 그대로 써요.
        Image(
            painter = painterResource(R.drawable.onboarding_app_icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .figma(T.IconCard)
                .clip(RoundedCornerShape(T.ICON_CARD_RADIUS.dp)),
        )
        Box(
            Modifier
                .figma(T.IconBadge)
                .background(if (colors.isDark) T.IconBadgeDark else T.IconBadgeLight, CircleShape),
        )
    }
}

// ---------------------------------------------------------------- Motion Fullscreen

/**
 * 폰 잠금 화면 06:59 → 1.2초 뒤 07:00 → 0.35초 뒤 알람 화면이 0.15초 만에 올라와요(Ease out) →
 * 0.25초 뒤 폰이 좌우로 9번(0.04초씩) 떨고, 0.45초 쉬고 다시 9번 떨어요 → 1.2초 뒤 0.4초 동안 잠금 화면으로 겹쳐 바뀌어요.
 */
@Composable
private fun MotionFullscreen(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val clockNew = remember { Animatable(0f) }
    val alarmRise = remember { Animatable(0f) }
    val alarmAlpha = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            clockNew.snapTo(1f)
            alarmRise.snapTo(1f)
            alarmAlpha.snapTo(1f)
            shake.snapTo(0f)
            return@LaunchedEffect
        }
        suspend fun shakeNine() {
            repeat(T.PHONE_SHAKE_COUNT) { index ->
                if (index > 0) delay(T.FULL_SHAKE_GAP)
                val target = when {
                    index == T.PHONE_SHAKE_COUNT - 1 -> 0f
                    index % 2 == 0 -> -T.PHONE_SHAKE_PX
                    else -> T.PHONE_SHAKE_PX
                }
                shake.animateTo(target, tween(T.FULL_SHAKE_STEP, easing = LinearEasing))
            }
        }
        while (true) {
            clockNew.snapTo(0f)
            alarmRise.snapTo(0f)
            alarmAlpha.snapTo(0f)
            delay(T.FULL_LOCK_HOLD)
            clockNew.snapTo(1f)
            delay(T.FULL_ALARM_WAIT)
            coroutineScope {
                launch { alarmRise.animateTo(1f, tween(T.FULL_ALARM_RISE, easing = T.EaseOut)) }
                launch { alarmAlpha.animateTo(1f, tween(T.FULL_ALARM_RISE, easing = T.EaseOut)) }
            }
            delay(T.FULL_SHAKE_WAIT)
            shakeNine()
            delay(T.FULL_SHAKE_PAUSE)
            shakeNine()
            delay(T.FULL_END_HOLD)
            // 겹쳐 바뀌기(Dissolve): 알람 화면 밑의 시계를 06:59로 돌려 두고 알람 화면만 흐려지게 하면 두 장면이 겹쳐 보여요.
            clockNew.snapTo(0f)
            alarmAlpha.animateTo(0f, tween(T.FULL_DISSOLVE, easing = T.EaseInOut))
        }
    }
    Box(modifier) {
        Box(Modifier.figma(T.Phone).graphicsLayer { translationX = shake.value.dp.toPx() }) {
            PhoneBody(clockNew = clockNew.value, alarmRise = alarmRise.value, alarmAlpha = alarmAlpha.value)
        }
    }
}

@Composable
private fun PhoneBody(clockNew: Float, alarmRise: Float, alarmAlpha: Float) {
    val colors = MorningTheme.colors
    val buttonShape = RoundedCornerShape(T.PHONE_BUTTON_RADIUS.dp)
    listOf(T.PhonePower, T.PhoneVolDown, T.PhoneVolUp).forEach { rect ->
        Box(Modifier.figma(rect).background(T.PhoneButtonColor, buttonShape))
    }
    Box(
        Modifier
            .figma(FigmaRect(0f, 0f, T.Phone.w, T.Phone.h))
            .background(colors.borderDefault, RoundedCornerShape(T.PHONE_RADIUS.dp)),
    )
    Box(Modifier.figma(T.PhoneBezel).background(T.PhoneBezelColor, RoundedCornerShape(T.PHONE_BEZEL_RADIUS.dp)))
    Box(
        Modifier
            .figma(T.PhoneScreen)
            .clip(RoundedCornerShape(T.PHONE_SCREEN_RADIUS.dp))
            .background(T.PhoneScreenColor),
    ) {
        Box(Modifier.figma(T.PhoneLock)) {
            Icon(
                imageVector = Lucide.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(T.PhoneLock.w.dp),
            )
        }
        Box(Modifier.graphicsLayer { alpha = 1f - clockNew }) {
            FigmaText(
                stringResource(R.string.onboarding_motion_clock_before),
                T.PhoneClock,
                T.PHONE_CLOCK_SIZE,
                FontWeight.Bold,
                Color.White,
            )
        }
        Box(Modifier.graphicsLayer { alpha = clockNew }) {
            FigmaText(
                stringResource(R.string.onboarding_motion_clock_after),
                T.PhoneClock,
                T.PHONE_CLOCK_SIZE,
                FontWeight.Bold,
                Color.White,
            )
        }
        if (alarmAlpha > 0f) {
            val y = lerp(T.ALARM_FROM_Y, 0f, alarmRise)
            AlarmScreen(
                Modifier
                    .figma(FigmaRect(0f, y, T.PhoneScreen.w, T.PhoneScreen.h))
                    .graphicsLayer { alpha = alarmAlpha },
            )
        }
    }
    Box(Modifier.figma(T.PhoneIsland).background(T.PhoneIslandColor, CircleShape))
}

/** 폰 속 알람 화면. 피그마 지금 값: 빨간 그라데이션, 날짜, 7:00, 파란 동그란 '미션 시작'. */
@Composable
private fun AlarmScreen(modifier: Modifier) {
    val colors = MorningTheme.colors
    Box(modifier.background(Brush.verticalGradient(listOf(T.AlarmTop, T.AlarmBottom)))) {
        FigmaText(
            stringResource(R.string.onboarding_motion_date),
            T.AlarmDate,
            T.ALARM_DATE_SIZE,
            FontWeight.Medium,
            colors.textOnSkySoft,
        )
        FigmaText(
            stringResource(R.string.onboarding_motion_time),
            T.AlarmTime,
            T.ALARM_TIME_SIZE,
            FontWeight.Bold,
            colors.textOnSky,
        )
        // 11-20 사용자 지시: 온보딩 목업의 '미션 시작'은 파란 바탕(primary/default)에 흰 글자(text/on-primary).
        // 실제 울림 화면 D는 그대로예요.
        Box(Modifier.figma(T.AlarmButton).background(colors.primaryDefault, CircleShape)) {
            FigmaText(
                stringResource(R.string.onboarding_motion_mission_start),
                FigmaRect(0f, 0f, T.AlarmButton.w, T.AlarmButton.h),
                T.ALARM_BUTTON_TEXT_SIZE,
                FontWeight.Bold,
                colors.primaryOn,
            )
        }
    }
}

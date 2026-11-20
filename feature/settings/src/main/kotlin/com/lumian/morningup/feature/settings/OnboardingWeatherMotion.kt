package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.lumian.morningup.feature.settings.OnboardingMotionTokens as T

/** 시험에서 O3 낮 그림과 밤 그림을 가리는 이름. */
internal const val WEATHER_DAY_TAG = "onboarding-weather-day"
internal const val WEATHER_NIGHT_TAG = "onboarding-weather-night"

/**
 * O3 날씨 그림(11-20 사용자 지시). 카드 없이 화면 배경 위에 그려요. 테마는 앱과 같이 시각을 따라요(`rememberNightByClock`).
 * 라이트는 구름과 그 뒤의 해, 다크는 별과 노란 초승달이에요(색만 바꾼 같은 그림이 아니에요).
 */
@Composable
internal fun MotionWeather(modifier: Modifier = Modifier) {
    if (MorningTheme.colors.isDark) {
        MotionWeatherNight(modifier.testTag(WEATHER_NIGHT_TAG))
    } else {
        MotionWeatherDay(modifier.testTag(WEATHER_DAY_TAG))
    }
}

// ---------------------------------------------------------------- Motion Weather Cloud (라이트)

/**
 * 0.2초 뒤 구름이 16 아래에서 GENTLE 0.6초로 가운데에 떠오르고, 0.1초 뒤 해와 빛이 구름 뒤에서 40 올라오며 나타나요(EASE_OUT 0.8초).
 * 그다음 구름만 위아래 6을 EASE_IN_AND_OUT 1.8초씩 오가요.
 */
@Composable
private fun MotionWeatherDay(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val cloudY = remember { Animatable(T.CLOUD_ENTER_Y) }
    val cloudShown = remember { Animatable(0f) }
    val sun = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            cloudY.snapTo(T.CLOUD_Y)
            cloudShown.snapTo(1f)
            sun.snapTo(1f)
            return@LaunchedEffect
        }
        cloudY.snapTo(T.CLOUD_ENTER_Y)
        cloudShown.snapTo(0f)
        sun.snapTo(0f)
        delay(T.CLOUD_ENTER_WAIT)
        coroutineScope {
            launch { cloudY.animateTo(T.CLOUD_Y, weatherEnter()) }
            launch { cloudShown.animateTo(1f, weatherEnter()) }
            delay(T.CLOUD_ENTER)
        }
        delay(T.SUN_WAIT)
        sun.animateTo(1f, tween(T.SUN_ENTER, easing = T.EaseOut))
        while (true) {
            delay(T.FLOAT_GAP)
            cloudY.animateTo(T.CLOUD_UP_Y, tween(T.FLOAT_STEP, easing = T.EaseInOut))
            delay(T.FLOAT_GAP)
            cloudY.animateTo(T.CLOUD_Y, tween(T.FLOAT_STEP, easing = T.EaseInOut))
        }
    }
    val colors = MorningTheme.colors
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val shown = sun.value.coerceIn(0f, 1f)
            if (shown <= 0f) return@Canvas
            val center = Offset(T.SUN_CENTER_X.dp.toPx(), (T.SUN_CENTER_Y + (1f - sun.value) * T.SUN_RISE).dp.toPx())
            softGlow(colors.celestialSunHalo, center, T.SUN_HALO_SIZE / 2, T.SUN_HALO_BLUR, T.SUN_HALO_ALPHA * shown)
            softGlow(
                colors.celestialSunInnerGlow,
                center,
                T.SUN_INNER_SIZE / 2,
                T.SUN_INNER_BLUR,
                T.SUN_INNER_ALPHA * shown,
            )
            val r = (T.SUN_SIZE / 2).dp.toPx()
            drawCircle(
                brush = diagonal(colors.celestialSunCore1, colors.celestialSunCore2, center - Offset(r, r), r * 2),
                radius = r,
                center = center,
                alpha = shown,
            )
        }
        Canvas(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = cloudShown.value.coerceIn(0f, 1f) },
        ) {
            val top = cloudY.value
            val base = T.CloudBase
            drawRoundRect(
                color = colors.weatherParticle,
                topLeft = Offset(base.x.dp.toPx(), (top + base.y).dp.toPx()),
                size = Size(base.w.dp.toPx(), base.h.dp.toPx()),
                cornerRadius = CornerRadius(T.CLOUD_BASE_RADIUS.dp.toPx()),
            )
            listOf(T.CloudPuffSmall, T.CloudPuffBig).forEach { puff ->
                drawCircle(
                    color = colors.weatherParticle,
                    radius = (puff.w / 2).dp.toPx(),
                    center = Offset((puff.x + puff.w / 2).dp.toPx(), (top + puff.y + puff.h / 2).dp.toPx()),
                )
            }
        }
    }
}

/**
 * 피그마 레이어 흐림(blur)을 둥근 그라데이션으로 흉내 내요(옛 안드로이드는 흐림 효과가 없어요).
 * 원 반지름 [radius]에서 흐림 [blur]만큼 안팎으로 번져요.
 */
private fun DrawScope.softGlow(color: Color, center: Offset, radius: Float, blur: Float, alpha: Float) {
    val outer = (radius + blur).dp.toPx()
    val solid = ((radius - blur) / (radius + blur)).coerceAtLeast(0f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            solid to color.copy(alpha = alpha),
            1f to color.copy(alpha = 0f),
            center = center,
            radius = outer,
        ),
        radius = outer,
        center = center,
    )
}

/** 피그마 선형 그라데이션(왼쪽 위 → 오른쪽 아래). [topLeft]에서 [size] 크기 칸 기준이에요. */
private fun diagonal(from: Color, to: Color, topLeft: Offset, size: Float): Brush = Brush.linearGradient(
    colors = listOf(from, to),
    start = topLeft + Offset(size * T.GRADIENT_FROM, size * T.GRADIENT_FROM),
    end = topLeft + Offset(size * T.GRADIENT_TO, size * T.GRADIENT_TO),
)

// ---------------------------------------------------------------- Motion Weather Night (다크)

/**
 * 0.2초 뒤 별 1, 3, 5(GENTLE 0.5초), 0.05초 뒤 별 2, 4, 6(GENTLE 0.5초), 0.1초 뒤 노란 초승달이 30 아래에서 떠올라요(EASE_OUT 0.8초).
 * 그다음 달은 가운데를 축으로 흔들흔들(±8도), 별은 반짝반짝, EASE_IN_AND_OUT 1.8초씩 오가요. 달 뒤 빛은 없어요.
 */
@Composable
private fun MotionWeatherNight(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val oddStars = remember { Animatable(0f) }
    val evenStars = remember { Animatable(0f) }
    val moon = remember { Animatable(0f) }
    // 0 = 피그마 Down(달 시계 방향 8도, 별 Down 진하기), 1 = Up(반시계 방향 8도, 별 Up 진하기).
    val swing = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            oddStars.snapTo(1f)
            evenStars.snapTo(1f)
            moon.snapTo(1f)
            swing.snapTo(0f)
            return@LaunchedEffect
        }
        listOf(oddStars, evenStars, moon, swing).forEach { it.snapTo(0f) }
        delay(T.STAR_ENTER_WAIT)
        coroutineScope {
            launch { oddStars.animateTo(1f, weatherEnter()) }
            delay(T.STAR_ENTER)
        }
        delay(T.STAR_GAP)
        coroutineScope {
            launch { evenStars.animateTo(1f, weatherEnter()) }
            delay(T.STAR_ENTER)
        }
        delay(T.MOON_WAIT)
        moon.animateTo(1f, tween(T.MOON_ENTER, easing = T.EaseOut))
        while (true) {
            delay(T.FLOAT_GAP)
            swing.animateTo(1f, tween(T.FLOAT_STEP, easing = T.EaseInOut))
            delay(T.FLOAT_GAP)
            swing.animateTo(0f, tween(T.FLOAT_STEP, easing = T.EaseInOut))
        }
    }
    val colors = MorningTheme.colors
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            T.Stars.forEachIndexed { index, rect ->
                // 별 1, 3, 5(0, 2, 4번)가 먼저, 2, 4, 6이 나중에 나와요.
                val enter = (if (index % 2 == 0) oddStars.value else evenStars.value).coerceIn(0f, 1f)
                val alpha = lerp(T.StarAlphaDown[index], T.StarAlphaUp[index], swing.value) * enter
                if (alpha <= 0f) return@forEachIndexed
                drawPath(
                    path = starPath(rect),
                    color = if (index % 2 == 0) colors.skyStar else colors.skyStarBlue,
                    alpha = alpha,
                )
            }
        }
        val shown = moon.value.coerceIn(0f, 1f)
        if (shown > 0f) {
            Canvas(
                Modifier
                    .figma(
                        FigmaRect(
                            T.MOON_CENTER_X - T.MOON_SIZE / 2,
                            T.MOON_CENTER_Y - T.MOON_SIZE / 2 + (1f - moon.value) * T.MOON_RISE,
                            T.MOON_SIZE,
                            T.MOON_SIZE,
                        ),
                    )
                    .graphicsLayer {
                        alpha = shown
                        // 피그마 -8도(시계 방향)가 Compose +8도예요.
                        rotationZ = lerp(T.MOON_SWING, -T.MOON_SWING, swing.value)
                    },
            ) {
                val full = Path().apply { addOval(Rect(Offset.Zero, size)) }
                val cut = Path().apply {
                    addOval(
                        Rect(
                            Offset(T.MOON_CUT_X.dp.toPx(), T.MOON_CUT_Y.dp.toPx()),
                            Size(T.MOON_CUT_SIZE.dp.toPx(), T.MOON_CUT_SIZE.dp.toPx()),
                        ),
                    )
                }
                val crescent = Path().apply { op(full, cut, PathOperation.Difference) }
                drawPath(crescent, diagonal(colors.skyNoonGlow, colors.celestialSunCore1, Offset.Zero, size.width))
            }
        }
    }
}

/** 4각 별(안쪽 반지름 38%). */
private fun DrawScope.starPath(rect: FigmaRect): Path {
    val cx = (rect.x + rect.w / 2).dp.toPx()
    val cy = (rect.y + rect.h / 2).dp.toPx()
    val outer = (rect.w / 2).dp.toPx()
    val inner = outer * T.STAR_INNER
    val corners = T.STAR_POINTS * 2
    return Path().apply {
        for (i in 0 until corners) {
            val r = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + PI * i / T.STAR_POINTS
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** O3 등장용 GENTLE. 피그마보다 두 배 빨라요(11-20 사용자 요청). */
private fun weatherEnter(): AnimationSpec<Float> =
    spring(dampingRatio = T.GENTLE_DAMPING_RATIO, stiffness = T.WEATHER_ENTER_STIFFNESS)

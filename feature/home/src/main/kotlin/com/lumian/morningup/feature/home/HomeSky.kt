package com.lumian.morningup.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.theme.MorningColors
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import java.time.LocalTime
import kotlin.random.Random

/** H0 하늘 장면. 해와 달은 시각을 따라 바뀌어요(날씨는 9단계). */
enum class SkyScene { Dawn, Noon, Dusk, Night }

/**
 * 시각으로 고르는 장면. 다크 모드는 늘 밤이에요(밝은 하늘 위 흰 글자는 읽기 어려워서요).
 * 라이트 모드의 밤 시간은 '새벽' 하늘에 달만 띄워요(아침 컨셉 하나, 글자 대비 유지).
 */
fun skySceneAt(time: LocalTime, dark: Boolean): SkyScene = when {
    dark -> SkyScene.Night
    time.hour in NOON_START until DUSK_START -> SkyScene.Noon
    time.hour in DUSK_START until NIGHT_START -> SkyScene.Dusk
    else -> SkyScene.Dawn
}

/** 해 대신 달을 띄우는 시각(밤 7시부터 새벽 5시 전까지). */
fun showsMoon(time: LocalTime, dark: Boolean): Boolean =
    dark || time.hour >= NIGHT_START || time.hour < DAWN_START

private const val DAWN_START = 5
private const val NOON_START = 10
private const val DUSK_START = 16
private const val NIGHT_START = 19

private val SunCore = 140.dp
private val SunInner = 184.dp
private val SunHalo = 240.dp
private val MoonCore = 130.dp
private val MoonHalo = 200.dp

/** 해, 달 가운데의 세로 위치(본체 아래 30%만 화면 안). */
private val BodyCenterY = (-28).dp
private val RiseFrom = 220.dp
private val FloatRange = 6.dp
private const val RISE_MILLIS = 1_000
private const val FLOAT_MILLIS = 3_500
private const val TWINKLE_MILLIS = 2_400
private const val SKY_SOLID_STOP = 0.55f
private const val SKY_FADE_STOP = 0.9f
private const val SUN_HALO_ALPHA = 0.55f
private const val SUN_INNER_ALPHA = 0.6f
private const val MOON_HALO_ALPHA = 0.3f
private const val MARE_ALPHA = 0.45f
private const val CRATER_ALPHA = 0.5f
private const val STAR_COUNT = 34
private const val STAR_SEED = 7
private const val STAR_BLUE_SHARE = 0.2f
private const val STAR_DIM = 0.3f
private const val STAR_MIN_ALPHA = 0.35f
private const val STAR_AREA = 0.6f
private const val BODY_X = 0.5f

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val blue: Boolean,
    val twinkles: Boolean,
)

/**
 * 홈 위쪽 하늘(Home Sky 360x420). 아래 55%부터 90%까지 바탕색으로 서서히 녹아요.
 * 처음 보일 때 해나 달이 아래에서 1초 동안 올라오고, 그 뒤 위아래 6dp로 둥실거려요. 밤에는 별 절반이 반짝여요.
 * '애니메이션 줄이기'가 켜져 있으면 제자리에 멈춘 장면이에요.
 */
@Composable
fun HomeSky(time: LocalTime, height: Dp, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val reduceMotion = MorningTheme.reduceMotion
    val dark = colors.isDark
    val scene = skySceneAt(time, dark)
    val moon = showsMoon(time, dark)
    val rise = remember { Animatable(if (reduceMotion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) rise.animateTo(0f, tween(RISE_MILLIS, easing = LinearOutSlowInEasing))
    }
    val float = floatState(reduceMotion)
    val twinkle = twinkleState(reduceMotion || scene != SkyScene.Night)
    val stars = remember { stars() }
    val (top, bottom) = sceneColors(scene, colors)
    val base = colors.backgroundBase
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawRect(
            Brush.verticalGradient(
                0f to top,
                SKY_SOLID_STOP to bottom,
                SKY_FADE_STOP to base,
                1f to base,
            ),
        )
        if (scene == SkyScene.Night) drawStars(stars, twinkle.value, colors)
        val center = Offset(
            size.width * BODY_X,
            BodyCenterY.toPx() + rise.value * RiseFrom.toPx() + float.value * FloatRange.toPx(),
        )
        if (moon) drawMoon(center, colors) else drawSun(center, colors)
    }
}

@Composable
private fun floatState(still: Boolean): State<Float> {
    if (still) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(FLOAT_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
}

@Composable
private fun twinkleState(still: Boolean): State<Float> {
    if (still) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 1f,
        targetValue = STAR_DIM,
        animationSpec = infiniteRepeatable(tween(TWINKLE_MILLIS), RepeatMode.Reverse),
        label = "twinkleAlpha",
    )
}

private fun sceneColors(scene: SkyScene, c: MorningColors): Pair<Color, Color> = when (scene) {
    SkyScene.Dawn -> c.sky2 to c.sky4
    SkyScene.Noon -> c.skyNoonTop to c.skyNoonBottom
    SkyScene.Dusk -> c.skyDuskTop to c.skyDuskBottom
    SkyScene.Night -> c.skyNightTop to c.skyNightBottom
}

private fun stars(): List<Star> {
    val random = Random(STAR_SEED)
    return List(STAR_COUNT) { i ->
        Star(
            x = random.nextFloat(),
            y = random.nextFloat() * STAR_AREA,
            radius = 0.5f + random.nextFloat() * 1.5f,
            alpha = STAR_MIN_ALPHA + random.nextFloat() * (1f - STAR_MIN_ALPHA),
            blue = random.nextFloat() < STAR_BLUE_SHARE,
            twinkles = i % 2 == 0,
        )
    }
}

private fun DrawScope.drawStars(stars: List<Star>, twinkle: Float, c: MorningColors) {
    stars.forEach { s ->
        drawCircle(
            color = if (s.blue) c.skyStarBlue else c.skyStar,
            radius = s.radius.dp.toPx(),
            center = Offset(s.x * size.width, s.y * size.height),
            alpha = s.alpha * if (s.twinkles) twinkle else 1f,
        )
    }
}

private fun DrawScope.drawSun(center: Offset, c: MorningColors) {
    drawCircle(
        brush = Brush.radialGradient(listOf(c.celestialSunHalo, Color.Transparent), center, SunHalo.toPx() / 2),
        radius = SunHalo.toPx() / 2,
        center = center,
        alpha = SUN_HALO_ALPHA,
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(c.celestialSunInnerGlow, Color.Transparent), center, SunInner.toPx() / 2),
        radius = SunInner.toPx() / 2,
        center = center,
        alpha = SUN_INNER_ALPHA,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(c.celestialSunCore1, c.celestialSunCore2, c.celestialSunCore3),
            center,
            SunCore.toPx() / 2,
        ),
        radius = SunCore.toPx() / 2,
        center = center,
    )
}

/** 달 바다 무늬와 크레이터 자리(본체 반지름에 대한 비율: x, y, 크기). */
private val Mares = listOf(
    Triple(-0.3f, 0.15f, 0.22f),
    Triple(0.2f, 0.35f, 0.18f),
    Triple(0.05f, 0.6f, 0.14f),
    Triple(-0.45f, 0.5f, 0.1f),
)
private val Craters = listOf(
    Triple(0.45f, 0.1f, 0.06f),
    Triple(-0.1f, 0.4f, 0.05f),
    Triple(0.35f, 0.62f, 0.07f),
    Triple(-0.55f, 0.2f, 0.04f),
    Triple(0.6f, 0.4f, 0.04f),
)

private fun DrawScope.drawMoon(center: Offset, c: MorningColors) {
    val r = MoonCore.toPx() / 2
    drawCircle(
        brush = Brush.radialGradient(listOf(c.celestialMoonHalo, Color.Transparent), center, MoonHalo.toPx() / 2),
        radius = MoonHalo.toPx() / 2,
        center = center,
        alpha = MOON_HALO_ALPHA,
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(c.celestialMoon1, c.celestialMoon2, c.celestialMoon3), center, r),
        radius = r,
        center = center,
    )
    Mares.forEach { (x, y, s) ->
        drawCircle(c.celestialMoonMare, s * r, Offset(center.x + x * r, center.y + y * r), alpha = MARE_ALPHA)
    }
    Craters.forEach { (x, y, s) ->
        drawCircle(c.celestialMoonCrater, s * r, Offset(center.x + x * r, center.y + y * r), alpha = CRATER_ALPHA)
    }
}

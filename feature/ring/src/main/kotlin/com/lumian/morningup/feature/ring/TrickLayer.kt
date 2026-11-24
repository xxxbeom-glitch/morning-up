package com.lumian.morningup.feature.ring

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickLevel
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 버튼 크기 무작위 범위(11-24 사용자 추가): 손톱만 한 40dp부터 지금 크기 112dp까지. */
private val TrickMinSize = 40.dp

/** 보이는 버튼이 작아도 누르는 영역은 이 크기 이상이에요. */
private val TrickTouchMin = 48.dp

/** 날짜와 시각 글자 둘레로 비워 두는 폭. */
private val TrickAvoidPad = 16.dp

/** 세게에서 버튼끼리 띄우는 폭. */
private val TrickGap = 8.dp

/** 옮기기 전 제자리 흔들림 폭. */
private val ShakeAmplitude = 4.dp

/** 약하게: 이 간격마다 옮겨요(흔들림과 옮기기 포함). */
private const val MOVE_EVERY_MILLIS = 2_000

/** 옮겨 가는 시간, 그 전 흔들림 시간. */
private const val MOVE_MILLIS = 300
private const val SHAKE_MILLIS = 300

/** 보통, 세게: 보였다가(1.5초) 숨는(0.8초) 시간. */
private const val SEEN_MILLIS = 1_500
private const val HIDDEN_MILLIS = 800

/** 사라질 때 흐려지는 시간. 이 동안 누른 것은 인정해요(사라지기 직전 0.15초까지, 2-2). 속도와 상관없이 같아요. */
private const val FADE_MILLIS = 150

/** 20초가 지나 원래 자리로 돌아가는 시간. */
private const val RETURN_MILLIS = 300

/** '이건 가짜예요'를 보여 주는 시간. */
private const val FAKE_NOTICE_MILLIS = 1_000L

/** 세게의 가짜 버튼 수. */
private const val FAKE_COUNT = 2

private const val PLACE_TRIES = 60
private const val SHAKE_STEPS = 4
private const val VISIBLE_ALPHA = 0.01f
private const val MILLIS_PER_SECOND = 1_000L

/** 버튼 속이기 한 번 동안 일어난 일. 울림 화면은 기록을 남기고, 미리 해보기는 아무것도 하지 않아요. */
interface TrickListener {
    /** 화면을 처음 만졌어요. 여기서부터 20초를 세요. */
    fun onStart() {}

    /** 가짜 버튼을 [count]번째 눌렀어요. */
    fun onFakeTap(count: Int) {}

    /** 진짜 버튼을 눌렀어요. [expired]면 20초가 다 지나 멈춘 뒤 누른 거예요. */
    fun onEnd(seconds: Int, fakes: Int, expired: Boolean) {}

    companion object {
        val None = object : TrickListener {}
    }
}

/** 버튼 하나가 보이는 자리(가운데), 크기(px). */
data class TrickSpot(val center: Offset, val size: Float)

/**
 * 무작위 자리 고르기(11-24 사용자 추가: 화면 어디든, 크기 40~112dp). 모든 값은 움직이는 영역 안의 px예요.
 * 누르는 영역([touchMin] 이상)이 영역 밖으로 나가지 않고, [avoid](날짜와 시각 글자 둘레)와 겹치지 않고,
 * 버튼끼리 [gap] 이상 떨어지게 골라요. 끝까지 못 찾으면 겹침만 허용해요.
 */
object TrickPlacer {
    @Suppress("LongParameterList")
    fun place(
        count: Int,
        area: Size,
        avoid: Rect?,
        minSize: Float,
        maxSize: Float,
        touchMin: Float,
        gap: Float,
        random: Random,
    ): List<TrickSpot> {
        val spots = mutableListOf<TrickSpot>()
        repeat(count) {
            var pick: TrickSpot? = null
            for (attempt in 0 until PLACE_TRIES) {
                val candidate = randomSpot(area, minSize, maxSize, touchMin, random)
                val touch = max(candidate.size, touchMin) / 2
                val clearOfText = avoid == null || !circleHitsRect(candidate.center, touch, avoid)
                val clearOfOthers = spots.all { other ->
                    val needed = touch + max(other.size, touchMin) / 2 + gap
                    distance(candidate.center, other.center) >= needed
                }
                if (clearOfText && clearOfOthers) {
                    pick = candidate
                    break
                }
                if (attempt == PLACE_TRIES - 1 && clearOfText) pick = candidate
            }
            spots += pick ?: randomSpot(area, minSize, maxSize, touchMin, random)
        }
        return spots
    }

    private fun randomSpot(area: Size, minSize: Float, maxSize: Float, touchMin: Float, random: Random): TrickSpot {
        val size = minSize + random.nextFloat() * (maxSize - minSize)
        val half = max(size, touchMin) / 2
        val x = between(half, area.width - half, random)
        val y = between(half, area.height - half, random)
        return TrickSpot(Offset(x, y), size)
    }

    private fun between(from: Float, until: Float, random: Random): Float =
        if (until <= from) (from + until) / 2 else from + random.nextFloat() * (until - from)

    private fun distance(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

    /** 원(가운데 [c], 반지름 [r])이 [rect]에 닿는지. */
    fun circleHitsRect(c: Offset, r: Float, rect: Rect): Boolean {
        val nearestX = c.x.coerceIn(rect.left, rect.right)
        val nearestY = c.y.coerceIn(rect.top, rect.bottom)
        return distance(c, Offset(nearestX, nearestY)) < r
    }
}

/** 움직이는 버튼 하나. 가운데 자리, 보이는 크기(px), 투명도, 흔들림(px)을 따로 움직여요. */
internal class TrickButton {
    val center = Animatable(Offset.Zero, Offset.VectorConverter)
    val size = Animatable(0f)
    val alpha = Animatable(1f)
    val shake = Animatable(0f)
}

/**
 * 울림 화면이 한 번 뜰 동안의 버튼 속이기(결정 146 2절). 화면이 새로 뜨면 새로 만들어 처음부터 다시 걸려요.
 * - [firstTouchAt]: 화면을 처음 만진 때. 여기서부터 [LabSettings.TRICK_SECONDS]초가 지나면 [expired].
 * - [done]: 원래 자리로 돌아가 멈췄어요. 그 뒤는 원래 버튼이 그대로 있어요.
 */
@Stable
class TrickRun(val trick: ButtonTrick, internal val random: Random = Random.Default) {
    internal var firstTouchAt by mutableStateOf<Long?>(null)
    internal var expired by mutableStateOf(false)
    var done by mutableStateOf(false)
        internal set
    internal var fakes by mutableIntStateOf(0)
    internal var realIndex by mutableIntStateOf(0)
    internal var cycle by mutableIntStateOf(0)
    internal var noticeTick by mutableIntStateOf(0)
    var showFakeNotice by mutableStateOf(false)
        internal set
    private var ended = false
    internal var placed = false

    /** 움직이는 영역, 날짜와 시각 묶음, 원래 버튼 자리(모두 화면 기준 px). */
    internal var area by mutableStateOf<Rect?>(null)
    var avoid by mutableStateOf<Rect?>(null)
    var home by mutableStateOf<Rect?>(null)

    internal val buttons = List(if (trick.level == TrickLevel.Strong) 1 + FAKE_COUNT else 1) { TrickButton() }

    internal fun touched(listener: TrickListener) {
        if (firstTouchAt != null) return
        firstTouchAt = SystemClock.elapsedRealtime()
        listener.onStart()
    }

    /** 진짜 버튼을 눌렀어요. 한 번만 알려요. 20초가 지나 원래 버튼을 누른 경우도 여기로 와요. */
    fun finish(listener: TrickListener) {
        if (ended) return
        ended = true
        val seconds = firstTouchAt?.let { ((SystemClock.elapsedRealtime() - it) / MILLIS_PER_SECOND).toInt() } ?: 0
        listener.onEnd(seconds, fakes, expired)
    }
}

/**
 * 버튼 속이기 층. 울림 화면 위에 꽉 차게 덮고 버튼을 그려요. 원래 버튼 자리는 비워 두고([TrickRun.home]만 재요),
 * 20초가 지나면 진짜 버튼이 그 자리로 0.3초에 걸쳐 돌아가 [TrickRun.done]이 돼요.
 * - 약하게: 2초마다 0.3초 흔들려 예고하고 0.3초에 걸쳐 새 자리와 크기로 옮겨요.
 * - 보통: 옮기기와 숨바꼭질(1.5초 보였다가 0.8초 숨고 다른 자리에 나타남)을 번갈아 해요.
 * - 세게: 보통 + 똑같은 가짜 버튼 2개. 숨었다 나타날 때마다 진짜가 바뀌어요.
 * - 가짜를 누르면 짧은 진동(햅틱, 울림 진동을 끊지 않아요)과 '이건 가짜예요' 1초, 그리고 자리를 다시 섞어요.
 * - 누르는 순간(손가락이 닿을 때) 인정해요. 사라지는 중(0.15초)에 닿은 것도 인정해요.
 */
@Composable
fun TrickLayer(run: TrickRun, label: String, listener: TrickListener, onReal: (center: Offset) -> Unit) {
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentListener by rememberUpdatedState(listener)
    val currentOnReal by rememberUpdatedState(onReal)
    val minSize = with(density) { TrickMinSize.toPx() }
    val maxSize = with(density) { MorningDimens.sizeButtonCircle.toPx() }
    val touchMin = with(density) { TrickTouchMin.toPx() }
    val gap = with(density) { TrickGap.toPx() }
    val shakePx = with(density) { ShakeAmplitude.toPx() }
    val avoidPad = with(density) { TrickAvoidPad.toPx() }

    val onTap: (Int) -> Unit = { index ->
        val button = run.buttons[index]
        val area = run.area
        if (button.alpha.value > VISIBLE_ALPHA && area != null) {
            if (index == run.realIndex) {
                view.perform(MorningHaptic.Confirm)
                run.finish(currentListener)
                currentOnReal(button.center.value + area.topLeft)
            } else {
                run.fakes += 1
                currentListener.onFakeTap(run.fakes)
                view.perform(MorningHaptic.Reject)
                val tick = ++run.noticeTick
                run.showFakeNotice = true
                scope.launch {
                    delay(FAKE_NOTICE_MILLIS)
                    if (run.noticeTick == tick) run.showFakeNotice = false
                }
                if (!run.expired) run.cycle += 1
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(run) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    run.touched(currentListener)
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(MorningDimens.space16)
                .onGloballyPositioned { run.area = it.boundsInRoot() },
        ) {
            run.buttons.forEachIndexed { index, button ->
                TrickFace(button, label, touchMin, maxSize) { onTap(index) }
            }
        }
    }

    // 처음 만진 때부터 20초.
    LaunchedEffect(run, run.firstTouchAt) {
        val start = run.firstTouchAt ?: return@LaunchedEffect
        val left = LabSettings.TRICK_SECONDS * MILLIS_PER_SECOND - (SystemClock.elapsedRealtime() - start)
        delay(left.coerceAtLeast(0))
        run.expired = true
    }

    val area = run.area
    LaunchedEffect(run, area?.size, run.cycle, run.expired) {
        if (area == null) return@LaunchedEffect
        val avoid = run.avoid?.translate(-area.topLeft)?.inflate(avoidPad)
        val places = { TrickPlacer.place(run.buttons.size, area.size, avoid, minSize, maxSize, touchMin, gap, run.random) }
        val factor = run.trick.speed.factor
        if (run.expired) {
            val home = run.home?.center?.minus(area.topLeft)
            run.returnHome(home, maxSize)
            run.done = true
            return@LaunchedEffect
        }
        if (!run.placed) {
            run.snapTo(places())
            run.placed = true
        } else {
            // 가짜를 눌러 다시 섞어요.
            run.moveTo(places(), (MOVE_MILLIS * factor).roundToInt())
        }
        var hideNext = false
        while (isActive) {
            if (run.trick.level != TrickLevel.Weak && hideNext) {
                delay((SEEN_MILLIS * factor).toLong())
                run.fade(0f)
                delay(((HIDDEN_MILLIS * factor) - 2 * FADE_MILLIS).toLong().coerceAtLeast(0))
                run.snapTo(places())
                if (run.buttons.size > 1) run.realIndex = run.random.nextInt(run.buttons.size)
                run.fade(1f)
            } else {
                val shake = (SHAKE_MILLIS * factor).roundToInt()
                val move = (MOVE_MILLIS * factor).roundToInt()
                delay(((MOVE_EVERY_MILLIS * factor) - shake - move).toLong().coerceAtLeast(0))
                run.shake(shakePx, shake)
                run.moveTo(places(), move)
            }
            hideNext = !hideNext
        }
    }
}

private suspend fun TrickRun.snapTo(spots: List<TrickSpot>) = coroutineScope {
    buttons.zip(spots).forEach { (button, spot) ->
        launch { button.center.snapTo(spot.center) }
        launch { button.size.snapTo(spot.size) }
    }
}

private suspend fun TrickRun.moveTo(spots: List<TrickSpot>, millis: Int) = coroutineScope {
    buttons.zip(spots).forEach { (button, spot) ->
        launch { button.center.animateTo(spot.center, tween(millis)) }
        launch { button.size.animateTo(spot.size, tween(millis)) }
        launch { button.alpha.animateTo(1f, tween(FADE_MILLIS)) }
    }
}

private suspend fun TrickRun.fade(to: Float) = coroutineScope {
    buttons.forEach { button -> launch { button.alpha.animateTo(to, tween(FADE_MILLIS, easing = LinearEasing)) } }
}

private suspend fun TrickRun.shake(amplitude: Float, millis: Int) = coroutineScope {
    val step = millis / SHAKE_STEPS
    buttons.forEach { button ->
        launch {
            button.shake.animateTo(
                0f,
                keyframes {
                    durationMillis = millis
                    amplitude at step
                    -amplitude at step * 2
                    amplitude at step * 3
                },
            )
        }
    }
}

/** 20초가 지나면 진짜 버튼은 원래 자리와 크기로 돌아가고, 가짜는 사라져요. */
private suspend fun TrickRun.returnHome(home: Offset?, fullSize: Float) = coroutineScope {
    buttons.forEachIndexed { index, button ->
        launch { button.shake.snapTo(0f) }
        if (index == realIndex) {
            if (home != null) launch { button.center.animateTo(home, tween(RETURN_MILLIS)) }
            launch { button.size.animateTo(fullSize, tween(RETURN_MILLIS)) }
            launch { button.alpha.animateTo(1f, tween(RETURN_MILLIS)) }
        } else {
            launch { button.alpha.animateTo(0f, tween(RETURN_MILLIS)) }
        }
    }
}

/**
 * 버튼 하나. 피그마 Button Circle(112)을 비율 그대로 줄여 그려요(글자도 같이 줄어요).
 * 누르는 영역은 보이는 크기와 [touchMin] 중 큰 쪽이에요.
 */
@Composable
private fun TrickFace(button: TrickButton, label: String, touchMin: Float, fullSize: Float, onTap: () -> Unit) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier = Modifier
            .offset {
                val touch = max(button.size.value, touchMin)
                val c = button.center.value
                IntOffset((c.x + button.shake.value - touch / 2).roundToInt(), (c.y - touch / 2).roundToInt())
            }
            .layout { measurable, _ ->
                val touch = max(button.size.value, touchMin).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(touch, touch))
                layout(touch, touch) { placeable.place(0, 0) }
            }
            .graphicsLayer { alpha = button.alpha.value }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    currentOnTap()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(MorningDimens.sizeButtonCircle)
                .graphicsLayer {
                    val scale = button.size.value / fullSize
                    scaleX = scale
                    scaleY = scale
                }
                .background(colors.primaryDefault, CircleShape)
                .padding(horizontal = MorningDimens.space16),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = typography.titleMedium, color = colors.primaryOn, textAlign = TextAlign.Center)
        }
    }
}

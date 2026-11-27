package com.lumian.morningup.feature.ring

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** 누르는 영역은 이 크기 이상이에요(버튼은 결정 155 고침으로 112dp 고정이라 지금은 늘 넘어요). */
private val TrickTouchMin = 48.dp

/** 한 프레임에 셀 시간 상한(초). 화면이 잠깐 멈췄다 돌아와도 버튼이 벽을 뚫고 튀지 않게 해요. */
private const val MAX_FRAME_SECONDS = 0.1f

private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_SECOND = 1_000_000_000f

/** 버튼 속이기 한 번 동안 일어난 일. 울림 화면은 기록을 남기고, 미리 해보기는 아무것도 하지 않아요. */
interface TrickListener {
    /** 화면을 처음 만졌어요. */
    fun onStart() {}

    /** 버튼을 눌렀어요. [seconds]는 처음 만진 때부터 누를 때까지예요. */
    fun onEnd(seconds: Int) {}

    companion object {
        val None = object : TrickListener {}
    }
}

/** 튕기는 버튼의 가운데(px)와 속도(px/초). */
data class Bounce(val center: Offset, val velocity: Offset)

/**
 * 공처럼 튕기기(결정 155). 버튼은 같은 빠르기로 곧게 가다가 벽에 닿으면 반사해요.
 * 반사할 때 방향을 ±[JITTER_DEGREES]도 무작위로 틀고, 수평이나 수직에서 [AXIS_GAP_DEGREES]도 안쪽이면 그만큼 밀어
 * 벽을 따라 미끄러지지 않게 해요. 모든 값은 통(울림 화면 안쪽) 기준 px예요.
 */
object TrickBounce {
    const val JITTER_DEGREES = 15.0
    const val AXIS_GAP_DEGREES = 15.0
    private const val RIGHT_ANGLE = 90.0
    private const val FULL_TURN = 360.0

    /** 처음 방향: 무작위인데 수평이나 수직에서 15도 안쪽이면 고쳐요. */
    fun start(center: Offset, speed: Float, random: Random): Bounce {
        val degrees = random.nextDouble() * FULL_TURN
        val radians = Math.toRadians(degrees)
        val raw = Offset((cos(radians) * speed).toFloat(), (sin(radians) * speed).toFloat())
        return Bounce(center, aim(raw, speed, 0.0))
    }

    /** [seconds]만큼 움직여요. 벽([half]만큼 안쪽)에 닿으면 반사하고 방향을 조금 틀어요. */
    fun step(state: Bounce, seconds: Float, area: Size, half: Float, random: Random): Bounce {
        var x = state.center.x + state.velocity.x * seconds
        var y = state.center.y + state.velocity.y * seconds
        var vx = state.velocity.x
        var vy = state.velocity.y
        var bounced = false
        val (minX, maxX) = range(half, area.width)
        val (minY, maxY) = range(half, area.height)
        if (x < minX) {
            x = minX + (minX - x)
            vx = abs(vx)
            bounced = true
        } else if (x > maxX) {
            x = maxX - (x - maxX)
            vx = -abs(vx)
            bounced = true
        }
        if (y < minY) {
            y = minY + (minY - y)
            vy = abs(vy)
            bounced = true
        } else if (y > maxY) {
            y = maxY - (y - maxY)
            vy = -abs(vy)
            bounced = true
        }
        val center = Offset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
        val velocity = Offset(vx, vy)
        if (!bounced) return Bounce(center, velocity)
        val speed = hypot(vx, vy)
        val turn = (random.nextDouble() * 2 - 1) * JITTER_DEGREES
        return Bounce(center, aim(velocity, speed, turn))
    }

    /** [velocity]가 가는 사분면은 그대로 두고, 각도를 [turn]만큼 튼 뒤 축에서 15도 이상 떨어지게 해요. */
    private fun aim(velocity: Offset, speed: Float, turn: Double): Offset {
        val sx = if (velocity.x < 0) -1f else 1f
        val sy = if (velocity.y < 0) -1f else 1f
        val degrees = (Math.toDegrees(atan2(abs(velocity.y), abs(velocity.x)).toDouble()) + turn)
            .coerceIn(AXIS_GAP_DEGREES, RIGHT_ANGLE - AXIS_GAP_DEGREES)
        val radians = Math.toRadians(degrees)
        return Offset(sx * (cos(radians) * speed).toFloat(), sy * (sin(radians) * speed).toFloat())
    }

    private fun range(half: Float, length: Float): Pair<Float, Float> =
        if (length - half < half) length / 2 to length / 2 else half to length - half

    /** 통 안 무작위 자리(누르는 영역이 밖으로 나가지 않게). */
    fun randomCenter(area: Size, half: Float, random: Random): Offset {
        val (minX, maxX) = range(half, area.width)
        val (minY, maxY) = range(half, area.height)
        return Offset(minX + random.nextFloat() * (maxX - minX), minY + random.nextFloat() * (maxY - minY))
    }
}

/**
 * 울림 화면이 한 번 뜰 동안의 버튼 속이기(결정 146, 155). 화면이 새로 뜨면 새로 만들어 처음부터 다시 걸려요.
 * - [firstTouchAt]: 화면을 처음 만진 때. 누르기까지 걸린 시간을 재요.
 * - 버튼은 울리는 내내 튕겨요(결정 190, 옛 20초 제한을 없앰). 누를 때까지 멈추지 않아요.
 */
@Stable
class TrickRun(val trick: ButtonTrick, internal val random: Random = Random.Default) {
    internal var firstTouchAt by mutableStateOf<Long?>(null)
    private var ended = false

    /** 버튼 가운데(통 기준 px), 보이는 크기(px). 크기는 처음 자리 잡을 때 한 번 정해요. */
    internal val center = Animatable(Offset.Zero, Offset.VectorConverter)
    internal val size = Animatable(0f)
    internal var velocity = Offset.Zero
    internal var placed = false

    /** 통(울림 화면에서 상태 표시줄과 내비게이션 바 안쪽, 화면 기준 px). */
    internal var area by mutableStateOf<Rect?>(null)

    /** 버튼에 손가락이 닿았어요. 움직이는 중이어도 인정해요. */
    internal fun tap(listener: TrickListener, onReal: (Offset) -> Unit) {
        val box = area ?: return
        if (!placed) return
        finish(listener)
        onReal(center.value + box.topLeft)
    }

    internal fun touched(listener: TrickListener) {
        if (firstTouchAt != null) return
        firstTouchAt = SystemClock.elapsedRealtime()
        listener.onStart()
    }

    /** 버튼을 눌렀어요. 한 번만 알려요. */
    fun finish(listener: TrickListener) {
        if (ended) return
        ended = true
        val seconds = firstTouchAt?.let { ((SystemClock.elapsedRealtime() - it) / MILLIS_PER_SECOND).toInt() } ?: 0
        listener.onEnd(seconds)
    }
}

/**
 * 화면 어디든 처음 닿은 때를 알려요(속이기 시작 기록). 울림 화면 내용을 감싼 부모에 달아요.
 * 부모에서 앞 단계(Initial)로 보기만 하고 소비하지 않아서, 아래 미루기 버튼과 움직이는 버튼이 그대로 눌려요(ISS-066).
 */
fun Modifier.trickTouchWatch(run: TrickRun, listener: () -> TrickListener): Modifier = pointerInput(run) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        run.touched(listener())
    }
}

/**
 * 버튼 속이기 층. 울림 화면 위에 꽉 차게 놓고 버튼 하나를 그려요(하늘, 날짜, 시각 위로도 지나가요).
 * 층 자체는 터치를 받지 않고 버튼 자리만 받아요. 그래서 버튼이 없는 곳의 미루기 버튼이 눌려요(ISS-066).
 * 원래 버튼 자리는 비워 두고, 버튼은 울리는 내내 튕겨요(결정 190).
 * 움직임은 프레임마다(애니메이션 시계) 계산해요. 빠르기는 1초에 통 너비의 [ButtonTrick.speed] 배예요.
 */
@Composable
fun TrickLayer(run: TrickRun, label: String, listener: TrickListener, onReal: (center: Offset) -> Unit) {
    val density = LocalDensity.current
    val view = LocalView.current
    val currentListener by rememberUpdatedState(listener)
    val currentOnReal by rememberUpdatedState(onReal)
    val maxSize = with(density) { MorningDimens.sizeButtonCircle.toPx() }
    val touchMin = with(density) { TrickTouchMin.toPx() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .onGloballyPositioned { run.area = it.boundsInRoot() },
        ) {
            TrickFace(run, label, touchMin, maxSize) {
                run.tap(currentListener) { center ->
                    view.perform(MorningHaptic.Confirm)
                    currentOnReal(center)
                }
            }
        }
    }

    // 결정 190: 울리는 내내 튕겨요. 누르거나 울림 화면이 닫힐 때까지 멈추지 않아요.
    val area = run.area
    LaunchedEffect(run, area?.size) {
        if (area == null) return@LaunchedEffect
        run.bounce(area.size, maxSize, touchMin)
    }
}

/** 처음이면 자리와 방향을 정하고, 그 뒤로는 프레임마다 움직여요. 크기는 지금 버튼 크기(112dp) 그대로예요(결정 155). */
private suspend fun TrickRun.bounce(area: Size, visible: Float, touchMin: Float) {
    val speed = trick.speed.widthsPerSecond * area.width
    if (!placed) {
        size.snapTo(visible)
        val start = TrickBounce.start(TrickBounce.randomCenter(area, max(visible, touchMin) / 2, random), speed, random)
        center.snapTo(start.center)
        velocity = start.velocity
        placed = true
    }
    val half = max(size.value, touchMin) / 2
    var last = withFrameNanos { it }
    while (currentCoroutineContext().isActive) {
        val now = withFrameNanos { it }
        val seconds = ((now - last) / NANOS_PER_SECOND).coerceIn(0f, MAX_FRAME_SECONDS)
        last = now
        val next = TrickBounce.step(Bounce(center.value, velocity), seconds, area, half, random)
        velocity = next.velocity
        center.snapTo(next.center)
    }
}

/**
 * 버튼 하나. 피그마 Button Circle(112)을 비율 그대로 줄여 그려요(글자도 같이 줄어요).
 * 누르는 영역은 보이는 크기와 [touchMin] 중 큰 쪽이에요. 누르는 순간(손가락이 닿을 때) 인정해요.
 */
@Composable
private fun TrickFace(run: TrickRun, label: String, touchMin: Float, fullSize: Float, onTap: () -> Unit) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier = Modifier
            .offset {
                val touch = max(run.size.value, touchMin)
                val c = run.center.value
                IntOffset((c.x - touch / 2).roundToInt(), (c.y - touch / 2).roundToInt())
            }
            .layout { measurable, _ ->
                val touch = max(run.size.value, touchMin).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(touch, touch))
                layout(touch, touch) { placeable.place(0, 0) }
            }
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
                    val scale = run.size.value / fullSize
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

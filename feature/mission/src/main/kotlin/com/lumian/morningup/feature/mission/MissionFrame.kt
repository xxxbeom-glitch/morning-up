package com.lumian.morningup.feature.mission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.RingStateMachine

/** 미리보기 닫기 이름표(테스트용). */
const val MISSION_CLOSE_TAG = "missionClose"

private val CloseTouch = 48.dp

/** 피그마 M 위 줄 타이머: 지름 40, 고리 두께는 반지름의 15%. */
private val TimerSize = 40.dp
private const val TIMER_STROKE_RATIO = 0.15f
private const val FULL_SWEEP = 360f
private const val TOP_ANGLE = -90f

/** 마지막 10초 숨쉬기(1초에 한 번 커졌다 작아짐), 마지막 5초 1초마다 약한 틱. */
private const val BREATHE_FROM_SECONDS = 10
private const val TICK_FROM_SECONDS = 5
private const val BREATHE_SCALE = 1.08f
private const val BREATHE_HALF_MILLIS = 500

/** 다시 찰 때만 스프링으로 움직여요. 줄어드는 쪽은 시간 그대로 따라가요. */
private const val REFILL_JUMP = 0.02f

private const val MILLIS_PER_SECOND = 1_000L

/** 빛 덩어리 자리(가로, 세로 비율)와 반지름(화면 폭 비율). Z1 잠들기와 같은 배치예요. */
private val GlowSpots = listOf(
    Triple(0.15f, 0.12f, 0.75f),
    Triple(0.9f, 0.42f, 0.65f),
    Triple(0.25f, 0.88f, 0.7f),
)

/**
 * 미션 화면 공통 틀. 어두운 바탕(미션마다 색만 다름), 공통 헤더(결정 89: 위 타임라인 바, 그 아래 20 띄워 가운데 22 크기
 * 상태 글 '정답 4 / 10'), 내용. 원형 타이머는 헤더에 쓰지 않아요.
 * - [status]와 [remainingMillis]가 null이면 헤더에 그것을 그리지 않아요(Shake Shake는 헤더 없이 큰 고리만 써요).
 * - [onClose]가 있으면(미리보기) 왼쪽 위에 닫기를 두고 뒤로 가기도 닫기예요.
 *   없으면(진짜 울림) 뒤로 가기는 아무것도 하지 않아요(미션 중 빠져나가지 못해요).
 */
@Composable
fun MissionFrame(
    backdrop: MissionColors.Backdrop,
    status: String?,
    remainingMillis: Long?,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = true) { onClose?.invoke() }
    Box(modifier = modifier.fillMaxSize().missionBackdrop(backdrop)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = MorningDimens.space20),
        ) {
            // 헤더는 글이나 타이머가 있을 때만 만들어요. 닫기만 있으면(M4 미리보기) 겹쳐 둬서 고리가 화면 가운데에 남아요.
            if (status != null || remainingMillis != null) {
                MissionHeader(status = status, remainingMillis = remainingMillis, onClose = onClose)
            }
            content()
        }
        if (status == null && remainingMillis == null && onClose != null) {
            Box(
                Modifier
                    .systemBarsPadding()
                    .padding(start = MorningDimens.space20, top = MorningDimens.space12),
            ) { CloseButton(onClose) }
        }
    }
}

/** 피그마 Mission Top Bar(결정 89): 타임라인 바(높이 6, 모서리 3), 아래 20 띄워 상태 글 22 Bold 흰색 가운데. */
private val TimelineHeight = 6.dp
private val TimelineRadius = 3.dp
private val HeaderGap = 20.dp
private val HeaderStatusSize = 22.sp

/** 타임라인 바 이름표(테스트용). */
const val MISSION_TIMELINE_TAG = "missionTimeline"

@Composable
private fun MissionHeader(status: String?, remainingMillis: Long?, onClose: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = MorningDimens.space12)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onClose != null) CloseButton(onClose)
            if (remainingMillis != null) {
                MissionTimeline(remainingMillis = remainingMillis, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        if (status != null) {
            Spacer(Modifier.height(HeaderGap))
            Text(
                text = status,
                style = MorningTheme.typography.headlineLarge.copy(
                    fontSize = HeaderStatusSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = MissionColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 남은 시간 막대(초 숫자 없음). 흰 18% 바탕에 흰 막대가 남은 만큼 남아요.
 * 조작해서 다시 차면 스프링으로 차오르고, 마지막 5초는 1초마다 약하게 진동해요(원형 타이머와 같은 규칙).
 */
@Composable
fun MissionTimeline(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    totalMillis: Long = RingStateMachine.MISSION_MILLIS,
) {
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val total = totalMillis.toFloat()
    val target = (remainingMillis / total).coerceIn(0f, 1f)
    val fraction = remember { Animatable(target) }
    val latestTarget by rememberUpdatedState(target)
    val latestReduceMotion by rememberUpdatedState(reduceMotion)
    LaunchedEffect(fraction) {
        snapshotFlow { latestTarget }.collect { next ->
            if (next > fraction.value + REFILL_JUMP) {
                fraction.animateTo(next, MorningMotion.spatial(MorningMotion.SpatialDefault, latestReduceMotion))
                fraction.snapTo(latestTarget)
            } else {
                fraction.snapTo(next)
            }
        }
    }
    val secondsLeft = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    LaunchedEffect(secondsLeft) {
        if (secondsLeft in 1..TICK_FROM_SECONDS) view.perform(MorningHaptic.Tick)
    }
    val description = stringResource(R.string.mission_timer_description, secondsLeft)
    Canvas(
        modifier = modifier
            .height(TimelineHeight)
            .testTag(MISSION_TIMELINE_TAG)
            .semantics { contentDescription = description },
    ) {
        val radius = CornerRadius(TimelineRadius.toPx())
        drawRoundRect(color = MissionColors.TimerTrack, cornerRadius = radius)
        val width = size.width * fraction.value
        if (width > 0f) {
            drawRoundRect(color = MissionColors.TimerFill, size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

/** 미리보기 왼쪽 위 닫기(Lucide X). 누르는 곳 48. */
@Composable
private fun CloseButton(onClose: () -> Unit) {
    val view = LocalView.current
    val description = stringResource(R.string.mission_preview_close)
    Box(
        modifier = Modifier
            .size(CloseTouch)
            .testTag(MISSION_CLOSE_TAG)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                role = Role.Button,
            ) {
                view.perform(MorningHaptic.Press)
                onClose()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Lucide.X,
            contentDescription = null,
            tint = MissionColors.Text,
            modifier = Modifier.size(MorningDimens.iconMd),
        )
    }
}

private fun Modifier.missionBackdrop(backdrop: MissionColors.Backdrop): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(backdrop.top, backdrop.bottom)))
    backdrop.glows.zip(MissionColors.GlowAlphas).zip(GlowSpots).forEach { (glow, spot) ->
        val (color, alpha) = glow
        val (x, y, r) = spot
        val center = Offset(size.width * x, size.height * y)
        val radius = size.width * r
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

/**
 * 59초 고리. 남은 만큼 흰 호가 남아요. 조작해서 다시 차면 스프링으로 차오르고,
 * 마지막 10초는 숨쉬듯 커졌다 작아지고, 마지막 5초는 1초마다 약하게 진동해요.
 */
/** 위 원형 타이머 가운데 남은 초 글자 크기. */
private val TimerSecondsSize = 14.sp

@Composable
fun MissionTimer(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    diameter: Dp = TimerSize,
    strokeWidth: Dp? = null,
    track: Color = MissionColors.TimerTrack,
    showSeconds: Boolean = true,
) {
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val total = RingStateMachine.MISSION_MILLIS.toFloat()
    val target = (remainingMillis / total).coerceIn(0f, 1f)
    val fraction = remember { Animatable(target) }
    val latestTarget by rememberUpdatedState(target)
    val latestReduceMotion by rememberUpdatedState(reduceMotion)
    // 남은 시간은 매 프레임 바뀌어요. 값마다 다시 시작하면 다시 채우는 움직임이 처음부터 되풀이돼 원이 덜 찬 채 멈춰요.
    // 그래서 한 줄로 받아, 다시 채울 때는 움직임을 끝까지 마친 뒤 최신 값으로 이어가요.
    LaunchedEffect(fraction) {
        snapshotFlow { latestTarget }.collect { next ->
            if (next > fraction.value + REFILL_JUMP) {
                fraction.animateTo(next, MorningMotion.spatial(MorningMotion.SpatialDefault, latestReduceMotion))
                fraction.snapTo(latestTarget)
            } else {
                fraction.snapTo(next)
            }
        }
    }
    val secondsLeft = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    LaunchedEffect(secondsLeft) {
        if (secondsLeft in 1..TICK_FROM_SECONDS) view.perform(MorningHaptic.Tick)
    }
    val breathing = secondsLeft in 1..BREATHE_FROM_SECONDS && !reduceMotion
    val pulse = rememberInfiniteTransition(label = "timerBreath")
    val breath by pulse.animateFloat(
        initialValue = 1f,
        targetValue = BREATHE_SCALE,
        animationSpec = infiniteRepeatable(tween(BREATHE_HALF_MILLIS), RepeatMode.Reverse),
        label = "breath",
    )
    val description = stringResource(R.string.mission_timer_description, secondsLeft)
    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                val scale = if (breathing) breath else 1f
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        TimerArcs(fraction.value, strokeWidth, track)
        // 가운데 남은 초(Bold 14, 데모와 피그마 Mission Timer와 같음). 큰 흔들기 고리는 숫자를 따로 그려요.
        if (showSeconds) {
            Text(
                text = secondsLeft.toString(),
                style = MorningTheme.typography.captionStrong.copy(
                    fontSize = TimerSecondsSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = MissionColors.Text,
            )
        }
    }
}

@Composable
private fun TimerArcs(fraction: Float, strokeWidth: Dp?, track: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = strokeWidth?.toPx() ?: (size.minDimension / 2 * TIMER_STROKE_RATIO)
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(track, 0f, FULL_SWEEP, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(
            color = MissionColors.TimerFill,
            startAngle = TOP_ANGLE,
            sweepAngle = FULL_SWEEP * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** 남은 시간 밀리초를 [endsAt]과 [nowMillis]로 계산하는 작은 도우미(화면 루프에서 써요). */
internal fun remainingOf(endsAt: Long?, nowMillis: Long): Long = ((endsAt ?: nowMillis) - nowMillis).coerceAtLeast(0)

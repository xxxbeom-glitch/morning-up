package com.lumian.morningup.feature.mission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.RingStateMachine

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
 * 미션 화면 공통 틀. 어두운 바탕(미션마다 색만 다름), 위 줄(왼쪽 상태 글, 오른쪽 59초 고리), 내용.
 * 뒤로 가기는 아무것도 하지 않아요(미션 중 빠져나가지 못해요).
 */
@Composable
fun MissionFrame(
    backdrop: MissionColors.Backdrop,
    status: String,
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = true) {}
    Box(modifier = modifier.fillMaxSize().missionBackdrop(backdrop)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = MorningDimens.space20),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MorningDimens.space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = status,
                    style = MorningTheme.typography.bodyMediumStrong.copy(fontWeight = FontWeight.Bold),
                    color = MissionColors.TextStatus,
                    modifier = Modifier.weight(1f),
                )
                MissionTimer(remainingMillis = remainingMillis)
            }
            content()
        }
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
@Composable
fun MissionTimer(remainingMillis: Long, modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val total = RingStateMachine.MISSION_MILLIS.toFloat()
    val target = (remainingMillis / total).coerceIn(0f, 1f)
    val fraction = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (target > fraction.value + REFILL_JUMP) {
            fraction.animateTo(target, MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion))
        } else {
            fraction.snapTo(target)
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
    Canvas(
        modifier = modifier
            .size(TimerSize)
            .graphicsLayer {
                val scale = if (breathing) breath else 1f
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = description },
    ) {
        val stroke = size.minDimension / 2 * TIMER_STROKE_RATIO
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(MissionColors.TimerTrack, 0f, FULL_SWEEP, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(
            color = MissionColors.TimerFill,
            startAngle = TOP_ANGLE,
            sweepAngle = FULL_SWEEP * fraction.value,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** 남은 시간 밀리초를 [endsAt]과 [nowMillis]로 계산하는 작은 도우미(화면 루프에서 써요). */
internal fun remainingOf(endsAt: Long?, nowMillis: Long): Long = ((endsAt ?: nowMillis) - nowMillis).coerceAtLeast(0)

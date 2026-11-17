package com.lumian.morningup.feature.ring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.DayPart
import kotlin.math.hypot
import kotlinx.coroutines.delay

/** W2 '깼어요' 뒤 체크를 보여 주고 닫기까지. */
private const val AWAKE_CLOSE_DELAY_MILLIS = 600L

/** W2 남은 시간 다시 그리는 간격. */
private const val COUNTDOWN_FRAME_MILLIS = 200L

/**
 * 울림 화면 묶음(D0, M, W1, W2). 잠금 화면 위 액티비티(:app의 RingActivity)가 이것 하나만 그려요.
 * - 미션 화면은 [missionContent] 자리에 :app이 `:feature:mission`을 꽂아요(feature끼리 직접 부르지 않아요).
 * - D0에서 미션으로 넘어갈 때 버튼 자리에서 원형으로 퍼지며 열려요. '애니메이션 줄이기'면 페이드예요.
 * - 보여 줄 화면이 없어지면 [onClose]를 불러요. [canClose]가 false면(저장된 울림을 읽는 중) 기다려요.
 * - W1 '확인'은 [onWakeUpConfirmed]로 알려요(잠겨 있었으면 닫고, 풀려 있었으면 홈으로 가는 일은 액티비티 몫).
 */
@Composable
fun RingRoute(
    canClose: Boolean,
    onClose: () -> Unit,
    onWakeUpConfirmed: () -> Unit,
    missionContent: @Composable (index: Int) -> Unit,
    viewModel: RingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var awakeTapped by remember { mutableStateOf(false) }
    // 닫히는 동안(W1 확인 뒤, W2 체크 뒤)에도 마지막 화면을 그대로 그려요.
    val last = remember { LastScreen() }
    if (state.screen != RingScreen.None) last.screen = state.screen
    val lastShown = last.screen
    LaunchedEffect(state.screen) {
        if (state.screen != RingScreen.WakeCheck && state.screen != RingScreen.None) awakeTapped = false
    }

    val confirmWakeUp = {
        viewModel.onWakeUpClosed()
        onWakeUpConfirmed()
    }
    // 울리는 동안 뒤로 가기로 빠져나가지 못해요. W1만 '확인'과 같게 닫아요.
    BackHandler(enabled = true) {
        if (state.screen == RingScreen.WakeUp) confirmWakeUp()
    }
    LaunchedEffect(state.screen, canClose) {
        if (state.screen == RingScreen.None && canClose) {
            if (last.screen == RingScreen.WakeCheck) delay(AWAKE_CLOSE_DELAY_MILLIS)
            onClose()
        }
    }

    val night = state.dayPart == DayPart.Night
    MorningTheme(darkTheme = night, reduceMotion = MorningTheme.reduceMotion) {
        RingContent(
            state = state,
            shown = lastShown,
            awakeTapped = awakeTapped,
            nowMillis = viewModel::nowMillis,
            onMainButton = viewModel::onMainButton,
            onWakeUpConfirmed = confirmWakeUp,
            onAwake = {
                awakeTapped = true
                viewModel.onAwake()
            },
            missionContent = missionContent,
        )
    }
}

private class LastScreen(var screen: RingScreen = RingScreen.None)

@Composable
private fun RingContent(
    state: RingUiState,
    shown: RingScreen,
    awakeTapped: Boolean,
    nowMillis: () -> Long,
    onMainButton: () -> Unit,
    onWakeUpConfirmed: () -> Unit,
    onAwake: () -> Unit,
    missionContent: @Composable (index: Int) -> Unit,
) {
    var buttonCenter by remember { mutableStateOf<Offset?>(null) }
    val ringing = @Composable {
        RingingScreen(
            date = state.date,
            time = state.time,
            dayPart = state.dayPart,
            hasMissions = state.hasMissions,
            onMainButton = onMainButton,
            onButtonCenter = { buttonCenter = it },
        )
    }
    when (shown) {
        RingScreen.None, RingScreen.Ringing -> ringing()
        is RingScreen.Mission -> MissionReveal(center = buttonCenter, under = ringing) {
            // 미션 순서가 바뀌어도 같은 자리를 유지해요(미션 사이 넘어가기는 미션 화면이 그려요).
            missionContent(shown.index)
        }
        RingScreen.WakeUp -> WakeUpScreen(dayPart = state.dayPart, onConfirm = onWakeUpConfirmed)
        RingScreen.WakeCheck -> {
            val endsAt = state.wakeCheckEndsAt
            var remaining by remember { mutableLongStateOf(endsAt?.minus(nowMillis()) ?: 0L) }
            LaunchedEffect(endsAt, awakeTapped) {
                while (endsAt != null && !awakeTapped) {
                    remaining = (endsAt - nowMillis()).coerceAtLeast(0)
                    delay(COUNTDOWN_FRAME_MILLIS)
                }
            }
            WakeCheckScreen(
                dayPart = state.dayPart,
                answerSeconds = state.wakeCheckAnswerSeconds,
                remainingMillis = remaining,
                confirmed = awakeTapped,
                onAwake = onAwake,
            )
        }
    }
}

/**
 * D0 버튼 자리에서 원형으로 퍼지며 미션 화면이 열려요(`spatialSlow`). 버튼 자리를 모르면(앱이 미션 중에 다시 뜬 경우)
 * 가운데에서 퍼져요. '애니메이션 줄이기'면 짧은 페이드예요.
 */
@Composable
private fun MissionReveal(center: Offset?, under: @Composable () -> Unit, content: @Composable () -> Unit) {
    val reduceMotion = MorningTheme.reduceMotion
    val progress = remember { Animatable(if (center == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, MorningMotion.spatial(MorningMotion.SpatialSlow, reduceMotion))
    }
    val revealing = progress.value < 1f
    Box(Modifier.fillMaxSize()) {
        if (revealing) under()
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    if (reduceMotion) {
                        alpha = p
                    } else if (p < 1f) {
                        val origin = center ?: Offset(size.width / 2, size.height / 2)
                        clip = true
                        shape = CircleRevealShape(origin, p * farthestCorner(origin, size))
                    }
                },
        ) { content() }
    }
}

/** [origin]에서 가장 먼 모서리까지 거리. 이 반지름이면 화면을 다 덮어요. */
private fun farthestCorner(origin: Offset, size: Size): Float = maxOf(
    hypot(origin.x, origin.y),
    hypot(size.width - origin.x, origin.y),
    hypot(origin.x, size.height - origin.y),
    hypot(size.width - origin.x, size.height - origin.y),
)

private class CircleRevealShape(private val origin: Offset, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(origin, radius)) })
}

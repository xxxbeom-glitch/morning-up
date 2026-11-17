package com.lumian.morningup.feature.mission

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.feature.mission.play.MissionHostViewModel
import com.lumian.morningup.feature.mission.play.MissionPlay
import com.lumian.morningup.feature.mission.play.MissionPreviewViewModel
import com.lumian.morningup.feature.mission.play.MissionUiState
import com.lumian.morningup.feature.mission.play.RingMissionViewModel

private val DoneCircle = 72.dp
private val DoneIcon = 36.dp

/** 미션 화면에서 받는 조작 묶음. */
data class MissionActions(
    val onMathKey: (MathKey) -> Unit = {},
    val onQuizAnswer: (Int) -> Unit = {},
    val onDropKey: (String) -> Unit = {},
    val onDropDelete: () -> Unit = {},
    val onDropSubmit: () -> Unit = {},
)

/** :app이 울림 화면의 미션 자리에 꽂는 연결 함수. */

/** 미션 화면은 높이의 1/3 아래에서 올라와요. */
private const val PLAY_ENTER_DIVISOR = 3

@Composable
fun RingMissionRoute(viewModel: RingMissionViewModel = hiltViewModel()) {
    MissionHostRoute(viewModel = viewModel, onClose = null)
}

/** 미리보기 길. [onClosed]는 닫힐 때(닫기, 59초 끝, '잘했어요' 뒤, 진짜 알람) 한 번 불러요. */
@Composable
fun MissionPreviewRouteScreen(onClosed: () -> Unit, viewModel: MissionPreviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onClosed() }
    MissionHostRoute(viewModel = viewModel, onClose = viewModel::close)
}

@Composable
private fun MissionHostRoute(viewModel: MissionHostViewModel, onClose: (() -> Unit)?) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(viewModel.nowMillis()) }
    LaunchedEffect(viewModel) {
        while (true) {
            withFrameMillis { }
            viewModel.onFrame()
            now = viewModel.nowMillis()
        }
    }
    if (state.play is MissionPlay.Shake && state.transitionTo == null && !state.completed) {
        ShakeSensor(onShake = viewModel::onShake)
    }
    MissionHostScreen(
        state = state,
        nowMillis = now,
        actions = MissionActions(
            onMathKey = viewModel::onMathKey,
            onQuizAnswer = viewModel::onQuizAnswer,
            onDropKey = viewModel::onDropKey,
            onDropDelete = viewModel::onDropDelete,
            onDropSubmit = viewModel::onDropSubmit,
        ),
        onClose = onClose,
    )
}

/** 화면에 보일 단계: 미션, '다음 미션 n / total', '잘했어요'. */
private sealed interface Stage {
    data class Play(val index: Int) : Stage

    data class Next(val number: Int, val total: Int) : Stage

    data object Done : Stage
}

/** 미션 화면(상태만 받아 그려요). 울림과 미리보기가 같이 써요. */
@Composable
fun MissionHostScreen(state: MissionUiState, nowMillis: Long, actions: MissionActions, onClose: (() -> Unit)?) {
    val play = state.play ?: return
    val remaining = remainingOf(state.timerEndsAt, nowMillis)
    val stage: Stage = when {
        state.completed -> Stage.Done
        state.transitionTo != null -> Stage.Next(state.transitionTo, state.total)
        else -> Stage.Play(state.index)
    }
    val playing = stage is Stage.Play
    MissionFrame(
        backdrop = backdropOf(play.type),
        status = if (playing) statusOf(play) else null,
        remainingMillis = if (playing && play !is MissionPlay.Shake) remaining else null,
        onClose = onClose,
    ) {
        val reduceMotion = MorningTheme.reduceMotion
        AnimatedContent(
            targetState = stage,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { stageTransition(targetState, reduceMotion) },
            label = "missionStage",
        ) { shown ->
            Column(Modifier.fillMaxSize()) {
                when (shown) {
                    is Stage.Play -> PlayContent(
                        play = play,
                        nowMillis = nowMillis,
                        remaining = remaining,
                        actions = actions,
                    )
                    is Stage.Next -> CenterMessage(
                        text = stringResource(R.string.mission_next, shown.number, shown.total),
                    )
                    Stage.Done -> CenterMessage(text = stringResource(R.string.mission_preview_done), check = true)
                }
            }
        }
    }
}

private fun stageTransition(target: Stage, reduceMotion: Boolean): ContentTransform {
    val exit = fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault))
    return when (target) {
        is Stage.Play -> slideInVertically(
            MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion),
        ) {
            it / PLAY_ENTER_DIVISOR
        } +
            fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)) togetherWith exit
        else -> scaleIn(MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion), initialScale = 0.6f) +
            fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)) togetherWith exit
    }
}

@Composable
private fun ColumnScope.PlayContent(play: MissionPlay, nowMillis: Long, remaining: Long, actions: MissionActions) {
    when (play) {
        is MissionPlay.Math -> QuickMathContent(play = play, onKey = actions.onMathKey)
        is MissionPlay.Quiz -> PopQuizContent(play = play, onAnswer = actions.onQuizAnswer)
        is MissionPlay.Drop -> DropWordsContent(
            play = play,
            nowMillis = nowMillis,
            onKey = actions.onDropKey,
            onDelete = actions.onDropDelete,
            onSubmit = actions.onDropSubmit,
        )
        is MissionPlay.Shake -> ShakeShakeContent(play = play, remainingMillis = remaining)
    }
}

/** 가운데 글(넘어가기, '잘했어요'). 나타날 때 `CONFIRM`. */
@Composable
private fun ColumnScope.CenterMessage(text: String, check: Boolean = false) {
    val view = LocalView.current
    LaunchedEffect(Unit) { view.perform(MorningHaptic.Confirm) }
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (check) {
                Box(
                    modifier = Modifier.size(DoneCircle).background(MissionColors.TimerFill, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = MissionColors.OnConfirm,
                        modifier = Modifier.size(DoneIcon),
                    )
                }
                Spacer(Modifier.height(MorningDimens.space16))
            }
            Text(
                text = text,
                style = MorningTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MissionColors.Text,
            )
        }
    }
}

@Composable
private fun statusOf(play: MissionPlay): String? = when (play) {
    // 결정 92: '연속'을 빼고 세 미션 모두 '정답 2 / 3'.
    is MissionPlay.Math -> stringResource(R.string.mission_correct, play.game.correct, play.game.target)
    is MissionPlay.Quiz -> stringResource(R.string.mission_correct, play.game.correct, play.game.target)
    is MissionPlay.Drop -> stringResource(R.string.mission_correct, play.game.correct, play.game.target)
    is MissionPlay.Shake -> null
}

internal fun backdropOf(type: MissionType): MissionColors.Backdrop = when (type) {
    MissionType.DropWords -> MissionColors.DropWords
    MissionType.PopQuiz -> MissionColors.PopQuiz
    MissionType.QuickMath -> MissionColors.QuickMath
    MissionType.ShakeShake -> MissionColors.ShakeShake
}

package com.lumian.morningup.feature.mission

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.QuizCategory
import com.lumian.morningup.core.model.mission.QuizRound
import com.lumian.morningup.feature.mission.play.FeedbackKind
import com.lumian.morningup.feature.mission.play.MissionFeedback
import com.lumian.morningup.feature.mission.play.MissionPlay

/** 답 줄 이름표(테스트용). `"$QUIZ_ANSWER_TAG$index"`. */
const val QUIZ_ANSWER_TAG = "quizAnswer"

private const val QUESTION_MAX_LINES = 2

/**
 * M2 Pop Quiz 내용. 가운데 분야, 문제, 안내, 아래 답 4줄(폭 꽉 참, 사이 10).
 * 답을 고르면 새 문제가 아래에서 올라오고, 물러나는 이전 문제에서 고른 답이 정답이면 칠이 바뀌고 오답이면 흔들려요.
 * 정답 줄은 보여 주지 않아요(dev-plan 모션 표). 새 문제는 바로 누를 수 있어요.
 */
@Composable
internal fun ColumnScope.PopQuizContent(play: MissionPlay.Quiz, onAnswer: (Int) -> Unit) {
    val view = LocalView.current
    val reduceMotion = MorningTheme.reduceMotion
    val feedback = play.feedback
    LaunchedEffect(feedback?.id) {
        when (feedback?.kind) {
            FeedbackKind.Correct, FeedbackKind.Finished -> view.perform(MorningHaptic.Confirm)
            FeedbackKind.Wrong -> view.perform(MorningHaptic.Reject)
            null -> Unit
        }
    }
    AnimatedContent(
        targetState = play.game.round,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        transitionSpec = {
            val spec = MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion)
            (slideInVertically(spec) { it / 2 } + fadeIn()) togetherWith
                (slideOutVertically(spec) { -it / 2 } + fadeOut())
        },
        label = "quizRound",
    ) { round ->
        val marked = feedback?.takeIf { it.round == round }
        QuizRoundView(round = round, marked = marked, onAnswer = if (marked == null) onAnswer else null)
    }
}

@Composable
private fun QuizRoundView(round: QuizRound, marked: MissionFeedback?, onAnswer: ((Int) -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(categoryName(round.question.category)),
                style = MissionText.guide,
                color = MissionColors.TextGuide,
            )
            Spacer(Modifier.height(MorningDimens.space12))
            Text(
                text = round.question.question,
                style = MissionText.question,
                color = MissionColors.Text,
                textAlign = TextAlign.Center,
                maxLines = QUESTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(MorningDimens.space12))
            Text(
                text = stringResource(R.string.mission_math_hint),
                style = MissionText.hint,
                color = MissionColors.TextHint,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MissionGap)) {
            round.choices.forEachIndexed { index, choice ->
                AnswerRow(
                    text = choice,
                    kind = marked?.takeIf { it.choice == index }?.kind,
                    onClick = onAnswer?.let { { it(index) } },
                    modifier = Modifier.testTag("$QUIZ_ANSWER_TAG$index"),
                )
            }
        }
        Spacer(Modifier.height(MorningDimens.space24))
    }
}

@Composable
private fun AnswerRow(text: String, kind: FeedbackKind?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(kind) {
        if (kind == FeedbackKind.Wrong) shake.shakeSideways(with(density) { ShakeDistance.toPx() })
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = when {
            kind == FeedbackKind.Correct || kind == FeedbackKind.Finished -> MissionColors.Success
            pressed -> MissionColors.FillPressed
            else -> MissionColors.Fill
        },
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "answerFill",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MissionRowHeight)
            .graphicsLayer { translationX = shake.value }
            .background(fill, RoundedCornerShape(MissionRowRadius))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = onClick != null,
                role = Role.Button,
            ) { onClick?.invoke() }
            .padding(horizontal = MorningDimens.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MissionText.answer,
            color = MissionColors.Text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun categoryName(category: QuizCategory): Int = when (category) {
    QuizCategory.Economy -> R.string.mission_quiz_category_economy
    QuizCategory.Society -> R.string.mission_quiz_category_society
    QuizCategory.KoreanHistory -> R.string.mission_quiz_category_korean_history
    QuizCategory.WorldHistory -> R.string.mission_quiz_category_world_history
    QuizCategory.Science -> R.string.mission_quiz_category_science
    QuizCategory.People -> R.string.mission_quiz_category_people
    QuizCategory.Issues -> R.string.mission_quiz_category_issues
}

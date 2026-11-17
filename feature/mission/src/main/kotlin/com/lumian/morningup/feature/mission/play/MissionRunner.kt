package com.lumian.morningup.feature.mission.play

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.DropInput
import com.lumian.morningup.core.model.mission.DropWordsGame
import com.lumian.morningup.core.model.mission.DropWordsState
import com.lumian.morningup.core.model.mission.FallingWord
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.MissionContent
import com.lumian.morningup.core.model.mission.PopQuizGame
import com.lumian.morningup.core.model.mission.PopQuizState
import com.lumian.morningup.core.model.mission.QuickMathGame
import com.lumian.morningup.core.model.mission.QuickMathState
import com.lumian.morningup.core.model.mission.QuizResult
import com.lumian.morningup.core.model.mission.QuizRound
import com.lumian.morningup.core.model.mission.ShakeShake
import com.lumian.morningup.core.model.mission.ShakeShakeState
import com.lumian.morningup.core.model.mission.SubmitResult
import kotlin.random.Random

enum class FeedbackKind { Correct, Wrong, Finished }

/**
 * 정답, 오답 한 번. [id]가 바뀔 때마다 화면이 움직임과 진동을 한 번 내요.
 * - [choice], [round]: Pop Quiz에서 고른 답 자리와 그때 문제(흔들거나 칠한 뒤 새 문제로 바꿔요).
 * - [word]: Drop Words에서 맞힌 단어(1.2배로 커지며 사라지는 그림에 써요).
 */
data class MissionFeedback(
    val id: Int,
    val kind: FeedbackKind,
    val choice: Int = -1,
    val round: QuizRound? = null,
    val word: FallingWord? = null,
)

/** 지금 하는 미션 한 판의 화면 상태. */
sealed interface MissionPlay {
    val type: MissionType
    val feedback: MissionFeedback?
    val finished: Boolean

    data class Math(val game: QuickMathState, override val feedback: MissionFeedback? = null) : MissionPlay {
        override val type get() = MissionType.QuickMath
        override val finished get() = game.finished
    }

    data class Quiz(val game: PopQuizState, override val feedback: MissionFeedback? = null) : MissionPlay {
        override val type get() = MissionType.PopQuiz
        override val finished get() = game.finished
    }

    data class Drop(val game: DropWordsState, override val feedback: MissionFeedback? = null) : MissionPlay {
        override val type get() = MissionType.DropWords
        override val finished get() = game.finished
    }

    data class Shake(val game: ShakeShakeState, override val feedback: MissionFeedback? = null) : MissionPlay {
        override val type get() = MissionType.ShakeShake
        override val finished get() = game.finished
    }
}

/** 조작 한 번의 결과. [Interacted]면 59초를 다시 채우고, [Finished]면 미션이 끝났어요. */
enum class Outcome { Ignored, Interacted, Finished }

/**
 * 미션 한 판을 돌려요(울림과 미리보기가 같이 써요). 규칙은 :core:model의 순수 게임이 정하고,
 * 여기서는 조작을 게임에 넘기고 화면 상태([play])와 피드백 번호만 관리해요.
 */
class MissionRunner(setting: MissionSetting, content: MissionContent, random: Random, nowMillis: Long) {
    private val normalized = setting.normalized()
    private var feedbackId = 0

    private val math: QuickMathGame? =
        if (normalized.type ==
            MissionType.QuickMath
        ) {
            QuickMathGame(normalized.level, normalized.count, random)
        } else {
            null
        }
    private val quiz: PopQuizGame? =
        if (normalized.type == MissionType.PopQuiz) {
            PopQuizGame(normalized.level, normalized.count, content.popQuiz, random)
        } else {
            null
        }
    private val drop: DropWordsGame? =
        if (normalized.type == MissionType.DropWords) {
            DropWordsGame(normalized.level, normalized.speed, normalized.count, content.words(normalized.level), random)
        } else {
            null
        }

    var play: MissionPlay = when {
        math != null -> MissionPlay.Math(math.start())
        quiz != null -> MissionPlay.Quiz(quiz.start())
        drop != null -> MissionPlay.Drop(drop.start(nowMillis))
        else -> MissionPlay.Shake(ShakeShake.start(normalized.count))
    }
        private set

    private fun feedback(kind: FeedbackKind, choice: Int = -1, round: QuizRound? = null, word: FallingWord? = null) =
        MissionFeedback(++feedbackId, kind, choice, round, word)

    fun mathKey(key: MathKey): Outcome {
        val game = math ?: return Outcome.Ignored
        val current = play as MissionPlay.Math
        if (current.finished) return Outcome.Ignored
        when (key) {
            is MathKey.Digit -> play = current.copy(game = game.type(current.game, key.value))
            MathKey.Delete -> play = current.copy(game = game.delete(current.game))
            MathKey.Confirm -> {
                val (next, result) = game.submit(current.game)
                val kind = when (result) {
                    SubmitResult.Correct -> FeedbackKind.Correct
                    SubmitResult.Wrong -> FeedbackKind.Wrong
                    SubmitResult.Finished -> FeedbackKind.Finished
                    SubmitResult.Empty -> null
                }
                if (kind != null) play = current.copy(game = next, feedback = feedback(kind))
                if (kind == FeedbackKind.Finished) return Outcome.Finished
            }
        }
        return Outcome.Interacted
    }

    fun quizAnswer(index: Int): Outcome {
        val game = quiz ?: return Outcome.Ignored
        val current = play as MissionPlay.Quiz
        val round = current.game.round
        val (next, result) = game.answer(current.game, index)
        val kind = when (result) {
            QuizResult.Correct -> FeedbackKind.Correct
            QuizResult.Wrong -> FeedbackKind.Wrong
            QuizResult.Finished -> FeedbackKind.Finished
            QuizResult.Ignored -> return Outcome.Ignored
        }
        play = current.copy(game = next, feedback = feedback(kind, choice = index, round = round))
        return if (kind == FeedbackKind.Finished) Outcome.Finished else Outcome.Interacted
    }

    fun dropKey(syllable: String, nowMillis: Long): Outcome {
        val game = drop ?: return Outcome.Ignored
        val current = play as MissionPlay.Drop
        val (next, result) = game.type(current.game, syllable, nowMillis)
        play = when (result) {
            is DropInput.Correct ->
                current.copy(game = next, feedback = feedback(FeedbackKind.Correct, word = hit(current, result.word)))
            is DropInput.Finished ->
                current.copy(game = next, feedback = feedback(FeedbackKind.Finished, word = hit(current, result.word)))
            DropInput.Wrong -> current.copy(game = next, feedback = feedback(FeedbackKind.Wrong))
            DropInput.Typing -> current.copy(game = next)
            DropInput.Ignored -> return Outcome.Ignored
        }
        return if (result is DropInput.Finished) Outcome.Finished else Outcome.Interacted
    }

    private fun hit(current: MissionPlay.Drop, text: String): FallingWord? =
        current.game.words.filter { it.text == text }.minByOrNull { it.spawnedAt }

    fun dropDelete(): Outcome {
        val game = drop ?: return Outcome.Ignored
        val current = play as MissionPlay.Drop
        if (current.finished) return Outcome.Ignored
        play = current.copy(game = game.delete(current.game))
        return Outcome.Interacted
    }

    /** 화면 한 장마다 불러요. Drop Words는 바닥에 닿은 단어를 지우고 새 단어를 내려요. */
    fun frame(nowMillis: Long) {
        val game = drop ?: return
        val current = play as MissionPlay.Drop
        if (current.finished) return
        val next = game.tick(current.game, nowMillis)
        if (next != current.game) play = current.copy(game = next)
    }

    fun shake(): Outcome {
        val current = play as? MissionPlay.Shake ?: return Outcome.Ignored
        if (current.finished) return Outcome.Ignored
        val next = ShakeShake.shake(current.game)
        play = current.copy(
            game = next,
            feedback = feedback(if (next.finished) FeedbackKind.Finished else FeedbackKind.Correct),
        )
        return if (next.finished) Outcome.Finished else Outcome.Interacted
    }
}

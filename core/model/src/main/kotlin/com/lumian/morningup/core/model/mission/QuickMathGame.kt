package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.random.Random

/**
 * Quick Math 한 판의 상태.
 * [keypad]는 숫자 키 열 칸에 놓인 숫자 순서예요(화면 왼쪽 위부터, 마지막 숫자는 맨 아래 가운데).
 */
data class QuickMathState(
    val level: Int,
    val target: Int,
    /** 지금까지 맞힌 문제 수. 틀려도 줄지 않아요(결정 92). */
    val correct: Int,
    val problem: MathProblem,
    val input: String,
    val keypad: List<Int>,
    val finished: Boolean = false,
)

enum class SubmitResult { Correct, Wrong, Finished, Empty }

/** 키패드 칸. */
sealed interface MathKey {
    data class Digit(val value: Int) : MathKey

    /** 소수점(결정 90, 91). 모든 단계에서 늘 보여요. */
    data object Dot : MathKey

    data object Delete : MathKey

    /** 입력 칸 안 엔터 버튼. 키패드에는 없어요(결정 90). */
    data object Confirm : MathKey
}

/** 3x4 키패드 배치. 숫자 아홉 칸, 그 아래 줄은 '.'(왼쪽, 고정), 숫자 하나, 지우기(오른쪽, 고정)(결정 90). */
object QuickMathKeypad {
    private const val TOP_DIGITS = 9

    fun keys(keypad: List<Int>): List<MathKey> =
        keypad.take(TOP_DIGITS).map { MathKey.Digit(it) } +
            listOf(MathKey.Dot, MathKey.Digit(keypad[TOP_DIGITS]), MathKey.Delete)
}

/**
 * Quick Math 규칙(design-system M3, dev-plan 4단계).
 * - [target]개 맞히면 끝나요. 틀려도 맞힌 수는 그대로이고 새 문제로 바뀌어요(결정 92). 키패드는 그대로예요.
 * - 답은 소수로 쳐도 돼요('12.0'은 12와 같아요).
 * - 맞히면 새 문제가 나오고 키패드 숫자를 다시 섞어요. 숫자를 치는 도중에는 섞지 않아요.
 */
class QuickMathGame(level: Int, target: Int, private val random: Random) {
    private val range = MissionSetting.rangeOf(MissionType.QuickMath)
    private val level = level.coerceIn(range.levels)
    private val target = target.coerceIn(range.counts)

    fun start(): QuickMathState = QuickMathState(
        level = level,
        target = target,
        correct = 0,
        problem = QuickMath.problem(level, random),
        input = "",
        keypad = shuffled(null),
    )

    fun type(state: QuickMathState, digit: Int): QuickMathState {
        if (state.finished || digit !in DIGITS) return state
        val input = when {
            state.input == "0" -> digit.toString()
            state.input.length >= MAX_INPUT -> state.input
            else -> state.input + digit
        }
        return state.copy(input = input)
    }

    /** '.'은 한 번만, 맨 앞이면 '0.'으로 넣어요. */
    fun dot(state: QuickMathState): QuickMathState = when {
        state.finished || DOT in state.input || state.input.length >= MAX_INPUT - 1 -> state
        state.input.isEmpty() -> state.copy(input = "0$DOT")
        else -> state.copy(input = state.input + DOT)
    }

    fun delete(state: QuickMathState): QuickMathState =
        if (state.finished) state else state.copy(input = state.input.dropLast(1))

    fun submit(state: QuickMathState): Pair<QuickMathState, SubmitResult> {
        val typed = state.input.toBigDecimalOrNull()
        if (state.finished || typed == null) return state to SubmitResult.Empty
        if (typed.compareTo(state.problem.answer.toBigDecimal()) != 0) {
            val next = state.copy(problem = nextProblem(state.problem), input = "")
            return next to SubmitResult.Wrong
        }
        val correct = state.correct + 1
        if (correct >= state.target) {
            return state.copy(correct = correct, input = "", finished = true) to SubmitResult.Finished
        }
        val next = state.copy(
            correct = correct,
            problem = nextProblem(state.problem),
            input = "",
            keypad = shuffled(state.keypad),
        )
        return next to SubmitResult.Correct
    }

    private fun nextProblem(previous: MathProblem): MathProblem {
        var next = QuickMath.problem(level, random)
        while (next == previous || next.text == previous.text) next = QuickMath.problem(level, random)
        return next
    }

    /** 숫자 열 개를 섞어요. [previous]와 같은 배열은 다시 섞어요(정답 뒤에 늘 바뀐 게 보이게). */
    private fun shuffled(previous: List<Int>?): List<Int> {
        var order = DIGITS.toList().shuffled(random)
        while (order == previous) order = DIGITS.toList().shuffled(random)
        return order
    }

    private companion object {
        val DIGITS = 0..9
        const val MAX_INPUT = 7
        const val DOT = '.'
    }
}

package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Quick Math 진행: 연속 정답 N개, 틀리면 새 문제로 처음부터, 정답마다 키패드 다시 섞기. */
class QuickMathGameTest {
    private val game = QuickMathGame(level = 1, target = 3, random = Random(11))

    private fun QuickMathState.typeAnswer(answer: Int = problem.answer): QuickMathState =
        answer.toString().fold(this) { s, ch -> game.type(s, ch.digitToInt()) }

    @Test
    fun start_hasEmptyInput_zeroStreak_andShuffledKeypadOfAllDigits() {
        val s = game.start()
        assertThat(s.correct).isEqualTo(0)
        assertThat(s.target).isEqualTo(3)
        assertThat(s.input).isEmpty()
        assertThat(s.keypad.sorted()).isEqualTo((0..9).toList())
        assertThat(s.finished).isFalse()
    }

    @Test
    fun type_appendsDigits_upToLimit_andDeleteRemovesLast() {
        var s = game.start()
        repeat(9) { s = game.type(s, 4) }
        assertThat(s.input).isEqualTo("4444444")
        s = game.delete(s)
        assertThat(s.input).isEqualTo("444444")
        repeat(7) { s = game.delete(s) }
        assertThat(s.input).isEmpty()
    }

    @Test
    fun dot_onlyOnce_leadingDotBecomesZeroDot_andDecimalAnswerMatches() {
        var s = game.dot(game.start())
        assertThat(s.input).isEqualTo("0.")
        s = game.dot(s)
        assertThat(s.input).isEqualTo("0.")
        val start = game.start()
        val typed = start.typeAnswer().let { game.dot(it) }.let { game.type(it, 0) }
        assertThat(typed.input).isEqualTo("${start.problem.answer}.0")
        assertThat(game.submit(typed).second).isEqualTo(SubmitResult.Correct)
        // '.'만 있으면 빈 칸처럼 아무 일 없어요.
        assertThat(game.submit(start.copy(input = ".")).second).isEqualTo(SubmitResult.Empty)
    }

    @Test
    fun leadingZero_isReplaced() {
        var s = game.type(game.start(), 0)
        s = game.type(s, 7)
        assertThat(s.input).isEqualTo("7")
    }

    @Test
    fun submitEmpty_isIgnored() {
        val s = game.start()
        val (next, result) = game.submit(s)
        assertThat(result).isEqualTo(SubmitResult.Empty)
        assertThat(next).isEqualTo(s)
    }

    @Test
    fun correct_countsUp_newProblem_andReshufflesKeypad() {
        val s = game.start()
        val (next, result) = game.submit(s.typeAnswer())
        assertThat(result).isEqualTo(SubmitResult.Correct)
        assertThat(next.correct).isEqualTo(1)
        assertThat(next.input).isEmpty()
        assertThat(next.problem).isNotEqualTo(s.problem)
        assertThat(next.keypad).isNotEqualTo(s.keypad)
        assertThat(next.keypad.sorted()).isEqualTo((0..9).toList())
    }

    @Test
    fun wrong_keepsCorrectCount_newProblem_keypadStays() {
        val first = game.submit(game.start().typeAnswer()).first
        val (next, result) = game.submit(first.typeAnswer(first.problem.answer + 1))
        assertThat(result).isEqualTo(SubmitResult.Wrong)
        // 결정 92: 틀려도 처음부터 하지 않아요.
        assertThat(next.correct).isEqualTo(1)
        assertThat(next.input).isEmpty()
        assertThat(next.problem).isNotEqualTo(first.problem)
        assertThat(next.keypad).isEqualTo(first.keypad)
    }

    @Test
    fun targetCorrectInARow_finishes() {
        var s = game.start()
        var result: SubmitResult? = null
        repeat(3) {
            val step = game.submit(s.typeAnswer())
            s = step.first
            result = step.second
        }
        assertThat(result).isEqualTo(SubmitResult.Finished)
        assertThat(s.finished).isTrue()
        assertThat(s.correct).isEqualTo(3)
        // 끝난 뒤 입력은 무시해요.
        assertThat(game.type(s, 3)).isEqualTo(s)
        assertThat(game.submit(s).second).isEqualTo(SubmitResult.Empty)
    }

    @Test
    fun targetIsClampedToRange() {
        assertThat(QuickMathGame(1, 0, Random(1)).start().target).isEqualTo(1)
        assertThat(QuickMathGame(1, 50, Random(1)).start().target).isEqualTo(10)
    }

    @Test
    fun keypadLayout_dotBottomLeft_deleteBottomRight() {
        val s = game.start()
        val keys = QuickMathKeypad.keys(s.keypad)
        assertThat(keys).hasSize(12)
        // 결정 90: 마지막 줄은 '.', 숫자, 지우기. 확인 키는 입력 칸 안 엔터로 옮겼어요.
        assertThat(keys[9]).isEqualTo(MathKey.Dot)
        assertThat(keys[11]).isEqualTo(MathKey.Delete)
        assertThat(keys).doesNotContain(MathKey.Confirm)
        assertThat(keys.filterIsInstance<MathKey.Digit>().map { it.value }).isEqualTo(s.keypad)
    }

    @Test
    fun level1_tenProblems_doNotRepeatWithinOneGame() {
        // 1단계 덧셈은 56문제뿐이라 전에는 10문제 한 판에서 자주 겹쳤어요(11-19 지시 7).
        repeat(200) { seed ->
            val g = QuickMathGame(level = 1, target = 10, random = Random(seed))
            var s = g.start()
            val asked = mutableListOf(s.problem.text)
            while (!s.finished) {
                s = s.problem.answer.toString().fold(s) { acc, ch -> g.type(acc, ch.digitToInt()) }
                s = g.submit(s).first
                if (!s.finished) asked += s.problem.text
            }
            assertThat(asked.toSet()).hasSize(asked.size)
        }
    }
}

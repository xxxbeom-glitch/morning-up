package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Quick Math 문제 만들기 규칙(mission-levels.md Quick Math 표와 '만드는 규칙'). */
class QuickMathTest {
    private fun problems(level: Int, n: Int = 400, seed: Int = 7): List<MathProblem> {
        val random = Random(seed)
        return List(n) { QuickMath.problem(level, random) }
    }

    private fun MathProblem.numbers() = listOfNotNull(a, b, c)

    @Test
    fun everyLevel_answerIsPositive_andNoEasyOrRepeatedNumbers() {
        for (level in 1..7) {
            problems(level).forEach { p ->
                assertThat(p.answer).isGreaterThan(0)
                p.numbers().forEach { n ->
                    assertThat(n).isNotEqualTo(0)
                    assertThat(n).isNotEqualTo(1)
                    assertThat(n % 10).isNotEqualTo(0)
                }
                assertThat(p.numbers().toSet().size).isEqualTo(p.numbers().size)
            }
        }
    }

    @Test
    fun level1_singleDigitAddition() {
        problems(1).forEach { p ->
            assertThat(p.op1).isEqualTo(MathOp.Plus)
            assertThat(p.op2).isNull()
            assertThat(p.a).isIn(2..9)
            assertThat(p.b).isIn(2..9)
            assertThat(p.answer).isAtMost(99)
        }
    }

    @Test
    fun level2_twoDigitAddOrSubtract_withCarryOrBorrow() {
        val list = problems(2)
        list.forEach { p ->
            assertThat(p.a).isIn(11..99)
            assertThat(p.b).isIn(11..99)
            assertThat(p.op2).isNull()
            when (p.op1) {
                MathOp.Plus -> assertThat(p.a % 10 + p.b % 10).isAtLeast(10)
                MathOp.Minus -> {
                    assertThat(p.a).isGreaterThan(p.b)
                    assertThat(p.a % 10).isLessThan(p.b % 10)
                }
                MathOp.Times -> error("2단계에 곱셈은 없어요")
            }
            assertThat(p.answer).isAtMost(999)
        }
        assertThat(list.map { it.op1 }.toSet()).containsExactly(MathOp.Plus, MathOp.Minus)
    }

    @Test
    fun level3_twoDigitTimesOneDigit_threeDigitAnswer() {
        problems(3).forEach { p ->
            assertThat(p.op1).isEqualTo(MathOp.Times)
            assertThat(p.a).isIn(11..99)
            assertThat(p.b).isIn(2..9)
            assertThat(p.answer).isIn(100..999)
        }
    }

    @Test
    fun level4_multiplyThenAdd_threeDigitAnswer() {
        problems(4).forEach { p ->
            assertThat(p.op1).isEqualTo(MathOp.Times)
            assertThat(p.op2).isEqualTo(MathOp.Plus)
            assertThat(p.a).isIn(11..99)
            assertThat(p.b).isIn(2..9)
            assertThat(p.c).isIn(11..99)
            assertThat(p.answer).isIn(100..999)
        }
    }

    @Test
    fun level5_twoDigitTimesTwoDigit() {
        problems(5).forEach { p ->
            assertThat(p.op1).isEqualTo(MathOp.Times)
            assertThat(p.op2).isNull()
            assertThat(p.a).isIn(11..99)
            assertThat(p.b).isIn(11..99)
            assertThat(p.answer).isIn(100..9999)
        }
    }

    @Test
    fun level6_twoByTwoPlusThreeDigit_fourDigitAnswer() {
        problems(6).forEach { p ->
            assertThat(p.a).isIn(11..99)
            assertThat(p.b).isIn(11..99)
            assertThat(p.c).isIn(100..999)
            assertThat(p.answer).isIn(1000..9999)
        }
    }

    @Test
    fun level7_threeByTwoPlusFourDigit_fourDigitAnswer() {
        problems(7).forEach { p ->
            assertThat(p.a).isIn(100..999)
            assertThat(p.b).isIn(11..99)
            assertThat(p.c).isIn(1000..9999)
            assertThat(p.answer).isIn(1000..9999)
        }
    }

    @Test
    fun text_matchesFigmaShape() {
        assertThat(MathProblem(24, MathOp.Times, 7, MathOp.Plus, 58).text).isEqualTo("(24 × 7) + 58")
        assertThat(MathProblem(24, MathOp.Times, 7, MathOp.Plus, 58).answer).isEqualTo(226)
        assertThat(MathProblem(47, MathOp.Minus, 38).text).isEqualTo("47 − 38")
        assertThat(MathProblem(47, MathOp.Minus, 38).answer).isEqualTo(9)
        assertThat(MathProblem(7, MathOp.Plus, 5).text).isEqualTo("7 + 5")
    }

    @Test
    fun sameSeed_sameProblems() {
        assertThat(problems(4, n = 20, seed = 3)).isEqualTo(problems(4, n = 20, seed = 3))
    }

    @Test
    fun outOfRangeLevel_isClamped() {
        assertThat(QuickMath.problem(0, Random(1)).op1).isEqualTo(MathOp.Plus)
        assertThat(QuickMath.problem(9, Random(1)).c).isIn(1000..9999)
    }
}

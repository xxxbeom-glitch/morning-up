package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.random.Random

/** 연산 기호. 화면에는 [symbol]을 그대로 써요(빼기는 하이픈이 아니라 수학 빼기 기호). */
enum class MathOp(val symbol: String) {
    Plus("+"),
    Minus("−"),
    Times("×"),
    ;

    fun apply(x: Int, y: Int): Int = when (this) {
        Plus -> x + y
        Minus -> x - y
        Times -> x * y
    }
}

/** 문제 하나. 두 과정이면 `(a op1 b) op2 c`예요. */
data class MathProblem(val a: Int, val op1: MathOp, val b: Int, val op2: MathOp? = null, val c: Int? = null) {
    val answer: Int = op1.apply(a, b).let { first -> if (op2 != null && c != null) op2.apply(first, c) else first }

    /** '7 + 5', '(24 × 7) + 58'. */
    val text: String
        get() = if (op2 != null && c != null) {
            "($a ${op1.symbol} $b) ${op2.symbol} $c"
        } else {
            "$a ${op1.symbol} $b"
        }
}

/**
 * Quick Math 문제 만들기(mission-levels.md Quick Math 표).
 * 규칙: 답은 늘 0보다 커요. 0, 1, 10의 배수처럼 너무 쉬운 수는 빼요. 한 문제 안에 같은 수가 두 번 나오지 않아요.
 * 같은 난수(seed)면 늘 같은 문제가 나와요.
 */
object QuickMath {
    private const val MAX_TRIES = 10_000
    private const val ONE_DIGIT_MIN = 2
    private const val ONE_DIGIT_MAX = 9
    private const val TWO_DIGIT_MIN = 11
    private const val TWO_DIGIT_MAX = 99
    private const val THREE_DIGIT_MIN = 100
    private const val THREE_DIGIT_MAX = 999
    private const val FOUR_DIGIT_MIN = 1000
    private const val FOUR_DIGIT_MAX = 9999
    private const val TEN = 10

    private val ONE = ONE_DIGIT_MIN..ONE_DIGIT_MAX
    private val TWO = TWO_DIGIT_MIN..TWO_DIGIT_MAX
    private val THREE = THREE_DIGIT_MIN..THREE_DIGIT_MAX
    private val FOUR = FOUR_DIGIT_MIN..FOUR_DIGIT_MAX

    fun problem(level: Int, random: Random): MathProblem {
        val range = MissionSetting.rangeOf(MissionType.QuickMath).levels
        val maker = makers[level.coerceIn(range) - 1]
        repeat(MAX_TRIES) {
            maker(random)?.let { if (it.isFair()) return it }
        }
        error("문제를 만들지 못했어요: $level")
    }

    private val makers: List<(Random) -> MathProblem?> = listOf(
        // 1: 한 자리 덧셈
        { r -> MathProblem(r.pick(ONE), MathOp.Plus, r.pick(ONE)) },
        // 2: 두 자리 덧셈(받아올림) 또는 뺄셈(받아내림)
        { r ->
            val x = r.pick(TWO)
            val y = r.pick(TWO)
            if (r.nextBoolean()) {
                MathProblem(x, MathOp.Plus, y).takeIf { x % TEN + y % TEN >= TEN }
            } else {
                val big = maxOf(x, y)
                val small = minOf(x, y)
                MathProblem(big, MathOp.Minus, small).takeIf { big % TEN < small % TEN }
            }
        },
        // 3: 두 자리 × 한 자리, 답 세 자리
        { r -> MathProblem(r.pick(TWO), MathOp.Times, r.pick(ONE)).takeIf { it.answer in THREE } },
        // 4: (두 자리 × 한 자리) + 두 자리, 답 세 자리
        { r ->
            MathProblem(r.pick(TWO), MathOp.Times, r.pick(ONE), MathOp.Plus, r.pick(TWO)).takeIf { it.answer in THREE }
        },
        // 5: 두 자리 × 두 자리
        { r -> MathProblem(r.pick(TWO), MathOp.Times, r.pick(TWO)) },
        // 6: (두 자리 × 두 자리) + 세 자리, 답 네 자리
        { r ->
            MathProblem(r.pick(TWO), MathOp.Times, r.pick(TWO), MathOp.Plus, r.pick(THREE)).takeIf { it.answer in FOUR }
        },
        // 7: (세 자리 × 두 자리) + 네 자리, 답 네 자리. 곱이 너무 크지 않게 두 자리 수를 먼저 골라요.
        { r ->
            val b = r.pick(TWO_DIGIT_MIN..(FOUR_DIGIT_MAX - FOUR_DIGIT_MIN) / THREE_DIGIT_MIN)
            val a = r.pick(THREE_DIGIT_MIN..(FOUR_DIGIT_MAX - FOUR_DIGIT_MIN) / b)
            val room = FOUR_DIGIT_MAX - a * b
            if (room <
                FOUR_DIGIT_MIN
            ) {
                null
            } else {
                MathProblem(a, MathOp.Times, b, MathOp.Plus, r.pick(FOUR_DIGIT_MIN..room))
            }
        },
    )

    /** 10의 배수가 아닌 수를 골라요. */
    private fun Random.pick(range: IntRange): Int {
        while (true) {
            val n = nextInt(range.first, range.last + 1)
            if (n % TEN != 0) return n
        }
    }

    private fun MathProblem.isFair(): Boolean {
        val numbers = listOfNotNull(a, b, c)
        return answer > 0 && numbers.none { it <= 1 || it % TEN == 0 } && numbers.toSet().size == numbers.size
    }
}

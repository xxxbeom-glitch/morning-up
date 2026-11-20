package com.lumian.morningup.core.model.mission

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Drop Words 음절 키패드 칸 수와 정답 음절을 놓는 규칙(11-20 사용자 지시, mission-levels.md Drop Words 키패드 표).
 * 단계가 오를수록 칸이 늘고 한 단어의 음절끼리 멀리 흩어져요. 오른쪽 아래 칸은 늘 지우기라 음절 칸은 [size]개예요.
 * 거리는 칸 기준이에요. 가로, 세로 칸 수 중 큰 쪽(대각선도 1칸)으로 재고, [Spread.Adjacent]만 상하좌우 1칸이에요.
 */
data class DropKeypadLayout(
    val columns: Int,
    val rows: Int,
    val spread: Spread,
    /** 가짜 음절 중 받침만 바꾼 음절(예: 산 → 삼, 삭)의 비율. */
    val finalVariantShare: Float,
) {
    val size: Int get() = columns * rows - 1

    /** 한 단어 안에서 바로 이어지는 두 음절 칸이 단계 규칙을 지키는지. */
    fun allows(from: Int, to: Int): Boolean {
        val dx = abs(from % columns - to % columns)
        val dy = abs(from / columns - to / columns)
        val far = max(dx, dy)
        return when (spread) {
            Spread.Adjacent -> dx + dy == 1
            Spread.Near -> far in 1..NEAR
            Spread.Scattered -> far in NEAR..GLANCE
            Spread.OtherRow -> dy >= 1
            Spread.Far -> far >= FAR
            Spread.Farthest -> far >= FARTHEST
        }
    }

    enum class Spread {
        /** 상하좌우 바로 옆 칸. */
        Adjacent,

        /** 2칸 안. */
        Near,

        /** 붙어 있지 않고 3칸 안(한눈에 보이는 범위). */
        Scattered,

        /** 서로 다른 줄. */
        OtherRow,

        /** 3칸 이상. */
        Far,

        /** 4칸 이상. */
        Farthest,
    }

    companion object {
        private const val NEAR = 2
        private const val GLANCE = 3
        private const val FAR = 3
        private const val FARTHEST = 4

        private val LEVELS = listOf(
            DropKeypadLayout(5, 4, Spread.Adjacent, 0f),
            DropKeypadLayout(5, 4, Spread.Near, 0f),
            DropKeypadLayout(6, 4, Spread.Scattered, 0f),
            DropKeypadLayout(6, 5, Spread.OtherRow, 0f),
            DropKeypadLayout(7, 5, Spread.Far, 0f),
            DropKeypadLayout(8, 5, Spread.Far, 0.5f),
            DropKeypadLayout(8, 5, Spread.Farthest, 0.8f),
        )

        fun of(level: Int): DropKeypadLayout = LEVELS[level.coerceIn(1, LEVELS.size) - 1]
    }
}

/**
 * Drop Words 음절 키패드. 화면에 떠 있는 단어의 음절을 [DropKeypadLayout] 규칙대로 놓고,
 * 남는 칸은 그 음절과 비슷한 가짜 음절로 채워요. 칸 순서는 화면 왼쪽 위부터 차례로예요.
 * [words]는 먼저 떨어질 단어부터예요. 음절이 칸보다 많으면 앞쪽 단어 음절부터 넣어요.
 */
object SyllableKeypad {
    /** 가짜 음절을 만들 진짜 음절이 없을 때 쓰는 바탕 음절. */
    private const val FALLBACK = "가나다라마바사아자차카타파하"

    /** 한 단어를 규칙대로 놓을 때 시도하는 칸 수 한도. 넘으면 남은 음절은 빈칸 아무 데나 놓아요. */
    private const val MAX_STEPS = 4_000

    fun build(words: List<String>, layout: DropKeypadLayout, random: Random): List<String> {
        val grid = MutableList<String?>(layout.size) { null }
        place(grid, words, layout, random)
        val real = grid.filterNotNull()
        val fakes = fakes(real, grid.count { it == null }, layout.finalVariantShare, random).iterator()
        return grid.map { it ?: fakes.next() }
    }

    /**
     * 지금 키패드에 빠진 음절만 넣어요. 화면 단어에 쓰는 칸은 자리를 그대로 두고, 안 쓰는 칸 중에서만 골라요
     * (손 위치가 헷갈리지 않게). 빠진 음절도 같은 단어의 다른 음절과 단계 규칙을 지키게 놓아요.
     */
    fun fill(keypad: List<String>, words: List<String>, layout: DropKeypadLayout, random: Random): List<String> {
        val wanted = syllables(words).take(keypad.size).toSet()
        if (wanted.all { it in keypad }) return keypad
        val grid = keypad.map<String, String?> { if (it in wanted) it else null }.toMutableList()
        place(grid, words, layout, random)
        return grid.mapIndexed { i, s -> s ?: keypad[i] }
    }

    private fun syllables(words: List<String>): List<String> =
        words.flatMap { w -> w.map(Char::toString) }.distinct()

    /** [words] 음절을 빈칸(null)에 놓아요. 이미 있는 음절은 그 자리를 써요. */
    private fun place(grid: MutableList<String?>, words: List<String>, layout: DropKeypadLayout, random: Random) {
        val budget = syllables(words).take(grid.size).toSet()
        for (word in words) {
            val letters = word.map(Char::toString).filter { it in budget }
            if (letters.all { it in grid }) continue
            val steps = intArrayOf(0)
            // 규칙대로 놓을 수 없으면(다른 단어 음절이 칸을 차지함 등) 남은 음절은 빈칸 아무 데나.
            if (!placeWord(grid, letters, 0, null, layout, random, steps)) placeAnywhere(grid, letters, random)
        }
    }

    private fun placeAnywhere(grid: MutableList<String?>, letters: List<String>, random: Random) {
        letters.filter { it !in grid }.forEach { s ->
            val free = grid.indices.filter { grid[it] == null }
            if (free.isNotEmpty()) grid[free.random(random)] = s
        }
    }

    private fun placeWord(
        grid: MutableList<String?>,
        letters: List<String>,
        index: Int,
        previous: Int?,
        layout: DropKeypadLayout,
        random: Random,
        steps: IntArray,
    ): Boolean {
        if (index == letters.size) return true
        val letter = letters[index]
        val existing = grid.indexOf(letter)
        // 이미 놓인 음절(같은 단어에 두 번 나오거나 다른 단어와 함께 씀)은 옮기지 않아요.
        if (existing >= 0) return placeWord(grid, letters, index + 1, existing, layout, random, steps)
        val candidates = grid.indices
            .filter { grid[it] == null && (previous == null || layout.allows(previous, it)) }
            .shuffled(random)
        return candidates.any { cell ->
            steps[0]++
            if (steps[0] > MAX_STEPS) {
                false
            } else {
                grid[cell] = letter
                val ok = placeWord(grid, letters, index + 1, cell, layout, random, steps)
                if (!ok) grid[cell] = null
                ok
            }
        }
    }

    private fun fakes(real: List<String>, count: Int, finalShare: Float, random: Random): List<String> {
        val bases = real.mapNotNull { it.firstOrNull() }.ifEmpty { FALLBACK.toList() }
        val taken = real.mapNotNull { it.firstOrNull() }.toMutableSet()
        val result = mutableListOf<String>()
        // 6, 7단계: 받침만 다른 음절을 먼저 섞어요(11-20 사용자 지시).
        val finalCount = (count * finalShare).toInt()
        var attempt = 0
        while (result.size < finalCount && attempt < finalCount * MAX_TRIES_PER_KEY) {
            val base = bases[attempt % bases.size]
            attempt++
            val fake = Hangul.finalVariant(base, taken, random) ?: continue
            taken += fake
            result += fake.toString()
        }
        attempt = 0
        while (result.size < count && attempt < count * MAX_TRIES_PER_KEY) {
            val base = bases[attempt % bases.size]
            attempt++
            val fake = Hangul.similar(base, taken, random) ?: continue
            taken += fake
            result += fake.toString()
        }
        // 그래도 모자라면 바탕 음절에서 채워요(칸이 비지 않게).
        FALLBACK.forEach { c ->
            if (result.size < count && c !in taken) {
                taken += c
                result += c.toString()
            }
        }
        return result.shuffled(random)
    }

    private const val MAX_TRIES_PER_KEY = 20
}

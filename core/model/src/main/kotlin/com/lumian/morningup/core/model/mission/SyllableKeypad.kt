package com.lumian.morningup.core.model.mission

import kotlin.random.Random

/**
 * Drop Words 음절 키패드 19칸(5x4에서 오른쪽 아래 지우기를 뺀 칸).
 * 화면에 떠 있는 단어의 음절([needed], 먼저 떨어질 단어부터)을 넣고, 남는 칸은 그 음절과 비슷한 가짜 음절로 채운 뒤 섞어요.
 * 음절이 19개를 넘으면 앞쪽(먼저 떨어질 단어) 음절부터 넣어요.
 */
object SyllableKeypad {
    const val SIZE = 19

    /** 가짜 음절을 만들 진짜 음절이 없을 때 쓰는 바탕 음절. */
    private const val FALLBACK = "가나다라마바사아자차카타파하"

    fun build(needed: List<String>, random: Random): List<String> {
        val real = needed.distinct().take(SIZE)
        val fakes = fakes(real, SIZE - real.size, random)
        return (real + fakes).shuffled(random)
    }

    /**
     * 지금 키패드에 빠진 음절만 넣어요. 다른 칸은 자리를 그대로 두고, 화면 단어에 안 쓰는 칸만 바꿔요(손 위치가 헷갈리지 않게).
     */
    fun fill(keypad: List<String>, needed: List<String>, random: Random): List<String> {
        val wanted = needed.distinct().take(SIZE)
        val missing = wanted.filter { it !in keypad }
        if (missing.isEmpty()) return keypad
        val free = keypad.indices.filter { keypad[it] !in wanted }.shuffled(random)
        val result = keypad.toMutableList()
        missing.zip(free).forEach { (syllable, index) -> result[index] = syllable }
        return result
    }

    private fun fakes(real: List<String>, count: Int, random: Random): List<String> {
        val bases = real.mapNotNull { it.firstOrNull() }.ifEmpty { FALLBACK.toList() }
        val taken = real.mapNotNull { it.firstOrNull() }.toMutableSet()
        val result = mutableListOf<String>()
        var attempt = 0
        while (result.size < count && attempt < count * MAX_TRIES_PER_KEY) {
            val base = bases[attempt % bases.size]
            attempt++
            val fake = Hangul.similar(base, taken, random) ?: continue
            taken += fake
            result += fake.toString()
        }
        return result
    }

    private const val MAX_TRIES_PER_KEY = 20
}

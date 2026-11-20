package com.lumian.morningup.core.model.mission

import kotlin.random.Random

/**
 * 한글 음절 나누기와 합치기, Drop Words 가짜 음절 만들기(F16).
 * 가짜 음절은 진짜 음절의 첫소리(자음)나 가운뎃소리(모음) 하나만 바꿔 만들고, 받침은 그대로 둬요(예: 기 → 키, 치, 게).
 * 비슷하게 생긴 자음, 모음 무리 안에서 먼저 고르고, 다 쓰면 다른 자음, 모음으로 넓혀요.
 */
object Hangul {
    private const val FIRST = 0xAC00
    private const val LAST = 0xD7A3
    private const val VOWELS = 21
    private const val FINALS = 28
    private const val INITIALS = 19

    /** 음절 하나의 첫소리, 가운뎃소리, 받침 번호(유니코드 순서). */
    data class Jamo(val initial: Int, val vowel: Int, val final: Int)

    /** 비슷하게 생기거나 소리 나는 첫소리 무리(ㄱㄲㅋ, ㄷㄸㅌ, ㅂㅃㅍ, ㅈㅉㅊ, ㅅㅆ, ㄴㄹ, ㅁㅂ, ㅇㅎ). */
    private val initialGroups = listOf(
        listOf(0, 1, 15),
        listOf(3, 4, 16),
        listOf(7, 8, 17),
        listOf(12, 13, 14),
        listOf(9, 10),
        listOf(2, 5),
        listOf(6, 7),
        listOf(11, 18),
    )

    /** 비슷한 모음 무리(ㅏㅑㅓㅕ, ㅐㅔㅒㅖ, ㅗㅛㅜㅠ, ㅡㅣㅢ, ㅘㅝ, ㅚㅟ, ㅙㅞ). */
    private val vowelGroups = listOf(
        listOf(0, 2, 4, 6),
        listOf(1, 5, 3, 7),
        listOf(8, 12, 13, 17),
        listOf(18, 20, 19),
        listOf(9, 14),
        listOf(11, 16),
        listOf(10, 15),
    )

    fun isSyllable(c: Char): Boolean = c.code in FIRST..LAST

    /** 빈칸이나 영어, 낱자 없이 완성된 한글 음절로만 된 낱말인지. */
    fun isHangulWord(text: String): Boolean = text.isNotEmpty() && text.all(::isSyllable)

    fun split(c: Char): Jamo? {
        if (!isSyllable(c)) return null
        val index = c.code - FIRST
        return Jamo(index / (VOWELS * FINALS), index % (VOWELS * FINALS) / FINALS, index % FINALS)
    }

    fun join(jamo: Jamo): Char = (FIRST + (jamo.initial * VOWELS + jamo.vowel) * FINALS + jamo.final).toChar()

    /** [base]와 자음이나 모음 하나만 다른 음절. [exclude]에 있는 음절은 고르지 않아요. 만들 수 없으면 null. */
    fun similar(base: Char, exclude: Set<Char>, random: Random): Char? {
        val jamo = split(base) ?: return null
        val near = (
            neighbors(jamo.initial, initialGroups).map { jamo.copy(initial = it) } +
                neighbors(jamo.vowel, vowelGroups).map { jamo.copy(vowel = it) }
            ).shuffled(random)
        val far = (
            (0 until INITIALS).filter { it != jamo.initial }.map { jamo.copy(initial = it) } +
                (0 until VOWELS).filter { it != jamo.vowel }.map { jamo.copy(vowel = it) }
            ).shuffled(random)
        return (near + far).asSequence().map(::join).firstOrNull { it != base && it !in exclude }
    }

    /**
     * [base]와 받침만 다른 음절(예: 산 → 삼, 상, 사). 6, 7단계 Drop Words 키패드에 섞어요(11-20 사용자 지시).
     * 자주 쓰는 받침부터 골라요. [exclude]에 있는 음절은 고르지 않아요. 만들 수 없으면 null.
     */
    fun finalVariant(base: Char, exclude: Set<Char>, random: Random): Char? {
        val jamo = split(base) ?: return null
        val common = COMMON_FINALS.filter { it != jamo.final }.shuffled(random)
        val rest = (0 until FINALS).filter { it != jamo.final && it !in COMMON_FINALS }.shuffled(random)
        return (common + rest).asSequence().map { join(jamo.copy(final = it)) }.firstOrNull { it !in exclude }
    }

    /** 받침 없음, ㄱ, ㄴ, ㄹ, ㅁ, ㅂ, ㅅ, ㅇ (유니코드 받침 번호). */
    private val COMMON_FINALS = listOf(0, 1, 4, 8, 16, 17, 19, 21)

    private fun neighbors(value: Int, groups: List<List<Int>>): List<Int> =
        groups.filter { value in it }.flatten().filter { it != value }.distinct()
}

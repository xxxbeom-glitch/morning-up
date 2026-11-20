package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Drop Words 음절 키패드: 단계별 칸 수와 정답 음절 놓는 규칙(11-20 사용자 지시). */
class SyllableKeypadTest {
    private val random = Random(21)
    private val level1 = DropKeypadLayout.of(1)

    @Test
    fun layoutTable_matchesLevels() {
        val table = (1..7).map { DropKeypadLayout.of(it).let { l -> l.columns to l.rows } }
        assertThat(table).containsExactly(5 to 4, 5 to 4, 6 to 4, 6 to 5, 7 to 5, 8 to 5, 8 to 5).inOrder()
        assertThat(DropKeypadLayout.of(1).size).isEqualTo(19)
        assertThat(DropKeypadLayout.of(7).size).isEqualTo(39)
    }

    @Test
    fun build_everyLevel_fillsAllCellsWithUniqueHangul_containingWord() {
        val samples = listOf("사과", "운동화", "닭볶음", "일석이조", "소멸시효", "읊조리다", "탄산수소나트륨")
        (1..7).forEach { level ->
            val layout = DropKeypadLayout.of(level)
            val word = samples[level - 1]
            repeat(50) {
                val keys = SyllableKeypad.build(listOf(word), layout, random)
                assertThat(keys).hasSize(layout.size)
                assertThat(keys.toSet()).hasSize(layout.size)
                assertThat(keys).containsAtLeastElementsIn(word.map(Char::toString))
                keys.forEach { assertThat(Hangul.isHangulWord(it)).isTrue() }
            }
        }
    }

    /** 판마다 무작위로 놓아도 한 단어의 이어지는 음절은 늘 단계 규칙을 지켜요. */
    @Test
    fun build_everyLevel_keepsSpreadRule() {
        val samples = listOf("나무", "자동차", "깻잎전", "우공이산", "고혈압", "넓적다리", "탄산수소나트륨")
        (1..7).forEach { level ->
            val layout = DropKeypadLayout.of(level)
            val word = samples[level - 1]
            repeat(200) {
                val keys = SyllableKeypad.build(listOf(word), layout, Random(it * 31 + level))
                word.zipWithNext().forEach { (a, b) ->
                    val from = keys.indexOf(a.toString())
                    val to = keys.indexOf(b.toString())
                    assertThat(layout.allows(from, to)).isTrue()
                }
            }
        }
    }

    @Test
    fun spreadRules_byCells() {
        // 5열 기준 0은 (0,0), 1은 (1,0), 5는 (0,1), 6은 (1,1).
        assertThat(level1.allows(0, 1)).isTrue()
        assertThat(level1.allows(0, 5)).isTrue()
        assertThat(level1.allows(0, 6)).isFalse()
        assertThat(DropKeypadLayout.of(2).allows(0, 2)).isTrue()
        assertThat(DropKeypadLayout.of(2).allows(0, 3)).isFalse()
        // 6열: 붙어 있으면 안 되고 3칸 안.
        assertThat(DropKeypadLayout.of(3).allows(0, 1)).isFalse()
        assertThat(DropKeypadLayout.of(3).allows(0, 3)).isTrue()
        assertThat(DropKeypadLayout.of(3).allows(0, 4)).isFalse()
        // 6x5: 다른 줄.
        assertThat(DropKeypadLayout.of(4).allows(0, 5)).isFalse()
        assertThat(DropKeypadLayout.of(4).allows(0, 6)).isTrue()
        // 8열: 7단계는 4칸 이상.
        assertThat(DropKeypadLayout.of(7).allows(0, 3)).isFalse()
        assertThat(DropKeypadLayout.of(7).allows(0, 4)).isTrue()
    }

    @Test
    fun highLevels_mixFinalOnlyVariants() {
        fun finalOnlyShare(level: Int): Double {
            val layout = DropKeypadLayout.of(level)
            val word = "고혈압"
            var total = 0
            var finalOnly = 0
            repeat(30) {
                val keys = SyllableKeypad.build(listOf(word), layout, Random(it))
                val reals = word.map { c -> Hangul.split(c)!! }
                (keys - word.map(Char::toString).toSet()).forEach { fake ->
                    val f = Hangul.split(fake[0])!!
                    total++
                    val onlyFinal = reals.any { r ->
                        r.initial == f.initial && r.vowel == f.vowel && r.final != f.final
                    }
                    if (onlyFinal) finalOnly++
                }
            }
            return finalOnly.toDouble() / total
        }
        assertThat(finalOnlyShare(5)).isEqualTo(0.0)
        assertThat(finalOnlyShare(6)).isAtLeast(0.4)
        assertThat(finalOnlyShare(7)).isGreaterThan(finalOnlyShare(6))
    }

    @Test
    fun build_whenMoreSyllablesThanCells_keepsEarliestFirst() {
        val words = listOf("가나다라마바사아자차", "카타파하거너더러머버서")
        val keys = SyllableKeypad.build(words, level1, random)
        val all = words.joinToString("").map(Char::toString)
        assertThat(keys).containsExactlyElementsIn(all.take(level1.size))
    }

    @Test
    fun fill_insertsMissingInPlace_keepingOtherKeysWhereTheyWere() {
        val keys = SyllableKeypad.build(listOf("사과"), level1, random)
        val filled = SyllableKeypad.fill(keys, listOf("사과", "나무"), level1, random)
        assertThat(filled).hasSize(level1.size)
        assertThat(filled).containsAtLeast("사", "과", "나", "무")
        val changed = keys.indices.count { keys[it] != filled[it] }
        assertThat(changed).isAtMost(2)
        assertThat(filled.indexOf("사")).isEqualTo(keys.indexOf("사"))
        assertThat(filled.indexOf("과")).isEqualTo(keys.indexOf("과"))
        // 새로 넣은 단어도 1단계 규칙(바로 옆 칸)을 지켜요.
        assertThat(level1.allows(filled.indexOf("나"), filled.indexOf("무"))).isTrue()
    }

    @Test
    fun fill_whenNothingMissing_returnsSame() {
        val keys = SyllableKeypad.build(listOf("사과"), level1, random)
        assertThat(SyllableKeypad.fill(keys, listOf("과"), level1, random)).isEqualTo(keys)
    }
}

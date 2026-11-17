package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Drop Words 19칸 음절 키패드. */
class SyllableKeypadTest {
    private val random = Random(21)

    @Test
    fun build_has19UniqueKeys_containingAllNeeded() {
        val needed = listOf("아", "침", "햇", "살", "산", "책")
        val keys = SyllableKeypad.build(needed, random)
        assertThat(keys).hasSize(SyllableKeypad.SIZE)
        assertThat(keys.toSet()).hasSize(SyllableKeypad.SIZE)
        assertThat(keys).containsAtLeastElementsIn(needed)
        keys.forEach { assertThat(Hangul.isHangulWord(it)).isTrue() }
    }

    @Test
    fun build_whenMoreThan19Needed_keepsEarliestFirst() {
        val needed = "가나다라마바사아자차카타파하거너더러머버서".map { it.toString() }
        val keys = SyllableKeypad.build(needed, random)
        assertThat(keys).containsExactlyElementsIn(needed.take(SyllableKeypad.SIZE))
    }

    @Test
    fun build_fakesAreSimilarToNeeded() {
        val needed = listOf("사", "과")
        val keys = SyllableKeypad.build(needed, random)
        val fakes = keys - needed.toSet()
        fakes.forEach { fake ->
            val f = Hangul.split(fake[0])!!
            val closeToSome = needed.any { n ->
                val r = Hangul.split(n[0])!!
                r.final == f.final && listOf(r.initial != f.initial, r.vowel != f.vowel).count { it } == 1
            }
            assertThat(closeToSome).isTrue()
        }
    }

    @Test
    fun fill_insertsMissingInPlace_keepingOtherKeysWhereTheyWere() {
        val keys = SyllableKeypad.build(listOf("사", "과"), random)
        val filled = SyllableKeypad.fill(keys, listOf("사", "과", "나", "무"), random)
        assertThat(filled).hasSize(SyllableKeypad.SIZE)
        assertThat(filled).containsAtLeast("사", "과", "나", "무")
        // 바뀐 칸은 빠진 음절 두 개뿐이에요.
        val changed = keys.indices.count { keys[it] != filled[it] }
        assertThat(changed).isAtMost(2)
        assertThat(filled.indexOf("사")).isEqualTo(keys.indexOf("사"))
        assertThat(filled.indexOf("과")).isEqualTo(keys.indexOf("과"))
    }

    @Test
    fun fill_whenNothingMissing_returnsSame() {
        val keys = SyllableKeypad.build(listOf("사", "과"), random)
        assertThat(SyllableKeypad.fill(keys, listOf("과"), random)).isEqualTo(keys)
    }
}

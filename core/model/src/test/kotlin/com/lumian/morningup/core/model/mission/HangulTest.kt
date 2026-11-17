package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** 한글 음절 나누기와 가짜 음절(F16: 같은 자음이나 모음 하나만 바꿈). */
class HangulTest {
    @Test
    fun split_andJoin_roundTrip() {
        val jamo = Hangul.split('닭')!!
        assertThat(Hangul.join(jamo)).isEqualTo('닭')
        assertThat(Hangul.split('a')).isNull()
    }

    @Test
    fun isHangulWord_onlyFullSyllables() {
        assertThat(Hangul.isHangulWord("탄산수소나트륨")).isTrue()
        assertThat(Hangul.isHangulWord("abc")).isFalse()
        assertThat(Hangul.isHangulWord("사과 ")).isFalse()
        assertThat(Hangul.isHangulWord("ㄱ나")).isFalse()
        assertThat(Hangul.isHangulWord("")).isFalse()
    }

    @Test
    fun similar_changesExactlyOneOfInitialOrVowel_keepsFinal() {
        val random = Random(3)
        repeat(200) {
            val base = "키치게퍼닭볶음잎".random(random)
            val fake = Hangul.similar(base, emptySet(), random)!!
            val a = Hangul.split(base)!!
            val b = Hangul.split(fake)!!
            assertThat(fake).isNotEqualTo(base)
            assertThat(b.final).isEqualTo(a.final)
            val changed = listOf(a.initial != b.initial, a.vowel != b.vowel).count { it }
            assertThat(changed).isEqualTo(1)
        }
    }

    @Test
    fun similar_skipsExcluded() {
        val random = Random(5)
        val excluded = mutableSetOf<Char>()
        repeat(30) {
            val fake = Hangul.similar('가', excluded, random)!!
            assertThat(fake).isNotIn(excluded)
            excluded += fake
        }
    }

    @Test
    fun sameSeed_sameFake() {
        assertThat(Hangul.similar('사', emptySet(), Random(9))).isEqualTo(Hangul.similar('사', emptySet(), Random(9)))
    }
}

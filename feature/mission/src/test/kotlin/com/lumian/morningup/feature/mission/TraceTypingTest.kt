package com.lumian.morningup.feature.mission

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** TSK-238: 예문 위에 겹쳐 쓰기 판정. */
class TraceTypingTest {
    private val sentence = "테스트 문장 12345"

    @Test
    fun firstWrong_findsMismatchAndOverflow() {
        assertThat(TraceTyping.firstWrong("테스트", sentence)).isNull()
        assertThat(TraceTyping.firstWrong("테스ㅌ", sentence)).isEqualTo(2)
        assertThat(TraceTyping.firstWrong("$sentence!", sentence)).isEqualTo(sentence.length)
    }

    @Test
    fun firstWrong_skipsComposingSyllable() {
        // '장'을 치는 중 '자'는 조합 중이라 아직 틀림이 아니에요.
        assertThat(TraceTyping.firstWrong("테스트 문자", sentence, TextRange(5, 6))).isNull()
        assertThat(TraceTyping.firstWrong("테스트 문자", sentence)).isEqualTo(5)
    }

    @Test
    fun accepts_hangulCompositionSteps() {
        assertThat(TraceTyping.accepts("테스트 ", "테스트 뭊", sentence)).isTrue()
        assertThat(TraceTyping.accepts("테스트 뭊", "테스트 문자", sentence)).isTrue()
        assertThat(TraceTyping.accepts("테스트 문자", "테스트 문장", sentence)).isTrue()
    }

    @Test
    fun accepts_blocksTypingPastWrong_untilErased() {
        assertThat(TraceTyping.accepts("테스트 문장 1", "테스트 문장 1x", sentence)).isTrue()
        assertThat(TraceTyping.accepts("테스트 문장 1x", "테스트 문장 1x3", sentence)).isFalse()
        assertThat(TraceTyping.accepts("테스트 문장 1x", "테스트 문장 1", sentence)).isTrue()
    }
}

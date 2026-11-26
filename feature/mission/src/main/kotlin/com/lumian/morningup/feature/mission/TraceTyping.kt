package com.lumian.morningup.feature.mission

import androidx.compose.ui.text.TextRange

/**
 * M5 따라 쓰기 판정(TSK-231, TSK-238). 흐린 예문 위 같은 자리에 글자를 겹쳐 써요.
 * 글자마다 예문과 같은 자리 글자를 견줘요. 한글을 조합하는 중인 글자([composition])는 아직 판정하지 않아요.
 */
internal object TraceTyping {
    /** 틀린 첫 글자 자리. 예문보다 길게 쓴 글자도 틀림이에요. 조합 중인 글자는 건너뛰어요. */
    fun firstWrong(typed: String, sentence: String, composition: TextRange? = null): Int? =
        typed.indices.firstOrNull { i ->
            val composing = composition != null && i >= composition.min && i < composition.max
            !composing && (i >= sentence.length || typed[i] != sentence[i])
        }

    /**
     * 새 글을 받을지. 지우는 것은 늘 받아요. 늘어날 때는 마지막 글자 앞에 틀린 글자가 있으면 막아요.
     * 그래서 틀린 글자를 지워야 이어 쓸 수 있어요. 마지막 글자는 조합 중일 수 있어서 판정하지 않아요.
     */
    fun accepts(old: String, new: String, sentence: String): Boolean {
        if (new.length <= old.length) return true
        val wrong = firstWrong(new, sentence) ?: return true
        return wrong >= new.length - 1
    }
}

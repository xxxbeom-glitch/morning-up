package com.lumian.morningup.core.model

/**
 * S6 진동 패턴. [waveform]은 쉬기, 떨기를 번갈아 적은 밀리초이고 처음부터 되풀이해요.
 * [key]는 저장용 이름이라 바꾸지 않아요.
 */
@Suppress("MagicNumber") // 진동 길이 자체가 데이터예요.
enum class VibrationPattern(val key: String, val waveform: LongArray) {
    /** 기본: 0.8초 떨고 0.6초 쉬기. */
    Basic("basic", longArrayOf(0, 800, 600)),

    /** 짧게 여러 번: 0.2초씩 세 번 떨고 0.8초 쉬기. */
    ShortRepeat("short", longArrayOf(0, 200, 150, 200, 150, 200, 800)),

    /** 길게 한 번: 1.6초 떨고 1.2초 쉬기. */
    LongOnce("long", longArrayOf(0, 1600, 1200)),
    ;

    companion object {
        fun of(key: String?): VibrationPattern = entries.firstOrNull { it.key == key } ?: Basic
    }
}

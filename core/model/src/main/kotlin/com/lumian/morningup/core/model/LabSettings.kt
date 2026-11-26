package com.lumian.morningup.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 버튼이 튕기는 빠르기 1~10단계(결정 174, 결정 155의 세 칸을 바꿈). [widthsPerSecond]는 1초에 화면 너비의 몇 배를 가는지예요.
 * 한 단계에 0.1배씩, 1단계 0.3배부터 10단계 1.2배까지예요. 옛 '느리게'(0.4)는 2단계, '보통'(0.7)은 5단계,
 * '빠르게'(1.1)는 9단계와 같은 빠르기라 옛 저장값은 그 단계로 옮겨요. 처음 값은 옛 보통보다 한 단계 빠른 6단계이고
 * Z플립5에서 보고 고쳐요.
 */
@Serializable(with = TrickSpeedSerializer::class)
data class TrickSpeed(val level: Int) {
    init {
        require(level in MIN_LEVEL..MAX_LEVEL) { "speed level $level" }
    }

    val widthsPerSecond: Float get() = BASE_WIDTHS + STEP_WIDTHS * level

    /** 저장 글자(단계 숫자). */
    val key: String get() = level.toString()

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 10
        private const val BASE_WIDTHS = 0.2f
        private const val STEP_WIDTHS = 0.1f
        private const val SLOW_LEVEL = 2
        private const val NORMAL_LEVEL = 5
        private const val DEFAULT_LEVEL = 6
        private const val FAST_LEVEL = 9

        /** 옛 '느리게', '보통', '빠르게'와 같은 빠르기. */
        val Slow = TrickSpeed(SLOW_LEVEL)
        val Normal = TrickSpeed(NORMAL_LEVEL)
        val Fast = TrickSpeed(FAST_LEVEL)

        /** 처음 값 6단계(옛 보통보다 한 단계 빠름, 결정 174). */
        val Default = TrickSpeed(DEFAULT_LEVEL)

        /** 저장 글자 읽기. 단계 숫자, 옛 글자(slow, normal, fast) 모두 읽고, 모르면 처음 값이에요. */
        fun of(key: String?): TrickSpeed = when (key) {
            "slow" -> Slow
            "normal" -> Normal
            "fast" -> Fast
            else -> key?.toIntOrNull()?.coerceIn(MIN_LEVEL, MAX_LEVEL)?.let(::TrickSpeed) ?: Default
        }
    }
}

/** 울림 상태 JSON에는 단계 숫자 글자로 써요. 옛 JSON의 "normal" 같은 글자도 읽어요. */
internal object TrickSpeedSerializer : KSerializer<TrickSpeed> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TrickSpeed", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TrickSpeed) = encoder.encodeString(value.key)

    override fun deserialize(decoder: Decoder): TrickSpeed = TrickSpeed.of(decoder.decodeString())
}

/**
 * 울리기 시작할 때 떼어 둔 버튼 속이기 값. 울리는 중에 설정을 바꿔도 이번 울림은 이 값이에요.
 * 예전에 저장된 `level`(강도)은 읽을 때 버려요(결정 155에서 강도를 없앰).
 */
@Serializable
data class ButtonTrick(val speed: TrickSpeed = TrickSpeed.Default)

/**
 * '확실히 잠깨기' 실험실(결정 146). 모두 처음 꺼짐이에요.
 * - 버튼 속이기: [trickEnabled], [trickSpeed]. 버튼 하나가 울림 화면 안에서 공처럼 튕겨요(결정 155).
 *   화면을 처음 만진 때부터 [TRICK_SECONDS]초 동안만 해요.
 * - 여러 번 묻기는 없앴어요(결정 177). 묻는 횟수는 전체 설정 [WakeCheckSettings.recheckCount]예요.
 * - [escapeHidden]: 미션을 못 할 때 알림(미션 비상구, 결정 151) 숨기기. 처음 꺼짐(= 알림 뜸)이에요.
 * - [debugMinuteMillis]: 시험용 시간 배율(디버그 빌드의 adb 명령으로만 바뀌어요). W3 기다리는 시간과 대답할
 *   시간에만 써요. 출시 빌드에서는 늘 1분 = 60초예요.
 */
data class LabSettings(
    val trickEnabled: Boolean = false,
    val trickSpeed: TrickSpeed = TrickSpeed.Default,
    val escapeHidden: Boolean = false,
    val debugMinuteMillis: Long = MINUTE_MILLIS,
) {
    /** 이번 울림에 걸 버튼 속이기. 꺼져 있으면 null. */
    val trick: ButtonTrick? get() = if (trickEnabled) ButtonTrick(trickSpeed) else null

    companion object {
        const val MINUTE_MILLIS = 60_000L

        /** 버튼 속이기 시간 상한(초). 고정이에요(사용자 확정 11-24). */
        const val TRICK_SECONDS = 20
    }
}

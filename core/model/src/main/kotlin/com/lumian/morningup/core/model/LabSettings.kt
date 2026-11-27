package com.lumian.morningup.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

/**
 * 버튼이 튕기는 빠르기(결정 181, 결정 174의 1~10단계를 바꿈). [millis]는 버튼이 화면 한쪽 끝에서 다른 끝까지 가는
 * 시간(ms)이고 작을수록 빨라요. 처음 값은 옛 6단계와 같은 1250ms, 넣을 수 있는 범위는 [MIN_MILLIS]~[MAX_MILLIS]예요.
 * 옛 단계(1~10, 1초에 화면 너비의 0.2 + 0.1 x 단계 배)와 옛 글자(slow, normal, fast)는 같은 빠르기의 ms로 옮겨요.
 */
@Serializable(with = TrickSpeedSerializer::class)
data class TrickSpeed(val millis: Int) {
    init {
        require(millis in MIN_MILLIS..MAX_MILLIS) { "speed millis $millis" }
    }

    /** 1초에 화면 너비의 몇 배를 가는지. */
    val widthsPerSecond: Float get() = MILLIS_PER_SECOND / millis

    /** 저장 글자(ms 숫자). 옛 단계 숫자(1~10)와 범위가 겹치지 않아요. */
    val key: String get() = millis.toString()

    companion object {
        const val MIN_MILLIS = 50
        const val MAX_MILLIS = 5_000
        const val DEFAULT_MILLIS = 1_250
        private const val MILLIS_PER_SECOND = 1_000f
        private const val OLD_MIN_LEVEL = 1
        private const val OLD_MAX_LEVEL = 10
        private const val OLD_BASE_WIDTHS = 0.2f
        private const val OLD_STEP_WIDTHS = 0.1f

        /** 처음 값 1250ms(옛 6단계, 결정 181). */
        val Default = TrickSpeed(DEFAULT_MILLIS)

        /** 옛 '느리게'(2단계), '보통'(5단계), '빠르게'(9단계)와 같은 빠르기. */
        val Slow = fromOldLevel(2)
        val Normal = fromOldLevel(5)
        val Fast = fromOldLevel(9)

        /** 넣은 ms를 범위 안으로 맞춰요. 범위 밖이면 가까운 끝값이에요. */
        fun ofMillis(millis: Int): TrickSpeed = TrickSpeed(millis.coerceIn(MIN_MILLIS, MAX_MILLIS))

        /** 옛 1~10단계를 같은 빠르기의 ms로(6단계 = 1250ms). */
        fun fromOldLevel(level: Int): TrickSpeed {
            val widths = OLD_BASE_WIDTHS + OLD_STEP_WIDTHS * level.coerceIn(OLD_MIN_LEVEL, OLD_MAX_LEVEL)
            return ofMillis((MILLIS_PER_SECOND / widths).roundToInt())
        }

        /** 저장 글자 읽기. ms 숫자, 옛 단계 숫자(1~10), 옛 글자(slow, normal, fast) 모두 읽고, 모르면 처음 값이에요. */
        fun of(key: String?): TrickSpeed {
            val number = key?.toIntOrNull()
            return when {
                key == "slow" -> Slow
                key == "normal" -> Normal
                key == "fast" -> Fast
                number == null -> Default
                number in OLD_MIN_LEVEL..OLD_MAX_LEVEL -> fromOldLevel(number)
                else -> ofMillis(number)
            }
        }
    }
}

/** 울림 상태 JSON에는 ms 숫자 글자로 써요. 옛 JSON의 단계 숫자와 "normal" 같은 글자도 읽어요. */
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
 * 'Crazy Mode' 실험실(결정 146, 이름은 결정 188). 모두 처음 꺼짐이에요.
 * - 움직이는 버튼(옛 버튼 속이기): [trickEnabled], [trickSpeed]. 버튼 하나가 울림 화면 안에서 공처럼 튕겨요(결정 155).
 *   울리는 내내 튕겨요(결정 190, 옛 20초 제한을 없앰).
 * - 여러 번 묻기는 없앴어요(결정 177). 묻는 횟수는 전체 설정 [WakeCheckSettings.recheckCount]예요.
 * - [escapeHidden]: 미션을 못 할 때 알림(미션 비상구, 결정 151) 숨기기. 처음 꺼짐(= 알림 뜸)이에요.
 *   화면에서는 거꾸로 '미션 포기 사용하기' 토글이라 켜짐이 [escapeHidden] 꺼짐이에요(결정 188).
 * - [debugMinuteMillis]: 시험용 시간 배율(디버그 빌드의 adb 명령으로만 바뀌어요). W3 기다리는 시간과 대답할
 *   시간에만 써요. 출시 빌드에서는 늘 1분 = 60초예요.
 */
data class LabSettings(
    val trickEnabled: Boolean = false,
    val trickSpeed: TrickSpeed = TrickSpeed.Default,
    val escapeHidden: Boolean = false,
    val debugMinuteMillis: Long = MINUTE_MILLIS,
    /**
     * '좀비모드'(옛 '한 번 더 확인', W3 '정말 깼어요?', 결정 184, 188). 유료 후보라 실험실에서 켜요. 처음 꺼짐.
     * 꺼져 있으면 알람마다 저장된 횟수가 있어도 W3를 묻지 않고, 편집 시트에 '횟수' 줄이 안 보여요.
     */
    val recheckEnabled: Boolean = false,
) {
    /** 이번 울림에 걸 버튼 속이기. 꺼져 있으면 null. */
    val trick: ButtonTrick? get() = if (trickEnabled) ButtonTrick(trickSpeed) else null

    companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}

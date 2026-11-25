package com.lumian.morningup.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 버튼이 튕기는 빠르기(11-24 사용자 추가, 결정 155). [widthsPerSecond]는 1초에 화면 너비의 몇 배를 가는지예요.
 * 시작값은 0.4, 0.7, 1.1배이고 Z플립5에서 보고 고쳐요.
 */
@Serializable
@Suppress("MagicNumber")
enum class TrickSpeed(val key: String, val widthsPerSecond: Float) {
    @SerialName("slow")
    Slow("slow", 0.4f),

    @SerialName("normal")
    Normal("normal", 0.7f),

    @SerialName("fast")
    Fast("fast", 1.1f),
    ;

    companion object {
        fun of(key: String?): TrickSpeed = entries.firstOrNull { it.key == key } ?: Normal
    }
}

/**
 * 울리기 시작할 때 떼어 둔 버튼 속이기 값. 울리는 중에 설정을 바꿔도 이번 울림은 이 값이에요.
 * 예전에 저장된 `level`(강도)은 읽을 때 버려요(결정 155에서 강도를 없앰).
 */
@Serializable
data class ButtonTrick(val speed: TrickSpeed = TrickSpeed.Normal)

/**
 * '확실히 잠깨기' 실험실(결정 146). 모두 처음 꺼짐이에요.
 * - 버튼 속이기: [trickEnabled], [trickSpeed]. 버튼 하나가 울림 화면 안에서 공처럼 튕겨요(결정 155).
 *   화면을 처음 만진 때부터 [TRICK_SECONDS]초 동안만 해요.
 * - 여러 번 묻기: [multiAskEnabled], [multiAskCount](W2 1번 + W3 나머지). 켜면 T1 '한 번 더 확인'은 켜진 채 잠겨요.
 * - [escapeHidden]: 미션을 못 할 때 알림(미션 비상구, 결정 151) 숨기기. 처음 꺼짐(= 알림 뜸)이에요.
 * - [debugMinuteMillis]: 시험용 시간 배율(디버그 빌드의 adb 명령으로만 바뀌어요). 여러 번 묻기의 무작위 간격과
 *   1시간 상한에만 써요. 출시 빌드에서는 늘 1분 = 60초예요.
 */
data class LabSettings(
    val trickEnabled: Boolean = false,
    val trickSpeed: TrickSpeed = TrickSpeed.Normal,
    val multiAskEnabled: Boolean = false,
    val multiAskCount: Int = DEFAULT_ASK_COUNT,
    val escapeHidden: Boolean = false,
    val debugMinuteMillis: Long = MINUTE_MILLIS,
) {
    /** 이번 울림에 걸 버튼 속이기. 꺼져 있으면 null. */
    val trick: ButtonTrick? get() = if (trickEnabled) ButtonTrick(trickSpeed) else null

    companion object {
        const val DEFAULT_ASK_COUNT = 3
        const val MINUTE_MILLIS = 60_000L

        /** T3-2 횟수 3번, 4번, 5번. */
        val ASK_COUNTS = listOf(3, 4, 5)

        /** 버튼 속이기 시간 상한(초). 고정이에요(사용자 확정 11-24). */
        const val TRICK_SECONDS = 20
    }
}

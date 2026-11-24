package com.lumian.morningup.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 버튼 속이기 강도(결정 146 자세히 2-2). 처음 값은 [Normal]. [key]는 저장과 기록에 쓰는 고정 이름이에요. */
@Serializable
enum class TrickLevel(val key: String) {
    /** 버튼이 자리를 옮겨요. */
    @SerialName("weak")
    Weak("weak"),

    /** 옮기다가 잠깐씩 숨어요. */
    @SerialName("normal")
    Normal("normal"),

    /** 똑같은 가짜 버튼이 섞여요. */
    @SerialName("strong")
    Strong("strong"),
    ;

    companion object {
        fun of(key: String?): TrickLevel = entries.firstOrNull { it.key == key } ?: Normal
    }
}

/** 버튼이 옮겨 가고 숨는 빠르기(11-24 사용자 추가). [factor]는 보통 대비 시간 배율이에요(작을수록 빨라요). */
@Serializable
enum class TrickSpeed(val key: String, val factor: Float) {
    @SerialName("slow")
    Slow("slow", 1.5f),

    @SerialName("normal")
    Normal("normal", 1f),

    @SerialName("fast")
    Fast("fast", 0.6f),
    ;

    companion object {
        fun of(key: String?): TrickSpeed = entries.firstOrNull { it.key == key } ?: Normal
    }
}

/** 울리기 시작할 때 떼어 둔 버튼 속이기 값. 울리는 중에 설정을 바꿔도 이번 울림은 이 값이에요. */
@Serializable
data class ButtonTrick(val level: TrickLevel = TrickLevel.Normal, val speed: TrickSpeed = TrickSpeed.Normal)

/**
 * '확실히 잠깨기' 실험실(결정 146). 모두 처음 꺼짐이에요.
 * - 버튼 속이기: [trickEnabled], [trickLevel], [trickSpeed]. 화면을 처음 만진 때부터 [TRICK_SECONDS]초 동안만 해요.
 * - 여러 번 묻기: [multiAskEnabled], [multiAskCount](W2 1번 + W3 나머지). 켜면 T1 '한 번 더 확인'은 켜진 채 잠겨요.
 * - [debugMinuteMillis]: 시험용 시간 배율(디버그 빌드의 adb 명령으로만 바뀌어요). 여러 번 묻기의 무작위 간격과
 *   1시간 상한에만 써요. 출시 빌드에서는 늘 1분 = 60초예요.
 */
data class LabSettings(
    val trickEnabled: Boolean = false,
    val trickLevel: TrickLevel = TrickLevel.Normal,
    val trickSpeed: TrickSpeed = TrickSpeed.Normal,
    val multiAskEnabled: Boolean = false,
    val multiAskCount: Int = DEFAULT_ASK_COUNT,
    val debugMinuteMillis: Long = MINUTE_MILLIS,
) {
    /** 이번 울림에 걸 버튼 속이기. 꺼져 있으면 null. */
    val trick: ButtonTrick? get() = if (trickEnabled) ButtonTrick(trickLevel, trickSpeed) else null

    companion object {
        const val DEFAULT_ASK_COUNT = 3
        const val MINUTE_MILLIS = 60_000L

        /** T3-2 횟수 3번, 4번, 5번. */
        val ASK_COUNTS = listOf(3, 4, 5)

        /** 버튼 속이기 시간 상한(초). 고정이에요(사용자 확정 11-24). */
        const val TRICK_SECONDS = 20
    }
}

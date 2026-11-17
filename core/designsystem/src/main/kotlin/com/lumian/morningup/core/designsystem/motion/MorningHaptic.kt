package com.lumian.morningup.core.designsystem.motion

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * 앱에서 쓰는 진동 종류. 기기 '터치할 때 진동'이 꺼져 있으면
 * `View.performHapticFeedback`이 알아서 진동하지 않아요.
 */
enum class MorningHaptic {
    /** 미션 성공, '깼어요' 같은 확정. */
    Confirm,

    /** 오답, 막힌 동작. */
    Reject,

    /** 토글 켬. */
    ToggleOn,

    /** 토글 끔. */
    ToggleOff,

    /** 작은 선택(요일 칸, 미션 타일). */
    Tick,

    /** 드럼롤이 한 칸 지날 때. 지원 안 하는 폰은 [Tick]과 같아요. */
    SegmentTick,

    /** 일반 버튼 누름. */
    Press,
}

/** 기기 버전에 맞는 진동 상수. 지원 안 하는 버전은 비슷한 옛 상수로 바꿔요. */
@SuppressLint("InlinedApi")
fun MorningHaptic.toFeedbackConstant(sdkInt: Int): Int = when (this) {
    MorningHaptic.Confirm ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    MorningHaptic.Reject ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    MorningHaptic.ToggleOn ->
        if (sdkInt >= UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.VIRTUAL_KEY
    MorningHaptic.ToggleOff ->
        if (sdkInt >= UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CLOCK_TICK
    MorningHaptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
    MorningHaptic.SegmentTick ->
        if (sdkInt >= UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
    MorningHaptic.Press -> HapticFeedbackConstants.VIRTUAL_KEY
}

private const val UPSIDE_DOWN_CAKE = 34

/** 이 화면(View)에서 진동을 울려요. */
fun View.perform(haptic: MorningHaptic) {
    performHapticFeedback(haptic.toFeedbackConstant(Build.VERSION.SDK_INT))
}

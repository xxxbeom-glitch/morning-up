package com.lumian.morningup.core.model

/** 방해 금지 상태(안드로이드 `INTERRUPTION_FILTER_*`를 옮긴 것). */
enum class DndMode { Off, Priority, AlarmsOnly, Total, Unknown }

/**
 * 방해 금지가 알람 소리를 막는지(결정 143, TSK-177 R2). TSK-214 Z플립5 시험대로, 허용 앱이 아니라
 * 방해 금지의 '알람' 허용 하나가 정해요. 울림 화면은 막히지 않고 소리와 진동만 막혀요.
 */
object AlarmSilence {
    /** [alarmsAllowed]는 지금 적용 중인 방해 금지 정책(모드 합친 것)에 '알람'이 들어 있는지예요. */
    fun blocks(mode: DndMode, alarmsAllowed: Boolean): Boolean = when (mode) {
        DndMode.Off, DndMode.AlarmsOnly, DndMode.Unknown -> false
        DndMode.Total -> true
        DndMode.Priority -> !alarmsAllowed
    }
}

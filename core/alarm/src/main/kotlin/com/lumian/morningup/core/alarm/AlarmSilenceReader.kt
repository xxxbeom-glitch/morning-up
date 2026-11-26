package com.lumian.morningup.core.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lumian.morningup.core.model.AlarmSilence
import com.lumian.morningup.core.model.DndMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지금 방해 금지가 알람 소리를 막는지 읽어요(결정 143, TSK-177 R2). 갤럭시 모드도 합쳐진 정책으로 봐요(TSK-214).
 * 앱을 열 때, 울리기 10분 전, 울릴 때 같은 방법으로 읽으려고 따로 뒀어요.
 */
@Singleton
class AlarmSilenceReader @Inject constructor(@ApplicationContext private val context: Context) {
    fun silenced(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val mode = when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> DndMode.Off
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> DndMode.Priority
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> DndMode.AlarmsOnly
            NotificationManager.INTERRUPTION_FILTER_NONE -> DndMode.Total
            else -> DndMode.Unknown
        }
        val policy = if (mode == DndMode.Priority) {
            // 합친 정책을 못 읽으면 기본 정책으로 봐요.
            val consolidated = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    manager.consolidatedNotificationPolicy
                } else {
                    null
                }
            }.getOrNull()
            consolidated ?: runCatching { manager.notificationPolicy }.getOrNull()
        } else {
            null
        }
        val alarmsAllowed = policy == null ||
            policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
        return AlarmSilence.blocks(mode, alarmsAllowed)
    }
}

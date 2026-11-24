package com.lumian.morningup.core.alarm

import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.WakeCheckGate
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.model.engine.AlarmScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 화면용 알람 창구. 저장소에 쓰고 나면 늘 [AlarmRescheduler]로 그 알람의 예약을 맞춰요.
 * 화면(feature)은 저장소와 예약기를 직접 부르지 않고 이것만 써요.
 */
@Singleton
class SchedulingAlarmBook @Inject constructor(
    private val repository: AlarmRepository,
    private val rescheduler: AlarmRescheduler,
    private val scheduler: AlarmScheduler,
    private val now: NowProvider,
    private val checks: WakeCheckGate? = null,
) : AlarmBook {
    override val alarms: Flow<List<Alarm>> = repository.alarms

    override suspend fun get(id: Long): Alarm? = repository.get(id)

    override suspend fun save(alarm: Alarm): Alarm {
        // 예약 시각은 rescheduler가 새로 계산해 넣어요. 옛 값을 남기면 놓친 알람으로 잘못 볼 수 있어요.
        val stored = repository.insert(alarm.copy(nextTriggerAt = null))
        rescheduler.reschedule(stored, now.now())
        return repository.get(stored.id) ?: stored
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = repository.get(id) ?: return
        // 알람을 끄면 그 알람의 남은 확인(W2, W3)도 멈춰요(결정 145).
        if (!enabled) checks?.cancelFor(id)
        rescheduler.reschedule(alarm.copy(enabled = enabled, nextTriggerAt = null), now.now())
    }

    override suspend fun delete(id: Long): Alarm? {
        val alarm = repository.get(id) ?: return null
        scheduler.cancel(id)
        checks?.cancelFor(id)
        repository.delete(id)
        return alarm
    }

    override suspend fun restore(alarm: Alarm) {
        val restored = alarm.copy(nextTriggerAt = null)
        repository.save(restored)
        rescheduler.reschedule(restored, now.now())
    }
}

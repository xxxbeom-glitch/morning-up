package com.lumian.morningup.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 알람 표. 요일은 월=1비트 … 일=7비트 묶음, 미션은 설정 목록 JSON으로 저장해요([MissionCodec], v2). */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDaysMask: Int,
    val skipHolidays: Boolean,
    val enabled: Boolean,
    val missions: String,
    val ringAgainIntervalMinutes: Int,
    val ringAgainCount: Int?,
    val wakeCheckEnabled: Boolean,
    val wakeCheckAskAfterMinutes: Int,
    val wakeCheckAnswerSeconds: Int,
    val powerOffGuard: Boolean,
    val soundId: String,
    val volumePercent: Int,
    val vibrate: Boolean,
    val gentleWake: Boolean,
    val background: String?,
    val skipOnceDate: String?,
    val nextTriggerAt: Long?,
)

internal fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun Int.toDays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

fun AlarmEntity.toModel(): Alarm = Alarm(
    id = id,
    time = LocalTime.of(hour, minute),
    repeatDays = repeatDaysMask.toDays(),
    skipHolidays = skipHolidays,
    enabled = enabled,
    // 모르는 미션(앞으로 지운 미션)은 건너뛰어요.
    missions = MissionCodec.decode(missions),
    ringAgain = RingAgain(ringAgainIntervalMinutes, ringAgainCount),
    wakeCheck = WakeCheck(wakeCheckEnabled, wakeCheckAskAfterMinutes, wakeCheckAnswerSeconds),
    powerOffGuard = powerOffGuard,
    soundId = soundId,
    volumePercent = volumePercent,
    vibrate = vibrate,
    gentleWake = gentleWake,
    background = background,
    skipOnceDate = skipOnceDate?.let(LocalDate::parse),
    nextTriggerAt = nextTriggerAt,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = time.hour,
    minute = time.minute,
    repeatDaysMask = repeatDays.toMask(),
    skipHolidays = skipHolidays,
    enabled = enabled,
    missions = MissionCodec.encode(missions),
    ringAgainIntervalMinutes = ringAgain.intervalMinutes,
    ringAgainCount = ringAgain.count,
    wakeCheckEnabled = wakeCheck.enabled,
    wakeCheckAskAfterMinutes = wakeCheck.askAfterMinutes,
    wakeCheckAnswerSeconds = wakeCheck.answerSeconds,
    powerOffGuard = powerOffGuard,
    soundId = soundId,
    volumePercent = volumePercent,
    vibrate = vibrate,
    gentleWake = gentleWake,
    background = background,
    skipOnceDate = skipOnceDate?.toString(),
    nextTriggerAt = nextTriggerAt,
)

package com.lumian.morningup.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.Snooze
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.WakeCheckSettings
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
    /** W2 '다시 물어보기'(분). 결정 145~179에는 전체 설정이었다가 결정 180에 다시 알람마다예요. */
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
    /** v3: S6 진동 패턴([VibrationPattern.key]). */
    @ColumnInfo(defaultValue = "basic") val vibrationPattern: String = VibrationPattern.Basic.key,
    /** v3: S7 부드럽게 깨우기 길이(초). */
    @ColumnInfo(defaultValue = "30") val gentleSeconds: Int = Alarm.DEFAULT_GENTLE_SECONDS,
    /** v5: W3 '한 번 더 확인'(결정 180). */
    @ColumnInfo(defaultValue = "1") val recheckEnabled: Boolean = true,
    /** v5: W3 '확인까지'(분). */
    @ColumnInfo(defaultValue = "3") val recheckWaitMinutes: Int = WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
    /** v5: W3 '묻는 횟수'. */
    @ColumnInfo(defaultValue = "1") val recheckCount: Int = WakeCheckSettings.DEFAULT_RECHECK_COUNT,
    /** v8: 미루기 켬(결정 195). 처음 꺼짐. */
    @ColumnInfo(defaultValue = "0") val snoozeEnabled: Boolean = false,
    /** v8: 미루기 간격(분). */
    @ColumnInfo(defaultValue = "5") val snoozeMinutes: Int = Snooze.DEFAULT_MINUTES,
    /** v8: 미루기 횟수. */
    @ColumnInfo(defaultValue = "3") val snoozeCount: Int = Snooze.DEFAULT_COUNT,
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
    wakeCheck = WakeCheck(
        enabled = wakeCheckEnabled,
        askAfterMinutes = wakeCheckAskAfterMinutes,
        answerSeconds = wakeCheckAnswerSeconds,
        recheckEnabled = recheckEnabled,
        recheckWaitMinutes = recheckWaitMinutes,
        recheckCount = recheckCount,
    ).normalized(),
    snooze = Snooze(snoozeEnabled, snoozeMinutes, snoozeCount).normalized(),
    powerOffGuard = powerOffGuard,
    soundId = soundId,
    volumePercent = volumePercent,
    vibrate = vibrate,
    gentleWake = gentleWake,
    background = background,
    skipOnceDate = skipOnceDate?.let(LocalDate::parse),
    nextTriggerAt = nextTriggerAt,
    vibrationPattern = VibrationPattern.of(vibrationPattern),
    gentleSeconds = gentleSeconds,
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
    vibrationPattern = vibrationPattern.key,
    gentleSeconds = gentleSeconds,
    recheckEnabled = wakeCheck.recheckEnabled,
    recheckWaitMinutes = wakeCheck.recheckWaitMinutes,
    recheckCount = wakeCheck.recheckCount,
    snoozeEnabled = snooze.enabled,
    snoozeMinutes = snooze.minutes,
    snoozeCount = snooze.count,
)

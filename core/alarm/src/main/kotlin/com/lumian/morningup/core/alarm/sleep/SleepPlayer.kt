package com.lumian.morningup.core.alarm.sleep

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.lumian.morningup.core.alarm.AlarmScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 잠들기 재생 상태. [remainingMillis]는 종료 타이머가 켜졌을 때 남은 시간이에요.
 * [volume]은 0~1이고 타이머 마지막 30초 동안 줄어들어요(소리 파일이 생기면 그대로 써요).
 */
data class SleepPlayback(
    val trackId: String,
    val playing: Boolean = true,
    val timerMinutes: Int? = null,
    val remainingMillis: Long? = null,
    val volume: Float = 1f,
)

/**
 * 잠들기 재생기(Z2). 상태만 들고, 알림과 잠금 화면 조절은 [SleepPlaybackService]가 맡아요.
 * 소리 파일은 아직 없어서 소리 없이 재생, 일시정지, 종료 타이머, 서서히 줄이기만 동작해요.
 * 알람이 울리기 시작하면 [stopForAlarm]으로 바로 멈춰요(알람이 이겨요).
 */
@Singleton
class SleepPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    @AlarmScope private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<SleepPlayback?>(null)
    val state: StateFlow<SleepPlayback?> = mutableState.asStateFlow()

    /** 타이머가 끝나는 때(elapsedRealtime). 일시정지 중에는 null이고 남은 시간만 들고 있어요. */
    private var endsAt: Long? = null
    private var ticker: Job? = null

    /** 그 소리를 틀어요. 다른 소리를 틀던 중이면 타이머는 그대로 이어요. */
    fun play(trackId: String) {
        val current = mutableState.value
        if (current?.trackId == trackId && current.playing) return
        mutableState.value = (current?.copy(trackId = trackId, playing = true) ?: SleepPlayback(trackId))
        resumeTimer()
        startService()
    }

    fun toggle() {
        val current = mutableState.value ?: return
        if (current.playing) pause() else play(current.trackId)
    }

    fun pause() {
        val current = mutableState.value ?: return
        endsAt?.let { mutableState.value = current.copy(remainingMillis = it - SystemClock.elapsedRealtime()) }
        endsAt = null
        ticker?.cancel()
        mutableState.update { it?.copy(playing = false) }
    }

    /** Z2-1. null이면 타이머를 꺼요. */
    fun setTimer(minutes: Int?) {
        val current = mutableState.value ?: return
        mutableState.value = current.copy(
            timerMinutes = minutes,
            remainingMillis = minutes?.let { it * MILLIS_PER_MINUTE },
            volume = 1f,
        )
        endsAt = null
        ticker?.cancel()
        if (current.playing) resumeTimer()
    }

    fun stop() {
        ticker?.cancel()
        endsAt = null
        mutableState.value = null
    }

    /** 알람이 울리기 시작했어요. 잠들기 소리를 바로 꺼요. */
    fun stopForAlarm() {
        if (mutableState.value != null) {
            Timber.i("sleep playback stopped for alarm")
            stop()
        }
    }

    private fun resumeTimer() {
        val remaining = mutableState.value?.remainingMillis ?: return
        endsAt = SystemClock.elapsedRealtime() + remaining
        ticker?.cancel()
        ticker = scope.launch {
            var left = remainingUntil(endsAt)
            while (isActive && left != null && left > 0) {
                val volume = (left.toFloat() / FADE_MILLIS).coerceAtMost(1f)
                mutableState.update { it?.copy(remainingMillis = left, volume = volume) }
                delay(TICK_MILLIS)
                left = remainingUntil(endsAt)
            }
            if (left != null && left <= 0) stop()
        }
    }

    private fun remainingUntil(end: Long?): Long? = end?.let { it - SystemClock.elapsedRealtime() }

    private fun startService() {
        runCatching { context.startForegroundService(Intent(context, SleepPlaybackService::class.java)) }
            .onFailure { Timber.w(it, "sleep service start failed") }
    }

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L

        /** 타이머 마지막 이만큼 동안 소리를 서서히 줄여요. */
        const val FADE_MILLIS = 30_000L
        private const val TICK_MILLIS = 1_000L
    }
}

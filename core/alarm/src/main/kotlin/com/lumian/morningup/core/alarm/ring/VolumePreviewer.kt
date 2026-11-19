package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * S6 볼륨 시트 미리 듣기(11-18 사용자 지시). 알람에 고른 소리를 알람 소리 통로로 들려줘요.
 * 한 번에 10초 울려요(11-19 사용자 결정). 울리는 중에 막대를 움직이면 소리 크기가 바로 바뀌어요.
 * - [volume]: 볼륨 막대를 움직일 때마다 불러요. 그 크기로 [VOLUME_PREVIEW_MILLIS] 동안 울리고, 계속 움직이면 이어서 울려요.
 * - [gentle]: '볼륨 천천히 올리기' 버튼을 누를 때마다 불러요. 실제 알람처럼 작은 소리에서 그 시간 동안 올라가요.
 */
class VolumePreviewer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var target = 1f
    private var rampStartedAt = 0L
    private var rampMillis = 0L
    private val stopper = Runnable { stop() }
    private val rampStep = object : Runnable {
        override fun run() {
            apply()
            if (rampFactor() < 1f) handler.postDelayed(this, RAMP_STEP_MILLIS)
        }
    }

    fun volume(soundId: String?, percent: Int) {
        target = percent.coerceIn(0, FULL_PERCENT) / FULL_PERCENT.toFloat()
        if (player == null || rampMillis > 0) {
            stop()
            if (!start(soundId)) return
        }
        rampMillis = 0
        apply()
        restartTimer(VOLUME_PREVIEW_MILLIS)
    }

    fun gentle(soundId: String?, percent: Int, seconds: Int) {
        stop()
        target = percent.coerceIn(0, FULL_PERCENT) / FULL_PERCENT.toFloat()
        if (!start(soundId)) return
        rampStartedAt = SystemClock.elapsedRealtime()
        rampMillis = seconds * MILLIS_PER_SECOND
        apply()
        if (rampMillis > 0) handler.postDelayed(rampStep, RAMP_STEP_MILLIS)
        restartTimer(if (rampMillis > 0) rampMillis + HOLD_AFTER_RAMP_MILLIS else VOLUME_PREVIEW_MILLIS)
    }

    fun stop() {
        handler.removeCallbacks(stopper)
        handler.removeCallbacks(rampStep)
        rampMillis = 0
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
    }

    private fun start(soundId: String?): Boolean {
        val mp = MediaPlayer()
        val started = runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(context, RingtoneFiles.uriFor(context, soundId))
            mp.isLooping = true
            mp.prepare()
            mp.setVolume(0f, 0f)
            mp.start()
        }.isSuccess
        if (started) player = mp else mp.release()
        return started
    }

    private fun apply() {
        val v = RingMute.volume(target * rampFactor())
        player?.setVolume(v, v)
    }

    private fun rampFactor(): Float {
        if (rampMillis <= 0) return 1f
        val done = (SystemClock.elapsedRealtime() - rampStartedAt).toFloat() / rampMillis
        return (RAMP_FLOOR + (1f - RAMP_FLOOR) * done).coerceIn(RAMP_FLOOR, 1f)
    }

    private fun restartTimer(millis: Long) {
        handler.removeCallbacks(stopper)
        handler.postDelayed(stopper, millis)
    }

    private companion object {
        const val FULL_PERCENT = 100
        const val VOLUME_PREVIEW_MILLIS = 10_000L
        const val HOLD_AFTER_RAMP_MILLIS = 1_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val RAMP_STEP_MILLIS = 100L
        const val RAMP_FLOOR = 0.05f
    }
}

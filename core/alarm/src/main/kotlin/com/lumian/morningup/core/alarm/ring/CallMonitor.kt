package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 통화 중인지 알아채요. 전화 권한(READ_PHONE_STATE) 없이 소리 장치의 모드만 봐요:
 * 전화 통화(IN_CALL), 인터넷 통화(IN_COMMUNICATION, 카카오톡 보이스톡 등), 전화가 울리는 중(RINGTONE).
 * 안드로이드 12부터는 모드가 바뀔 때 알림을 받고, 그 전에는 1초마다 확인해요(울리는 동안만).
 */
@Singleton
class CallMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val executor = ContextCompat.getMainExecutor(context)
    private val handler = Handler(Looper.getMainLooper())
    private var listener: ((Boolean) -> Unit)? = null
    private var last: Boolean? = null

    private var modeListener: Any? = null
    private val poll = object : Runnable {
        override fun run() {
            notifyIfChanged()
            handler.postDelayed(this, POLL_MILLIS)
        }
    }

    fun inCall(): Boolean = isCallMode(audio?.mode ?: AudioManager.MODE_NORMAL)

    /** 통화 상태가 바뀔 때마다 [onChange]를 불러요. 한 번에 한 곳만 들어요. */
    fun start(onChange: (Boolean) -> Unit) {
        stop()
        listener = onChange
        last = inCall()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modeListener = addModeListener()
        } else {
            handler.postDelayed(poll, POLL_MILLIS)
        }
    }

    fun stop() {
        if (listener == null) return
        listener = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { audio?.removeOnModeChangedListener(it) }
            modeListener = null
        } else {
            handler.removeCallbacks(poll)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun addModeListener(): AudioManager.OnModeChangedListener {
        val l = AudioManager.OnModeChangedListener { notifyIfChanged() }
        audio?.addOnModeChangedListener(executor, l)
        return l
    }

    private fun notifyIfChanged() {
        val now = inCall()
        if (now != last) {
            last = now
            listener?.invoke(now)
        }
    }

    companion object {
        private const val POLL_MILLIS = 1_000L

        fun isCallMode(mode: Int): Boolean = mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
    }
}

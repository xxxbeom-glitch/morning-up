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

/** 지금 통화 중인지. 울림 상태표가 W2, W3를 미룰지 정할 때 봐요(결정 145 '통화 중'). */
fun interface CallState {
    fun inCall(): Boolean

    companion object {
        val NotInCall = CallState { false }
    }
}

/**
 * 통화 중인지 알아채요. 전화 권한(READ_PHONE_STATE) 없이 소리 장치의 모드만 봐요:
 * 전화 통화(IN_CALL), 인터넷 통화(IN_COMMUNICATION, 카카오톡 보이스톡 등), 전화가 울리는 중(RINGTONE).
 * 안드로이드 12부터는 모드가 바뀔 때 알림을 받고, 그 전에는 1초마다 확인해요(듣는 곳이 있을 때만).
 * 알람 소리([AlarmSoundPlayer])와 울림 서비스(W2, W3 미루기)가 이름([key])을 붙여 따로 들어요.
 */
@Singleton
class CallMonitor @Inject constructor(@ApplicationContext context: Context) : CallState {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val executor = ContextCompat.getMainExecutor(context)
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = LinkedHashMap<String, (Boolean) -> Unit>()
    private var last: Boolean? = null

    private var modeListener: Any? = null
    private val poll = object : Runnable {
        override fun run() {
            notifyIfChanged()
            handler.postDelayed(this, POLL_MILLIS)
        }
    }

    override fun inCall(): Boolean = isCallMode(audio?.mode ?: AudioManager.MODE_NORMAL)

    /** 통화 상태가 바뀔 때마다 [onChange]를 불러요. 같은 [key]로 다시 부르면 앞의 것을 바꿔요. */
    @Synchronized
    fun start(key: String = DEFAULT_KEY, onChange: (Boolean) -> Unit) {
        val first = listeners.isEmpty()
        listeners[key] = onChange
        if (!first) return
        last = inCall()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modeListener = addModeListener()
        } else {
            handler.postDelayed(poll, POLL_MILLIS)
        }
    }

    @Synchronized
    fun stop(key: String = DEFAULT_KEY) {
        if (listeners.remove(key) == null || listeners.isNotEmpty()) return
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
        val targets = synchronized(this) {
            if (now == last) return
            last = now
            listeners.values.toList()
        }
        targets.forEach { it(now) }
    }

    companion object {
        private const val POLL_MILLIS = 1_000L
        const val DEFAULT_KEY = "sound"

        fun isCallMode(mode: Int): Boolean = mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
    }
}

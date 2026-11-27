package com.lumian.morningup.core.alarm.sleep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.alarm.SoundNames
import com.lumian.morningup.core.model.SleepTracks
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 잠들기 재생 포그라운드 서비스(`mediaPlayback`). 알림과 미디어 세션으로 잠금 화면에서도 재생, 일시정지, 끄기를 해요.
 * 재생 상태는 [SleepPlayer]가 들고 있고, 상태가 비면(끄기, 타이머 끝, 알람 울림) 스스로 멈춰요.
 */
@AndroidEntryPoint
class SleepPlaybackService : LifecycleService() {
    @Inject lateinit var player: SleepPlayer

    private lateinit var session: MediaSession
    private val manager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_sleep), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.channel_sleep_description)
                setShowBadge(false)
            },
        )
        session = MediaSession(this, TAG).apply {
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() = player.state.value?.let { player.play(it.trackId) } ?: Unit

                    override fun onPause() = player.pause()

                    override fun onStop() = player.stop()
                },
            )
            isActive = true
        }
        lifecycleScope.launch {
            player.state
                .map { it?.let { s -> s.trackId to s.playing } }
                .distinctUntilChanged()
                .collect { now -> if (now == null) finish() else show(now.first, now.second) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_TOGGLE -> player.toggle()
            ACTION_STOP -> player.stop()
        }
        val current = player.state.value
        if (current == null) {
            finish()
        } else {
            show(current.trackId, current.playing)
        }
        return START_NOT_STICKY
    }

    private fun show(trackId: String, playing: Boolean) {
        val track = SleepTracks.find(trackId)
        val title = track?.let { getString(SoundNames.sleepTrack(it.id)) }.orEmpty()
        val group = SleepTracks.groupOf(trackId)?.let { getString(SoundNames.sleepGroup(it.key)) }.orEmpty()
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, group)
                .build(),
        )
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP,
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(title)
            .setContentText(group)
            .setOngoing(playing)
            .setContentIntent(openApp())
            .addAction(action(if (playing) R.string.sleep_pause else R.string.sleep_play, ACTION_TOGGLE, 0))
            .addAction(action(R.string.sleep_stop, ACTION_STOP, 1))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1))
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun action(label: Int, action: String, requestCode: Int): Notification.Action {
        val intent = PendingIntent.getService(
            this,
            requestCode,
            Intent(this, SleepPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_moon), getString(label), intent)
            .build()
    }

    private fun openApp(): PendingIntent? = packageManager.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun finish() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MorningSleep"
        const val CHANNEL = "sleep_playback"
        const val NOTIFICATION_ID = 4_201
        const val ACTION_TOGGLE = "com.lumian.morningup.sleep.TOGGLE"
        const val ACTION_STOP = "com.lumian.morningup.sleep.STOP"
    }
}

package com.lumian.morningup.feature.sleep

import androidx.lifecycle.ViewModel
import com.lumian.morningup.core.alarm.sleep.SleepPlayback
import com.lumian.morningup.core.alarm.sleep.SleepPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Z2 재생 화면 길. [trackId]는 [com.lumian.morningup.core.model.SleepTracks]의 id예요. */
@Serializable
data class SleepPlayerRoute(val trackId: String)

/** 잠들기 탭(Z1)과 재생 화면(Z2)이 같이 쓰는 재생 상태. 재생기는 앱 전체에 하나라 화면을 닫아도 이어져요. */
@HiltViewModel
class SleepViewModel @Inject constructor(private val player: SleepPlayer) : ViewModel() {
    val playback: StateFlow<SleepPlayback?> = player.state

    fun play(trackId: String) = player.play(trackId)

    /** 이 소리를 틀고 있으면 멈추고, 아니면 이 소리를 틀어요(타이머가 끝나 꺼진 뒤 다시 틀 때도). */
    fun toggle(trackId: String) {
        val current = player.state.value
        if (current?.trackId == trackId && current.playing) player.pause() else player.play(trackId)
    }

    fun setTimer(minutes: Int?) = player.setTimer(minutes)
}

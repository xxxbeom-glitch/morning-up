package com.lumian.morningup.core.alarm

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 버튼 속이기 접근성 자동 꺼짐은 터치 입력을 바꾸는 서비스에서만(ISS-041). */
@RunWith(RobolectricTestRunner::class)
class TrickAccessibilityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun touchReplacingServices_turnTrickOff() {
        listOf(
            "com.google.android.marvin.talkback/com.android.switchaccess.SwitchAccessService",
            "com.google.android.apps.accessibility.voiceaccess/.JustSpeakService",
            "com.samsung.accessibility/.universalswitch.UniversalSwitchService",
            "com.samsung.accessibility/.assistantmenu.serviceframework.AssistantMenuService",
        ).forEach { assertThat(TrickAccessibility.replacesTouch(it, 0)).isTrue() }
    }

    @Test
    fun touchExplorationFlag_turnsTrickOff() {
        val flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        assertThat(TrickAccessibility.replacesTouch("com.example.reader/.Reader", flags)).isTrue()
    }

    @Test
    fun ordinaryServices_keepTrick() {
        listOf(
            "com.sky.browserwipe/.WipeService",
            "com.example.notireader/.ReadAloudService",
            "net.dinglisch.android.taskerm/.MyAccessibilityService",
            null,
        ).forEach { assertThat(TrickAccessibility.replacesTouch(it, 0)).isFalse() }
    }

    @Test
    fun offReason_nothingEnabled_isNull_orNoAnimation() {
        assertThat(TrickAccessibility.offReason(context)).isNull()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertThat(TrickAccessibility.offReason(context)).isEqualTo(TrickAccessibility.NO_ANIMATION)
    }
}

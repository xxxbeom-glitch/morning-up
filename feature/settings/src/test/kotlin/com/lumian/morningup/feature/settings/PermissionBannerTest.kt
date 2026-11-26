package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.PermissionBanner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** H2-6, H2-7, H2-8 배너 글과 '그 밖에 n개 더'(결정 143, TSK-177 답 7, 시안 TSK-220). */
@RunWith(RobolectricTestRunner::class)
class PermissionBannerTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0

    private fun show(banner: PermissionBanner, more: Int = 0) = compose.setContent {
        MorningTheme(darkTheme = false, reduceMotion = true) {
            PermissionBanner(banner, onClick = { clicks++ }, more = more)
        }
    }

    @Test
    fun silenced_showsMoreCount_andView() {
        show(PermissionBanner.AlarmsSilenced, more = 2)
        compose.onNodeWithText("방해 금지가 알람 소리를 막아요").assertExists()
        compose.onNodeWithText("'알람' 허용을 켜 주세요. 그 밖에 2개 더").assertExists()
        compose.onNodeWithText("보기").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun restricted_alone_hasNoMore() {
        show(PermissionBanner.BatteryRestricted)
        compose.onNodeWithText("배터리가 '제한'으로 되어 있어요").assertExists()
        compose.onNodeWithText("알람이 울리지 않을 수 있어요").assertExists()
        compose.onNodeWithText("켜기").assertExists()
    }

    @Test
    fun exactAlarm_texts() {
        show(PermissionBanner.ExactAlarm)
        compose.onNodeWithText("알람이 몇 분 늦게 울릴 수 있어요").assertExists()
        compose.onNodeWithText("'정확한 알람'을 켜 주세요").assertExists()
    }
}

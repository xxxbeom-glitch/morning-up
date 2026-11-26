package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** T2-1 '방해 금지 중에도 울리게'(TSK-177 답 4): 한 단계 카드, '알아 두세요', 설정 열기 버튼. */
@RunWith(RobolectricTestRunner::class)
class DndGuidePageTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsStepNoteAndOpensSettings() {
        var opened = 0
        var back = 0
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                DndGuidePage(onBack = { back++ }, onOpenSettings = { opened++ })
            }
        }
        compose.onNodeWithText("방해 금지 중에도 울리게").assertExists()
        compose.onNodeWithText("방해 금지에서 '알람' 켜기").assertExists()
        compose.onNodeWithText("알아 두세요").performScrollTo().assertExists()
        compose.onNodeWithText("방해 금지 설정 열기").performClick()
        assertThat(opened).isEqualTo(1)
        assertThat(back).isEqualTo(0)
    }
}

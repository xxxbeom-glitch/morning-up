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
        compose.onNodeWithText("방해 금지 모드에서 허용하기").assertExists()
        compose.onNodeWithText("방해 금지 모드에서 '알람' 켜기").assertExists()
        compose.onNodeWithText("알아 두세요").performScrollTo().assertExists()
        compose.onNodeWithText("방해 금지 설정 열기").performClick()
        assertThat(opened).isEqualTo(1)
        assertThat(back).isEqualTo(0)
    }

    /** TSK-287: 맨 위 스위치. 지원 안 되는 폰(null)에서는 숨겨요. */
    @Test
    fun switchCard_togglesAndHidesWhenUnsupported() {
        var on: Boolean? = null
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                DndGuidePage(onBack = {}, onOpenSettings = {}, override = false, onOverride = { on = it })
            }
        }
        compose.onNodeWithText("알람이 울리는 동안만 허용").performClick()
        assertThat(on).isTrue()
    }

    @Test
    fun switchCard_hiddenWhenNull() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                DndGuidePage(onBack = {}, onOpenSettings = {})
            }
        }
        compose.onNodeWithText("알람이 울리는 동안만 허용").assertDoesNotExist()
    }
}

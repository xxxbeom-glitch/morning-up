package com.lumian.morningup.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** T7-1 문의 쓰기(TSK-289): 빈도 칩은 오류 신고일 때만, 보내기는 조건을 채워야 눌려요. */
@RunWith(RobolectricTestRunner::class)
class SupportPageTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sendEnabledOnlyWhenComplete() {
        var form by mutableStateOf(SupportForm())
        var sent = 0
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SupportPage(form = form, onForm = { form = it }, onSend = { sent++ }, onBack = {})
            }
        }
        compose.onNodeWithText("자주 그런가요?").assertDoesNotExist()
        compose.onNodeWithTag(SUPPORT_SEND_TAG).assertIsNotEnabled()
        compose.onNodeWithText("오류 신고").performClick()
        compose.onNodeWithText("자주 그런가요?").assertExists()
        assertThat(form.withLog).isTrue()
        compose.onNodeWithTag(SUPPORT_BODY_TAG).performTextInput("오전 6:30 알람이 울리지 않았어요")
        compose.onNodeWithTag(SUPPORT_SEND_TAG).assertIsNotEnabled()
        compose.onNodeWithText("위 내용에 동의해요").performScrollTo().performClick()
        compose.onNodeWithTag(SUPPORT_SEND_TAG).assertIsEnabled().performClick()
        assertThat(sent).isEqualTo(1)
    }

    @Test
    fun questionType_logOffByDefault() {
        var form by mutableStateOf(SupportForm())
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SupportPage(form = form, onForm = { form = it }, onSend = {}, onBack = {})
            }
        }
        compose.onNodeWithText("궁금한 점").performClick()
        assertThat(form.withLog).isFalse()
        compose.onNodeWithText("자주 그런가요?").assertDoesNotExist()
    }
}

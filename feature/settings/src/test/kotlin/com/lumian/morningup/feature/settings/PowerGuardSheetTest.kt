package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PowerGuardSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sheet_showsDecidedCopy_andButtonsWork() {
        var agreed = false
        var later = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                PowerGuardSheet(onAgree = { agreed = true }, onLater = { later = true })
            }
        }
        compose.onNodeWithText("미션 중 전원 메뉴를 막으려면\n접근성 권한이 필요해요").assertExists()
        compose.onNodeWithText("화면 내용은 읽거나 저장하지 않아요.\n'설치된 앱'에서 morning-up을 켜 주세요.").assertExists()
        compose.onNodeWithText("동의하고 설정 열기").performClick()
        compose.onNodeWithText("나중에").performClick()
        assertThat(agreed).isTrue()
        assertThat(later).isTrue()
    }
}

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

/** T6 자주 묻는 질문(TSK-288): 세 묶음, 답 아래 글자 버튼, 맨 아래 '답을 못 찾았어요'. */
@RunWith(RobolectricTestRunner::class)
class FaqPageTest {
    @get:Rule
    val compose = createComposeRule()

    private var permissions = 0
    private var dnd = 0
    private var notFound = 0

    private fun setPage() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                FaqPage(
                    onBack = {},
                    onOpenPermissions = { permissions++ },
                    onOpenDndGuide = { dnd++ },
                    onNotFound = { notFound++ },
                )
            }
        }
    }

    @Test
    fun groupsAndNotFound() {
        setPage()
        compose.onNodeWithText("알람이 울리지 않을 때").assertExists()
        compose.onNodeWithText("권한").assertExists()
        compose.onNodeWithText("기능").assertExists()
        compose.onNodeWithText("답을 못 찾았어요").performScrollTo().performClick()
        assertThat(notFound).isEqualTo(1)
    }

    @Test
    fun answerButtons_openTargets() {
        setPage()
        compose.onNodeWithText("알람이 아예 울리지 않았어요").performClick()
        compose.onNodeWithText("권한 화면 열기").performScrollTo().performClick()
        assertThat(permissions).isEqualTo(1)
        compose.onNodeWithText("알람 화면은 떴는데 소리가 안 났어요").performScrollTo().performClick()
        compose.onNodeWithText("방해 금지 모드에서 허용하기 열기").performScrollTo().performClick()
        assertThat(dnd).isEqualTo(1)
    }
}

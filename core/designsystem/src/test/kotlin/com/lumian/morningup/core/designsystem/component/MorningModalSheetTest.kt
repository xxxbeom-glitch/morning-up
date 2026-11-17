package com.lumian.morningup.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MorningModalSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsContentOnlyWhenVisible() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningModalSheet(visible = false, onDismiss = {}) { Text("시트 안") }
            }
        }
        compose.onNodeWithText("시트 안").assertDoesNotExist()
    }

    @Test
    fun visibleSheet_showsContent() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningModalSheet(visible = true, onDismiss = {}) { Text("시트 안") }
            }
        }
        compose.onNodeWithText("시트 안").assertExists()
    }
}

package com.lumian.morningup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Settings
import com.lumian.morningup.R
import com.lumian.morningup.core.designsystem.component.MorningBottomNav
import com.lumian.morningup.core.designsystem.component.MorningNavItem
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 앱의 세 탭. 순서가 하단 메뉴 순서예요. */
enum class TopTab { Alarm, Sleep, Settings }

/**
 * 앱 껍데기: 탭 내용 + 아래 가운데 떠 있는 하단 메뉴. [alarmTab]은 알람 탭(홈), [settingsTab]은 설정 탭(T1)이에요.
 */
@Composable
fun MorningUpApp(
    modifier: Modifier = Modifier,
    alarmTab: @Composable (Modifier) -> Unit = { Placeholder(stringResource(R.string.tab_alarm)) },
    settingsTab: @Composable (Modifier) -> Unit = { Placeholder(stringResource(R.string.tab_settings)) },
    sleepTab: @Composable (Modifier) -> Unit = { Placeholder(stringResource(R.string.tab_sleep)) },
    settingsAlert: Boolean = false,
    hideNav: Boolean = false,
) {
    var tab by rememberSaveable { mutableStateOf(TopTab.Alarm) }
    val labels = listOf(
        stringResource(R.string.tab_alarm),
        stringResource(R.string.tab_sleep),
        stringResource(R.string.tab_settings),
    )
    // 잠들기 탭(Z1)은 늘 다크 모드예요. 하단 메뉴도 같이 어둡게 바꿔요.
    MorningTheme(
        darkTheme = tab == TopTab.Sleep || MorningTheme.colors.isDark,
        reduceMotion = MorningTheme.reduceMotion,
    ) {
        SystemBarIcons(dark = MorningTheme.colors.isDark)
        AppFrame(modifier, tab, labels, settingsAlert, hideNav, onSelect = { tab = it }) {
            when (tab) {
                TopTab.Alarm -> alarmTab(Modifier.fillMaxSize())
                TopTab.Sleep -> sleepTab(Modifier.fillMaxSize())
                TopTab.Settings -> settingsTab(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun AppFrame(
    modifier: Modifier,
    tab: TopTab,
    labels: List<String>,
    settingsAlert: Boolean,
    hideNav: Boolean,
    onSelect: (TopTab) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase),
    ) {
        content()
        // 홈 선택 모드(H3-1)에서는 하단 메뉴 자리에 홈의 켜기, 끄기, 복사, 삭제 바가 떠요.
        if (!hideNav) {
            MorningBottomNav(
                items = listOf(
                    MorningNavItem(Lucide.AlarmClock, labels[0]),
                    MorningNavItem(Lucide.Moon, labels[1]),
                    MorningNavItem(Lucide.Settings, labels[2]),
                ),
                selectedIndex = tab.ordinal,
                onSelect = { onSelect(TopTab.entries[it]) },
                // 꺼진 필수 권한이 있으면 설정 아이콘 위에 '확인 필요' 말풍선(design-system 하단 메뉴 Alert).
                alertIndex = TopTab.Settings.ordinal.takeIf { settingsAlert },
                alertLabel = stringResource(R.string.tab_settings_alert),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = MorningDimens.space16),
            )
        }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.tab_placeholder, label),
            style = MorningTheme.typography.bodyMedium,
            color = MorningTheme.colors.textTertiary,
        )
    }
}

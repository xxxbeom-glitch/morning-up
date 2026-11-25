package com.lumian.morningup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** 앱의 탭. 순서가 하단 메뉴 순서예요. 잠들기 탭은 수면 라운지 스위치를 켰을 때만 보여요(결정 140, 141). */
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
    sleepLounge: Boolean = false,
    hideNav: Boolean = false,
    alarmTabRequested: Boolean = false,
    onAlarmTabShown: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(TopTab.Alarm) }
    // 배터리 전날 밤 알림을 누르면 권한 시트는 홈 위에 떠요(결정 129). 다른 탭에 있었으면 알람 탭으로 돌려요.
    LaunchedEffect(alarmTabRequested) {
        if (alarmTabRequested) {
            tab = TopTab.Alarm
            onAlarmTabShown()
        }
    }
    val tabs = if (sleepLounge) TopTab.entries.toList() else listOf(TopTab.Alarm, TopTab.Settings)
    // 잠들기 탭(Z1)은 늘 다크 모드예요. 하단 메뉴도 같이 어둡게 바꿔요.
    MorningTheme(
        darkTheme = tab == TopTab.Sleep || MorningTheme.colors.isDark,
        reduceMotion = MorningTheme.reduceMotion,
    ) {
        SystemBarIcons(dark = MorningTheme.colors.isDark)
        AppFrame(modifier, tabs, tab, settingsAlert, hideNav, onSelect = { tab = it }) {
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
    tabs: List<TopTab>,
    tab: TopTab,
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
        // 홈 선택 모드(H3-1)에서는 하단 메뉴 자리에 홈의 켜기, 끄기, 복제, 삭제 바가 떠요.
        if (!hideNav) {
            MorningBottomNav(
                items = tabs.map { navItem(it) },
                selectedIndex = tabs.indexOf(tab).coerceAtLeast(0),
                onSelect = { onSelect(tabs[it]) },
                // 꺼진 필수 권한이 있으면 설정 아이콘 위에 '확인 필요' 말풍선(design-system 하단 메뉴 Alert).
                alertIndex = tabs.indexOf(TopTab.Settings).takeIf { settingsAlert },
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
private fun navItem(tab: TopTab): MorningNavItem = when (tab) {
    TopTab.Alarm -> MorningNavItem(Lucide.AlarmClock, stringResource(R.string.tab_alarm))
    TopTab.Sleep -> MorningNavItem(Lucide.Moon, stringResource(R.string.tab_sleep))
    TopTab.Settings -> MorningNavItem(Lucide.Settings, stringResource(R.string.tab_settings))
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

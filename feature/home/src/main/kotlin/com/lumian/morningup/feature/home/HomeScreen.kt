package com.lumian.morningup.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.lumian.morningup.core.designsystem.component.MorningBottomNavHeight
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningSnackbar
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.RollingText
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.motion.pressScale
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import java.time.DayOfWeek
import kotlinx.coroutines.delay

/** 하늘 자리 높이(Home Sky 360x420). 하늘 장면은 7단계에서 그려요. */
private val SkyHeight = 420.dp

/** 꺼진 카드의 시각과 요일 투명도. */
private const val OFF_ALPHA = 0.45f

/** 토스트가 머무는 시간, 스낵바가 머무는 시간. */
private const val TOAST_MILLIS = 2_500L
private const val SNACKBAR_MILLIS = 4_000L

/**
 * 알람 탭 홈. [savedAlarmId]는 편집 화면에서 저장하고 돌아왔을 때 앱이 넘겨줘요(H6 토스트).
 * [onOpenEdit]은 null이면 새 알람, 아니면 그 알람의 편집 화면을 열어요.
 */
@Composable
fun HomeRoute(
    onOpenEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    savedAlarmId: Long? = null,
    onSavedAlarmShown: () -> Unit = {},
    banner: (@Composable () -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(savedAlarmId) {
        if (savedAlarmId != null) {
            viewModel.onAlarmSaved(savedAlarmId)
            onSavedAlarmShown()
        }
    }
    HomeScreen(
        state = state,
        onAdd = { onOpenEdit(null) },
        onOpen = { onOpenEdit(it.id) },
        onToggle = viewModel::setEnabled,
        onLongPress = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onUndo = viewModel::undoDelete,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
        banner = banner,
    )
}

/** H0 홈, H1 빈 홈, H5 삭제 확인, H6 저장 토스트, H7 삭제 스낵바. 상태와 콜백만 받아요. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: () -> Unit,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onLongPress: (Alarm) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onUndo: () -> Unit,
    onDismissMessage: (HomeMessage) -> Unit,
    modifier: Modifier = Modifier,
    onMore: () -> Unit = {},
    banner: (@Composable () -> Unit)? = null,
) {
    val navReserve = MorningBottomNavHeight + MorningDimens.space16
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase),
    ) {
        SkyBackdrop()
        if (!state.loading) {
            if (state.alarms.isEmpty()) {
                EmptyHome(state = state, onAdd = onAdd, bottomReserve = navReserve, banner = banner)
            } else {
                AlarmList(
                    state = state,
                    onAdd = onAdd,
                    onMore = onMore,
                    onOpen = onOpen,
                    onToggle = onToggle,
                    onLongPress = onLongPress,
                    bottomReserve = navReserve,
                    banner = banner,
                )
            }
        }
        Messages(
            message = state.message,
            onUndo = onUndo,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = navReserve + MorningDimens.space12)
                .padding(horizontal = MorningDimens.layoutScreenMargin),
        )
        state.pendingDelete?.let { alarm ->
            DeleteDialog(alarm = alarm, onCancel = onCancelDelete, onConfirm = onConfirmDelete)
        }
    }
}

/** 하늘 자리: 아침 하늘 토큰 그라데이션(sky/2 → sky/4 → 바탕). 해, 달, 날씨는 7단계. */
@Composable
private fun SkyBackdrop() {
    val colors = MorningTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(SkyHeight)
            .background(Brush.verticalGradient(listOf(colors.sky2, colors.sky4, colors.backgroundBase))),
    )
}

@Composable
private fun Header(
    headline: String,
    subline: String,
    modifier: Modifier = Modifier,
    banner: (@Composable () -> Unit)? = null,
) {
    val colors = MorningTheme.colors
    if (banner != null) {
        // H2: 배너가 있으면 상태 표시줄 아래 배너, 그 아래 20 띄우고 다음 알람 문구(Next Alarm Header Banner 형).
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                ),
        ) { banner() }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (banner == null) Modifier.statusBarsPadding() else Modifier)
            .padding(
                top = if (banner == null) MorningDimens.layoutHeaderTop else MorningDimens.space20,
                bottom = MorningDimens.space32,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RollingText(text = headline, style = MorningTheme.typography.headlineLarge, color = colors.textPrimary)
        Spacer(Modifier.height(MorningDimens.space8))
        Text(text = subline, style = MorningTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun headerTexts(state: HomeUiState): Pair<String, String> {
    val res = alarmResources()
    val next = state.next
    return if (next != null) {
        stringResource(R.string.home_next_headline, AlarmText.until(res, next.until)) to
            stringResource(
                R.string.home_next_subline,
                AlarmText.date(res, next.at.toLocalDate()),
                AlarmText.time(res, next.at.toLocalTime()),
            )
    } else {
        stringResource(R.string.home_empty_headline) to (state.today?.let { AlarmText.date(res, it) }.orEmpty())
    }
}

@Composable
private fun AlarmList(
    state: HomeUiState,
    onAdd: () -> Unit,
    onMore: () -> Unit,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onLongPress: (Alarm) -> Unit,
    bottomReserve: Dp,
    banner: (@Composable () -> Unit)? = null,
) {
    val (headline, subline) = headerTexts(state)
    val reduceMotion = MorningTheme.reduceMotion
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = navBar + bottomReserve + MorningDimens.space16),
        verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
    ) {
        item(key = "header") { Column { Header(headline, subline, banner = banner) } }
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MorningDimens.space4),
                horizontalArrangement = Arrangement.End,
            ) {
                MorningIconButton(Lucide.Plus, stringResource(R.string.home_add), onAdd)
                MorningIconButton(Lucide.EllipsisVertical, stringResource(R.string.home_more), onMore)
            }
        }
        items(state.alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onClick = { onOpen(alarm) },
                onLongPress = { onLongPress(alarm) },
                onToggle = { onToggle(alarm, it) },
                modifier = Modifier
                    .padding(horizontal = MorningDimens.layoutScreenMargin)
                    .animateItem(
                        fadeInSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
                        placementSpec = MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion),
                        fadeOutSpec = MorningMotion.effects(MorningMotion.EffectsFast),
                    ),
            )
        }
    }
}

/** 피그마 Alarm Card(State On, Off). 시각, 월~일 요일(고른 요일만 파랑), 오른쪽 토글. 길게 누르면 삭제 확인. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val res = alarmResources()
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val time = AlarmText.time(res, alarm.time)
    val description = stringResource(R.string.home_card_description, time, AlarmText.repeat(res, alarm.repeatDays))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .morningShadow(MorningShadows.card, shape)
            .background(colors.surfaceCard, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = {
                    view.perform(MorningHaptic.Confirm)
                    onLongPress()
                },
                onClick = onClick,
            )
            .padding(MorningDimens.layoutCardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (alarm.enabled) 1f else OFF_ALPHA)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            Text(text = time, style = MorningTheme.typography.headlineNumber, color = colors.textPrimary)
            Spacer(Modifier.height(MorningDimens.space4))
            DaysRow(alarm.repeatDays)
        }
        MorningToggle(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics { contentDescription = time },
        )
    }
}

/** 월~일. 고른 요일은 `Caption/Strong` `primary/text`, 나머지는 `Caption/Medium` `text/tertiary`. */
@Composable
private fun DaysRow(days: Set<DayOfWeek>) {
    val res = alarmResources()
    Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        DayOfWeek.entries.forEach { day ->
            val on = day in days
            Text(
                text = AlarmText.dayShort(res, day),
                style = if (on) MorningTheme.typography.captionStrong else MorningTheme.typography.captionMedium,
                color = if (on) MorningTheme.colors.primaryText else MorningTheme.colors.textTertiary,
            )
        }
    }
}

/** H1 빈 홈. 플러스와 더보기를 숨기고 가운데 안내와 '알람 만들기'만 둬요. */
@Composable
private fun EmptyHome(
    state: HomeUiState,
    onAdd: () -> Unit,
    bottomReserve: Dp,
    banner: (@Composable () -> Unit)? = null,
) {
    val colors = MorningTheme.colors
    val (headline, subline) = headerTexts(state)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = bottomReserve),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(headline, subline, banner = banner)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MorningDimens.layoutScreenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(EmptyIconBox)
                    .background(colors.surfaceCard, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.AlarmClock,
                    contentDescription = null,
                    tint = colors.primaryDefault,
                    modifier = Modifier.size(MorningDimens.iconLg),
                )
            }
            Spacer(Modifier.height(MorningDimens.space16))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MorningTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(MorningDimens.space20))
            MorningButton(
                text = stringResource(R.string.home_empty_button),
                onClick = onAdd,
                size = MorningButtonSize.Medium,
                modifier = Modifier.width(EmptyButtonWidth),
            )
            Spacer(Modifier.height(MorningDimens.space48))
        }
    }
}

private val EmptyIconBox = 64.dp
private val EmptyButtonWidth = 160.dp

@Composable
private fun Messages(
    message: HomeMessage?,
    onUndo: () -> Unit,
    onDismiss: (HomeMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = alarmResources()
    LaunchedEffect(message) {
        if (message != null) {
            delay(if (message is HomeMessage.Deleted) SNACKBAR_MILLIS else TOAST_MILLIS)
            onDismiss(message)
        }
    }
    MorningMessageHost(message = message, modifier = modifier) { shown ->
        when (shown) {
            is HomeMessage.Saved -> MorningToast(
                text = shown.until
                    ?.let { stringResource(R.string.home_saved, AlarmText.until(res, it)) }
                    ?: stringResource(R.string.home_saved_off),
            )
            is HomeMessage.Deleted -> MorningSnackbar(
                text = stringResource(R.string.home_deleted),
                actionLabel = stringResource(R.string.home_undo),
                onAction = onUndo,
                onSwipeDismiss = { onDismiss(shown) },
            )
        }
    }
}

@Composable
private fun DeleteDialog(alarm: Alarm, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val res = alarmResources()
    val time = AlarmText.time(res, alarm.time)
    val repeat = AlarmText.repeat(res, alarm.repeatDays)
    MorningDialog(
        title = stringResource(R.string.home_delete_title),
        message = if (alarm.missions.isEmpty()) {
            stringResource(R.string.home_delete_message, time, repeat)
        } else {
            stringResource(R.string.home_delete_message_missions, time, repeat)
        },
        dismissLabel = stringResource(R.string.home_delete_cancel),
        confirmLabel = stringResource(R.string.home_delete_confirm),
        onDismiss = onCancel,
        onConfirm = onConfirm,
        danger = true,
    )
}

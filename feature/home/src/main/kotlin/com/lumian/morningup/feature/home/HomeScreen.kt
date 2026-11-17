package com.lumian.morningup.feature.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.AlarmClockOff
import com.composables.icons.lucide.BellOff
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.lumian.morningup.core.designsystem.component.MorningBottomNavHeight
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningSelectMark
import com.lumian.morningup.core.designsystem.component.MorningSnackbar
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.RollingText
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
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
    onSelectingChange: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.selecting) { onSelectingChange(state.selecting) }
    BackHandler(enabled = state.selecting, onBack = viewModel::endSelection)
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
        onLongPress = { viewModel.startSelection(it) },
        onCancelDelete = viewModel::cancelDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onUndo = viewModel::undoDelete,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
        banner = banner,
        extra = HomeExtraActions(
            onRestOnce = viewModel::restOnce,
            onAllOff = viewModel::turnAllOff,
            onStartSelection = { viewModel.startSelection() },
            onToggleSelected = viewModel::toggleSelected,
            onSelectAll = viewModel::toggleSelectAll,
            onEndSelection = viewModel::endSelection,
            onTurnOnSelected = viewModel::turnOnSelected,
            onTurnOffSelected = viewModel::turnOffSelected,
            onCopySelected = viewModel::copySelected,
            onDeleteSelected = viewModel::deleteSelected,
        ),
    )
}

/** H3 더보기와 선택 모드, H8 이번만 쉬기 콜백. */
data class HomeExtraActions(
    val onRestOnce: (Alarm) -> Unit = {},
    val onAllOff: () -> Unit = {},
    val onStartSelection: () -> Unit = {},
    val onToggleSelected: (Alarm) -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onEndSelection: () -> Unit = {},
    val onTurnOnSelected: () -> Unit = {},
    val onTurnOffSelected: () -> Unit = {},
    val onCopySelected: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
)

/** H0 홈, H1 빈 홈, H3 더보기와 선택 모드, H5 삭제 확인, H6 저장 토스트, H7 삭제 스낵바, H8 이번만 쉬기. */
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
    banner: (@Composable () -> Unit)? = null,
    extra: HomeExtraActions = HomeExtraActions(),
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
                    onOpen = onOpen,
                    onToggle = onToggle,
                    onLongPress = onLongPress,
                    extra = extra,
                    bottomReserve = navReserve,
                    banner = banner.takeUnless { state.selecting },
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
        if (state.selecting) {
            SelectionBar(
                state = state,
                extra = extra,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = MorningDimens.space16),
            )
        }
        if (state.pendingDelete.isNotEmpty()) {
            DeleteDialog(alarms = state.pendingDelete, onCancel = onCancelDelete, onConfirm = onConfirmDelete)
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
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onLongPress: (Alarm) -> Unit,
    extra: HomeExtraActions,
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
        val selection = state.selection
        if (selection == null) {
            item(key = "header") { Column { Header(headline, subline, banner = banner) } }
            item(key = "actions") { ListActions(onAdd = onAdd, extra = extra) }
        } else {
            item(key = "selectHeader") { SelectHeader(count = selection.size) }
            item(key = "selectActions") {
                SelectActions(
                    allSelected = selection.size == state.alarms.size,
                    onSelectAll = extra.onSelectAll,
                    onCancel = extra.onEndSelection,
                )
            }
        }
        items(state.alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                look = CardLook(
                    shownOn = state.shownOn(alarm),
                    restOffer = selection == null && state.showsRestOffer(alarm),
                    restingDay = state.resting[alarm.id].takeIf { selection == null },
                    selected = selection?.contains(alarm.id),
                ),
                onClick = { if (selection != null) extra.onToggleSelected(alarm) else onOpen(alarm) },
                onLongPress = { if (selection != null) extra.onToggleSelected(alarm) else onLongPress(alarm) },
                onToggle = { onToggle(alarm, it) },
                onRestOnce = { extra.onRestOnce(alarm) },
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

/** 카드 모양: 켜진 모양인지, '내일 다시 켜기'(Rest Offer), 쉬는 중(Resting), 선택 모드면 골랐는지. */
private data class CardLook(
    val shownOn: Boolean,
    val restOffer: Boolean = false,
    val restingDay: DayOfWeek? = null,
    /** null이면 선택 모드가 아니에요. */
    val selected: Boolean? = null,
)

/**
 * 피그마 Alarm Card(State On, Off, Rest Offer, Resting × Mode Default, Select). 시각, 월~일 요일(고른 요일만 파랑),
 * 오른쪽 토글. 선택 모드면 토글 대신 앞에 Select Mark. 길게 누르면 선택 모드(H3-1)로 들어가요.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    look: CardLook,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestOnce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val res = alarmResources()
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val time = AlarmText.time(res, alarm.time)
    val description = stringResource(R.string.home_card_description, time, AlarmText.repeat(res, alarm.repeatDays))
    Column(
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (look.selected != null) {
                MorningSelectMark(selected = look.selected)
                Spacer(Modifier.width(MorningDimens.space16))
            }
            Column(Modifier.weight(1f)) {
                look.restingDay?.let { day ->
                    Text(
                        text = stringResource(R.string.home_resting, AlarmText.dayShort(res, day)),
                        style = MorningTheme.typography.captionStrong,
                        color = colors.statusError,
                    )
                    Spacer(Modifier.height(MorningDimens.space4))
                }
                Column(
                    modifier = Modifier
                        .alpha(if (look.shownOn) 1f else OFF_ALPHA)
                        .clearAndSetSemantics { contentDescription = description },
                ) {
                    Text(text = time, style = MorningTheme.typography.headlineNumber, color = colors.textPrimary)
                    Spacer(Modifier.height(MorningDimens.space4))
                    DaysRow(alarm.repeatDays)
                }
            }
            if (look.selected == null) {
                MorningToggle(
                    checked = look.shownOn,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = time },
                )
            }
        }
        if (look.restOffer) {
            Spacer(Modifier.height(MorningDimens.space16))
            RestOfferButton(onClick = onRestOnce)
        }
    }
}

private val RestButtonHeight = 36.dp
private val RestButtonRadius = 12.dp

/** H8 Rest Offer 버튼: 높이 36, 모서리 12, `primary/subtle` 바탕, `Body/Small Strong` `primary/text` 글자. */
@Composable
private fun RestOfferButton(onClick: () -> Unit) {
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(RestButtonRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RestButtonHeight)
            .background(colors.primarySubtle, shape)
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_rest_offer),
            style = MorningTheme.typography.bodySmallStrong,
            color = colors.primaryText,
        )
    }
}

/** 목록 위 오른쪽 플러스와 더보기(H3 메뉴: '모든 알람 끄기', '선택'). */
@Composable
private fun ListActions(onAdd: () -> Unit, extra: HomeExtraActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MorningDimens.space4),
        horizontalArrangement = Arrangement.End,
    ) {
        MorningIconButton(Lucide.Plus, stringResource(R.string.home_add), onAdd)
        Box {
            MorningIconButton(Lucide.EllipsisVertical, stringResource(R.string.home_more), { menuOpen = true })
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(MorningDimens.radiusCard),
                containerColor = MorningTheme.colors.surfaceCard,
            ) {
                MenuRow(Lucide.BellOff, stringResource(R.string.home_menu_all_off)) {
                    menuOpen = false
                    extra.onAllOff()
                }
                MenuRow(Lucide.ListChecks, stringResource(R.string.home_menu_select)) {
                    menuOpen = false
                    extra.onStartSelection()
                }
            }
        }
    }
}

private val MenuWidth = 200.dp

/** 피그마 Menu Item: 아이콘 20, `Body/Medium` 글자, 사이 12. */
@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .width(MenuWidth)
            .morningClickable(haptic = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Icon(icon, null, Modifier.size(MorningDimens.iconMd), tint = colors.textSecondary)
        Text(label, style = MorningTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

/** 선택 모드 헤더 높이(상태 표시줄 아래, 홈 Next Alarm Header와 같은 자리, Type=Select). */
private val SelectHeaderHeight = 170.dp

/** H3-1 하늘 위 '○개 선택됨'. 홈 헤더와 같은 높이 안 세로 가운데. */
@Composable
private fun SelectHeader(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(SelectHeaderHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_selected_count, count),
            style = MorningTheme.typography.headlineLarge,
            color = MorningTheme.colors.textPrimary,
        )
    }
}

private val SelectRowHeight = 48.dp

/** '전체 선택'(Select Mark + 글자, 사이 8)과 오른쪽 '취소'. */
@Composable
private fun SelectActions(allSelected: Boolean, onSelectAll: () -> Unit, onCancel: () -> Unit) {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SelectRowHeight)
            .padding(horizontal = MorningDimens.layoutScreenMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.morningClickable(
                haptic = MorningHaptic.Tick,
                role = Role.Checkbox,
                onClick = onSelectAll,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
        ) {
            MorningSelectMark(selected = allSelected)
            Text(
                text = stringResource(R.string.home_select_all),
                style = MorningTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.home_select_cancel),
            style = MorningTheme.typography.bodyMediumStrong,
            color = colors.textPrimary,
            modifier = Modifier.morningClickable(haptic = null, role = Role.Button, onClick = onCancel),
        )
    }
}

private val ActionItemWidth = 56.dp
private val ActionItemHeight = 48.dp
private const val DISABLED_ALPHA = 0.4f

/**
 * H3-1 하단 바: 하단 메뉴와 같은 떠 있는 알약에 켜기, 끄기, 복사, 삭제(칸 56x48, 사이 4, 안 여백 8).
 * 켜진 것과 꺼진 것이 섞이면 켜기와 끄기 둘 다, 모두 꺼져 있으면 켜기만, 모두 켜져 있으면 끄기만.
 */
@Composable
private fun SelectionBar(state: HomeUiState, extra: HomeExtraActions, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val chosen = state.alarms.filter { state.selection?.contains(it.id) == true }
    val any = chosen.isNotEmpty()
    val pill = RoundedCornerShape(MorningDimens.radiusFull)
    Row(
        modifier = modifier
            .morningShadow(MorningShadows.float, pill)
            .background(colors.surfaceCard, pill)
            .padding(MorningDimens.space8),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space4),
    ) {
        if (chosen.any { !state.shownOn(it) }) {
            ActionItem(Lucide.AlarmClock, stringResource(R.string.home_action_on), true, extra.onTurnOnSelected)
        }
        if (chosen.any { state.shownOn(it) }) {
            ActionItem(Lucide.AlarmClockOff, stringResource(R.string.home_action_off), true, extra.onTurnOffSelected)
        }
        ActionItem(Lucide.Copy, stringResource(R.string.home_action_copy), any, extra.onCopySelected)
        ActionItem(Lucide.Trash2, stringResource(R.string.home_action_delete), any, extra.onDeleteSelected)
    }
}

/** 피그마 Action Bar Item: 아이콘 20 위에 `Caption/Medium` 글자. */
@Composable
private fun ActionItem(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    Column(
        modifier = Modifier
            .size(ActionItemWidth, ActionItemHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.Button, enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(MorningDimens.iconMd), tint = colors.textPrimary)
        Text(label, style = MorningTheme.typography.captionMedium, color = colors.textSecondary)
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
            is HomeMessage.Copied -> MorningToast(text = stringResource(R.string.home_copied, shown.count))
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
private fun DeleteDialog(alarms: List<Alarm>, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val res = alarmResources()
    val alarm = alarms.first()
    val time = AlarmText.time(res, alarm.time)
    val repeat = AlarmText.repeat(res, alarm.repeatDays)
    val single = alarms.size == 1
    MorningDialog(
        title = if (single) {
            stringResource(R.string.home_delete_title)
        } else {
            stringResource(R.string.home_delete_many_title, alarms.size)
        },
        message = when {
            !single -> stringResource(R.string.home_delete_many_message)
            alarm.missions.isEmpty() -> stringResource(R.string.home_delete_message, time, repeat)
            else -> stringResource(R.string.home_delete_message_missions, time, repeat)
        },
        dismissLabel = stringResource(R.string.home_delete_cancel),
        confirmLabel = stringResource(R.string.home_delete_confirm),
        onDismiss = onCancel,
        onConfirm = onConfirm,
        danger = true,
    )
}

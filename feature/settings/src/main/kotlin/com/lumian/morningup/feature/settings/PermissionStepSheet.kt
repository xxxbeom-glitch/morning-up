package com.lumian.morningup.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.designsystem.component.MorningBlockingSheet
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonStyle
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningSheetTextButton
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.MorningToggleSize
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.PermissionStep
import com.lumian.morningup.core.model.allGranted
import com.lumian.morningup.core.model.openingStep
import com.lumian.morningup.core.model.stepAfterRequired
import java.io.Serializable
import kotlinx.coroutines.delay

/** 피그마 Permission List Row(729:7078): 칸 모서리 16, 아이콘 원 40, 이름과 설명 사이 2. */
private val RowCorner = 16.dp
private val RowIconCircle = 40.dp
private val RowTextGap = 2.dp

/** 피그마 PL2-3 battery note(765:13978): 모서리 12, '※'와 글 사이 4. */
private val NoteCorner = 12.dp
private val NoteMarkGap = 4.dp

/** 토스트는 시트 '다음'/'완료' 버튼 위에 떠요(보관 PL-T1 `737:12181`: 화면 아래 시스템 바 48 위로 72). */
private val ToastAboveButton = 72.dp

/** 2단계는 버튼 아래 '다음에 하기' 줄(44)과 사이(4)가 더 있어 버튼 위 8을 지키려면 120이에요(피그마 PL2-3 Bottom CTA 128). */
private val ToastAboveDoneButton = 120.dp

/** 토스트는 3초 보여요(결정 128). */
private const val TOAST_MILLIS = 3_000L

/** PL2 시트 안 토스트 3종(결정 127 보충 4). */
private sealed interface StepToast {
    data class NotYet(val permission: AlarmPermission) : StepToast

    data class TurnedOff(val permission: AlarmPermission) : StepToast

    data object CannotOpen : StepToast
}

/** 줄을 눌러 폰 설정에 다녀오는 중인 권한과, 누를 때 켜져 있었는지. */
private data class Pending(val permission: AlarmPermission, val wasGranted: Boolean) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 나중용 권한 시트 PL2(결정 129 보충 5, 132. 피그마 PL2-1~PL2-4 `765:13473` 등, 다크 `817:11863` 줄).
 * 홈 H2 배너나 배터리 전날 밤 알림을 누르면 홈 위에 떠요. [requested] 단계로 열지만, 필수가 꺼져 있으면 1단계부터,
 * 필수가 다 켜져 있으면 2단계부터예요. 1단계 '다음'은 필수 셋이 켜져야 눌리고, 2단계 '완료'는 언제나 눌려요.
 * 폰 뒤로 가기로만 닫히고(2단계는 '완료', '다음에 하기'로도), 막을 누르거나 끌어 내려도 닫히지 않아요.
 * 줄을 누르면 그 권한의 폰 설정이나 시스템 창으로 가고, 돌아오면 토글이 실제 상태로 바뀌어요.
 */
@Composable
fun PermissionStepSheet(requested: PermissionStep, onClose: () -> Unit, viewModel: SettingsViewModel) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(report.openingStep(requested)) }
    val current = step
    if (current == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val open = rememberPermissionOpener(viewModel)
    // 위치, 알림처럼 실행 중 권한을 폰 설정에서 끄면 안드로이드가 앱을 다시 띄워요. 그래도 '꺼졌어요'를 알리도록 저장해요.
    var pending by rememberSaveable { mutableStateOf<Pending?>(null) }
    var toast by remember { mutableStateOf<StepToast?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(TOAST_MILLIS)
            toast = null
        }
    }
    // 폰 설정이나 시스템 창에서 돌아오면 다시 확인하고, 안 켜고 왔거나 껐으면 토스트로 알려요.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestPending by rememberUpdatedState(pending)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            val back = latestPending
            if (event == Lifecycle.Event.ON_RESUME && back != null) {
                viewModel.refresh()
                pending = null
                val granted = viewModel.report.value.isGranted(back.permission)
                toast = when {
                    granted -> null
                    back.wasGranted -> StepToast.TurnedOff(back.permission)
                    else -> StepToast.NotYet(back.permission)
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    MorningBlockingSheet(
        onBack = onClose,
        tag = PERMISSION_STEP_SHEET_TAG,
        front = { StepToastHost(toast, current) },
    ) {
        PermissionStepContent(
            step = current,
            report = report,
            onToggle = { permission ->
                val wasGranted = report.isGranted(permission)
                if (open(permission)) {
                    pending = Pending(permission, wasGranted)
                } else {
                    toast = StepToast.CannotOpen
                }
            },
            onNext = {
                val next = viewModel.report.value.stepAfterRequired()
                if (next == null) onClose() else step = next
            },
            onDone = onClose,
        )
    }
}

@Composable
private fun BoxScope.StepToastHost(toast: StepToast?, step: PermissionStep) {
    MorningMessageHost(
        message = toast,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = if (step == PermissionStep.Optional) ToastAboveDoneButton else ToastAboveButton)
            .padding(horizontal = MorningDimens.layoutScreenMargin),
    ) { shown ->
        val text = when (shown) {
            is StepToast.NotYet -> stringResource(R.string.pl_toast_not_yet, stringResource(subject(shown.permission)))
            is StepToast.TurnedOff ->
                stringResource(R.string.pl_toast_turned_off, stringResource(subject(shown.permission)))
            StepToast.CannotOpen -> stringResource(R.string.pl_toast_cannot_open)
        }
        MorningToast(text)
    }
}

/** 토스트 앞에 붙는 권한 이름과 조사('알림'이, '다른 앱 위에 표시'가). */
private fun subject(permission: AlarmPermission): Int = when (permission) {
    AlarmPermission.Notifications -> R.string.pl_subject_notifications
    AlarmPermission.FullScreen -> R.string.pl_subject_full_screen
    AlarmPermission.Overlay -> R.string.pl_subject_overlay
    AlarmPermission.Location -> R.string.pl_subject_location
    else -> R.string.pl_subject_battery
}

/** PL2 줄 이름과 설명(결정 127 시트 문구). 아이콘과 원 색은 설정 '권한' 화면과 같아요. */
private fun rowTexts(permission: AlarmPermission): Pair<Int, Int> = when (permission) {
    AlarmPermission.Notifications -> R.string.permission_notifications to R.string.pl_notifications_desc
    AlarmPermission.FullScreen -> R.string.permission_full_screen to R.string.pl_full_screen_desc
    AlarmPermission.Overlay -> R.string.permission_overlay to R.string.pl_overlay_desc
    AlarmPermission.Location -> R.string.permission_location to R.string.pl_location_desc
    else -> R.string.pl_battery to R.string.pl_battery_desc
}

/**
 * PL2 시트 안 내용. 두 단계는 같은 시트 안에서 내용만 바뀌어요: 시트 높이가 내용에 맞춰 바뀌고 내용은 왼쪽으로 밀려요(피그마 메모 `765:14308`).
 * 위 32, 헤드(`Title/Large` 가운데), 목록(위 24, 양옆 16, 줄 사이 8), 아래 버튼 칸(위 16, 양옆 16, 아래 8).
 */
@Composable
internal fun PermissionStepContent(
    step: PermissionStep,
    report: PermissionReport,
    onToggle: (AlarmPermission) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
    val reduceMotion = MorningTheme.reduceMotion
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val slide = MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion)
            (slideInHorizontally(slide) { it } + fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)))
                .togetherWith(
                    slideOutHorizontally(slide) { -it } + fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault)),
                )
                .using(
                    SizeTransform(clip = true) { _, _ ->
                        MorningMotion.spatial<IntSize>(MorningMotion.SpatialDefault, reduceMotion)
                    },
                )
        },
        label = "permissionStep",
    ) { shown ->
        Column(Modifier.fillMaxWidth().padding(top = MorningDimens.space32)) {
            Text(
                text = stringResource(
                    if (shown == PermissionStep.Required) R.string.pl_required_title else R.string.pl_optional_title,
                ),
                style = MorningTheme.typography.titleLarge,
                color = MorningTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MorningDimens.space24)
                    .semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = MorningDimens.space16, end = MorningDimens.space16, top = MorningDimens.space24),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
            ) {
                shown.permissions.forEach { permission ->
                    StepRow(permission, report.isGranted(permission)) { onToggle(permission) }
                }
                if (shown == PermissionStep.Optional) BatteryNote()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MorningDimens.space16,
                        end = MorningDimens.space16,
                        top = MorningDimens.space16,
                        bottom = MorningDimens.space8,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space4),
            ) {
                if (shown == PermissionStep.Required) {
                    val ready = report.allGranted(PermissionStep.Required)
                    MorningButton(
                        text = stringResource(R.string.pl_next),
                        onClick = onNext,
                        enabled = ready,
                        modifier = Modifier.testTag(PL_NEXT_TAG),
                        style = if (ready) MorningButtonStyle.Primary else MorningButtonStyle.Muted,
                    )
                } else {
                    MorningButton(
                        text = stringResource(R.string.pl_done),
                        onClick = onDone,
                        modifier = Modifier.testTag(PL_DONE_TAG),
                    )
                    MorningSheetTextButton(
                        text = stringResource(R.string.pl_later),
                        onClick = onDone,
                    )
                }
            }
        }
    }
}

/** 피그마 Permission List Row(토글 형): `background/base` 칸, 안쪽 16, 원 40 + 이름, 설명 + 작은 토글. */
@Composable
private fun StepRow(permission: AlarmPermission, granted: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    val look = spec(permission)
    val (title, description) = rowTexts(permission)
    val state = stringResource(if (granted) R.string.permission_granted else R.string.permission_not_granted)
    val shape = RoundedCornerShape(RowCorner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundBase, shape)
            .morningClickable(haptic = null, onClick = onClick)
            .semantics { stateDescription = state }
            .padding(MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Box(
            modifier = Modifier
                .size(RowIconCircle)
                .background(look.tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icon = look.icon
            if (icon is RowIcon.Drawn) Image(painterResource(icon.res), contentDescription = null)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RowTextGap)) {
            Text(stringResource(title), style = MorningTheme.typography.bodyMediumStrong, color = colors.textPrimary)
            Text(stringResource(description), style = MorningTheme.typography.bodySmall, color = colors.textSecondary)
        }
        MorningToggle(checked = granted, onCheckedChange = { onClick() }, size = MorningToggleSize.Small)
    }
}

/** 배터리 줄 아래 안내문(피그마 battery note `765:13978`): `background/base` 칸 안 '※'와 설명, `Body/Small` `text/tertiary`. */
@Composable
private fun BatteryNote() {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundBase, RoundedCornerShape(NoteCorner))
            .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space12),
        horizontalArrangement = Arrangement.spacedBy(NoteMarkGap),
    ) {
        Text(
            stringResource(R.string.pl_battery_note_mark),
            style = MorningTheme.typography.bodySmall,
            color = colors.textTertiary,
        )
        Text(
            stringResource(R.string.pl_battery_note),
            style = MorningTheme.typography.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

const val PERMISSION_STEP_SHEET_TAG = "permissionStepSheet"
internal const val PL_NEXT_TAG = "plNext"
internal const val PL_DONE_TAG = "plDone"

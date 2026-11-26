package com.lumian.morningup.feature.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources

/** 이름표(테스트용). 채운 칸 `"$MISSION_SLOT_TAG${type.name}"`, 빈 칸 `"${MISSION_SLOT_TAG}Empty$index"`. */
const val MISSION_SLOT_TAG = "missionSlot"
const val MISSION_REMOVE_TAG = "missionRemove"
const val MISSION_PICK_ROW_TAG = "missionPickRow"

/**
 * Remove Badge: 칸 안쪽 오른쪽 위(위 4, 오른쪽 4), 20 원, X 12. 누르는 자리는 28로 조금 넓혀요.
 * 어떤 그림 위에서도 보이게 진한 원(`RemoveBadgeFill`) + 흰 둘레 1.5 + 흰 Lucide x예요(11-19 사용자 지적 6).
 */
private val BadgeSize = 20.dp
private val BadgeTouch = 28.dp
private val BadgeIcon = 12.dp
private val BadgeRing = 1.5.dp
private val AddIcon = 24.dp
private val PickThumb = 48.dp
private val ChevronSize = 20.dp
private const val DISABLED_ALPHA = 0.45f

/**
 * 편집 S0 '기상 미션' 카드의 미션 칸 4개(결정 87, 95). 줄 너비를 4칸으로 꽉 차게 나눈 정사각형, 사이 8, 테두리 없음.
 * 채운 칸은 미션 그림만 칸을 꽉 채우고(글자 없음) 누르면 그 미션 설정 시트, 오른쪽 위 X는 확인 없이 바로 빼요.
 * 빈 칸은 `background/raised` 위에 + 아이콘, 누르면 S1-1 미션 추가 시트가 열려요.
 */
@Composable
internal fun MissionSlots(
    missions: List<MissionSetting>,
    onOpen: (MissionType) -> Unit,
    onRemove: (MissionType) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
    ) {
        repeat(MissionSetting.MAX_MISSIONS) { index ->
            val setting = missions.getOrNull(index)
            val slotModifier = Modifier.weight(1f).aspectRatio(1f)
            if (setting == null) {
                EmptySlot(index = index, onClick = onAdd, modifier = slotModifier)
            } else {
                FilledSlot(
                    type = setting.type,
                    onClick = { onOpen(setting.type) },
                    onRemove = { onRemove(setting.type) },
                    modifier = slotModifier,
                )
            }
        }
    }
}

@Composable
private fun FilledSlot(type: MissionType, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier) {
    val name = AlarmText.missionName(alarmResources(), type)
    Box(modifier = modifier) {
        Image(
            painter = painterResource(thumbOf(type)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(MissionThumbShape)
                .testTag("$MISSION_SLOT_TAG${type.name}")
                .semantics { contentDescription = name }
                .morningClickable(role = Role.Button, onClick = onClick),
        )
        val removeLabel = stringResource(R.string.edit_mission_remove, name)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(BadgeTouch)
                .testTag("$MISSION_REMOVE_TAG${type.name}")
                .semantics { contentDescription = removeLabel }
                .morningClickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(BadgeSize)
                    .background(MorningFixedColors.RemoveBadgeFill, CircleShape)
                    .border(BadgeRing, MorningFixedColors.RemoveBadgeRing, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(BadgeIcon),
                )
            }
        }
    }
}

@Composable
private fun EmptySlot(index: Int, onClick: () -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.edit_mission_add_title)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MorningDimens.radiusTile))
            .background(MorningTheme.colors.backgroundRaised)
            .testTag("${MISSION_SLOT_TAG}Empty$index")
            .semantics { contentDescription = label }
            .morningClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Plus,
            contentDescription = null,
            tint = MorningTheme.colors.textTertiary,
            modifier = Modifier.size(AddIcon),
        )
    }
}

/**
 * S1-1 미션 추가 시트(피그마 `522:396`, 결정 100). 머리 아래 미션 4줄: 그림 48, 이름과 설명, 오른쪽 >.
 * 아래 버튼은 없어요. 줄을 누르면 이 시트를 닫고 그 미션 설정 시트를 열어요.
 * 이미 넣은 미션과 흔들기 센서가 없을 때 Shake Shake는 흐리게 두고 누를 수 없어요.
 */
@Composable
internal fun MissionPickSheetContent(state: EditUiState, onPick: (MissionType) -> Unit) {
    MorningSheetHeader(
        title = stringResource(R.string.edit_mission_add_title),
        description = stringResource(R.string.edit_mission_add_desc),
        modifier = Modifier.padding(top = MorningDimens.space8, bottom = MorningDimens.space8),
    )
    Column {
        EditUiState.MISSION_LIST_ORDER.forEach { type ->
            val enabled = type !in state.draft.missionTypes && state.canOpen(type)
            MissionPickRow(type = type, enabled = enabled, onClick = { onPick(type) })
        }
    }
    Spacer(Modifier.height(MorningDimens.space8))
}

@Composable
private fun MissionPickRow(type: MissionType, enabled: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$MISSION_PICK_ROW_TAG${type.name}")
            .morningClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(vertical = MorningDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(thumbOf(type)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(PickThumb).clip(MissionThumbShape),
        )
        Spacer(Modifier.width(MorningDimens.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AlarmText.missionName(alarmResources(), type),
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(descriptionOf(type)),
                style = MorningTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(ChevronSize),
        )
    }
}

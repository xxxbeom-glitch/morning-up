package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.TrickSpeed

const val LAB_TRICK_SPEED_TAG = "lab_trick_speed"

/** 칸에 넣을 수 있는 자리 수(5000까지). */
private const val MAX_DIGITS = 4

/**
 * T3-1 버튼 속이기 빠르기 숫자 칸(결정 181, 결정 174의 슬라이더를 바꿈). 임시 모양이라 새로 디자인하지 않고
 * 설정 줄과 같은 글자, 회색 칸으로 둬요. 버튼이 끝에서 끝까지 가는 ms를 넣고, 작을수록 빨라요.
 * 숫자를 칠 때마다 범위 안으로 맞춘 값을 바로 저장하고, 완료나 칸을 떠날 때 칸 글자도 맞춘 값으로 바꿔요.
 */
@Composable
internal fun TrickSpeedInput(speed: TrickSpeed, onSpeed: (TrickSpeed) -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    var text by remember { mutableStateOf(speed.key) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(speed) { if (!focused) text = speed.key }
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.settings_lab_speed_label)
    fun settle() {
        text = speed.key
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        Text(text = label, style = typography.bodyMediumStrong, color = colors.textPrimary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MorningDimens.sizeRow)
                .background(colors.backgroundBase, RoundedCornerShape(MorningDimens.radiusTile))
                .padding(horizontal = MorningDimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { typed ->
                    val digits = typed.filter(Char::isDigit).take(MAX_DIGITS)
                    text = digits
                    digits.toIntOrNull()?.let { ms ->
                        val next = TrickSpeed.ofMillis(ms)
                        if (next != speed) onSpeed(next)
                    }
                },
                singleLine = true,
                textStyle = typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primaryDefault),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    settle()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        if (focused && !it.isFocused) settle()
                        focused = it.isFocused
                    }
                    .testTag(LAB_TRICK_SPEED_TAG)
                    .semantics { contentDescription = label },
            )
            Text(
                text = stringResource(R.string.settings_lab_speed_unit),
                style = typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.settings_lab_speed_desc, TrickSpeed.MIN_MILLIS, TrickSpeed.MAX_MILLIS),
            style = typography.bodyMedium,
            color = colors.textBody,
            modifier = Modifier.padding(top = MorningDimens.space4),
        )
    }
}

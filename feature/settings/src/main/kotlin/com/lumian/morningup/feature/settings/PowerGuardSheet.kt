package com.lumian.morningup.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lumian.morningup.core.designsystem.component.MorningDialogSheet

/**
 * P6 전원 종료 방지 바텀시트(피그마 `402:11379`, 다크 `812:8693`, 결정 134). 접근성 권한 동의와 켜는 안내를 한 장에 담아요.
 * '동의하고 설정 열기'는 폰 접근성 설정으로 바로 가고, '나중에'와 뒤로 가기는 닫아요. 그림 칸은 아직 비워 둬요.
 */
@Composable
fun PowerGuardSheet(onAgree: () -> Unit, onLater: () -> Unit) {
    MorningDialogSheet(
        title = stringResource(R.string.power_sheet_title),
        message = stringResource(R.string.power_sheet_msg),
        confirmLabel = stringResource(R.string.power_sheet_agree),
        onConfirm = onAgree,
        laterLabel = stringResource(R.string.ask_later),
        onLater = onLater,
        tag = POWER_GUARD_SHEET_TAG,
    )
}

internal const val POWER_GUARD_SHEET_TAG = "powerGuardSheet"

package com.lumian.morningup.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val ChevronSize = 16.dp
private const val OPEN_ROTATION = 180f

private const val APACHE_2 = "https://www.apache.org/licenses/LICENSE-2.0"

/**
 * 앱에 넣은 오픈소스: 이름, 줄 오른쪽 라이선스 값, 누르면 여는 라이선스 전문 주소.
 * 새로 가져오면 여기에 한 줄 더해요(dev-plan '오픈소스는 T5에 바로 적어요').
 */
private val Licenses = listOf(
    Triple("Pretendard", "OFL 1.1", "https://github.com/orioncactus/pretendard/blob/main/LICENSE"),
    Triple("Lucide Icons", "ISC", "https://lucide.dev/license"),
    Triple("AndroidX", "Apache 2.0", APACHE_2),
    Triple("Jetpack Compose", "Apache 2.0", APACHE_2),
    Triple("Dagger Hilt", "Apache 2.0", APACHE_2),
    Triple("Kotlin", "Apache 2.0", APACHE_2),
    Triple("Timber", "Apache 2.0", APACHE_2),
)

/** 공통 틀: 회색 바탕, Top Bar Back, 아래로 넘기는 카드 묶음(카드형 규칙). */
@Composable
private fun InfoFrame(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MorningTopBar(title = title, onBack = onBack, backContentDescription = stringResource(R.string.settings_back))
        Column(
            modifier = Modifier.padding(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space16,
                bottom = MorningDimens.space32,
            ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) { content() }
    }
}

/**
 * T5 오픈소스 라이선스(피그마 `141:1818`). 카드 하나에 제목과 설명 한 줄, 그 아래 Nav Row로 이름과 라이선스 값을
 * 적어요. 줄을 누르면 그 라이선스 전문을 브라우저로 열어요.
 */
@Composable
fun LicensesPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val colors = MorningTheme.colors
    InfoFrame(stringResource(R.string.settings_licenses), onBack, modifier) {
        // 제목 없는 카드(위아래 16)에 머리말 위아래 8을 더해 피그마처럼 위 24에서 제목이 시작하고 머리말 높이가 54예요.
        MorningSettingsCard(title = null) {
            Column(
                Modifier
                    .padding(vertical = MorningDimens.space8)
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                Text(
                    stringResource(R.string.licenses_card),
                    style = MorningTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(MorningDimens.space4))
                Text(
                    stringResource(R.string.licenses_desc),
                    style = MorningTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            NavRows {
                Licenses.forEach { (name, license, url) ->
                    NavRow(label = name, value = license, onClick = { runCatching { uriHandler.openUri(url) } })
                }
            }
        }
    }
}

/** 자주 묻는 질문. 앱 안에서 접었다 펴는 목록이에요(design-system T1). */
@Composable
fun FaqPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        R.string.faq_q1 to R.string.faq_a1,
        R.string.faq_q2 to R.string.faq_a2,
        R.string.faq_q3 to R.string.faq_a3,
        R.string.faq_q4 to R.string.faq_a4,
        R.string.faq_q5 to R.string.faq_a5,
        R.string.faq_q6 to R.string.faq_a6,
        R.string.faq_q7 to R.string.faq_a7,
    )
    InfoFrame(stringResource(R.string.settings_faq), onBack, modifier) {
        MorningSettingsCard(title = stringResource(R.string.faq_card)) {
            Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
                items.forEach { (q, a) -> Fold(title = stringResource(q), body = stringResource(a)) }
            }
        }
    }
}

@Composable
private fun Fold(title: String, body: String) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val colors = MorningTheme.colors
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .morningClickable(haptic = null, role = Role.Button) { open = !open }
                .padding(vertical = MorningDimens.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Lucide.ChevronDown,
                null,
                Modifier.size(ChevronSize).rotate(if (open) OPEN_ROTATION else 0f),
                tint = colors.textTertiary,
            )
        }
        if (open) {
            Text(
                text = body,
                style = MorningTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = MorningDimens.space12),
            )
        }
    }
}

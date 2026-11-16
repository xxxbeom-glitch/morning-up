package com.lumian.morningup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lumian.morningup.core.designsystem.R

/** Pretendard (SIL OFL 1.1). 앱에 4굵기만 넣었어요. */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

internal fun FontWeightToken.toFontWeight(): FontWeight = when (this) {
    FontWeightToken.Regular -> FontWeight.Normal
    FontWeightToken.Medium -> FontWeight.Medium
    FontWeightToken.SemiBold -> FontWeight.SemiBold
    FontWeightToken.Bold -> FontWeight.Bold
}

internal fun textStyle(name: String): TextStyle {
    val token = TextStyleTokens.getValue(name)
    return TextStyle(
        fontFamily = Pretendard,
        fontWeight = token.weight.toFontWeight(),
        fontSize = token.size.sp,
        lineHeight = token.lineHeight.sp,
        letterSpacing = (token.letterSpacingPercent / PERCENT).em,
        // 피그마처럼 줄 높이 안에서 글자를 가운데에 두고 위아래 여백을 자르지 않아요.
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}

private const val PERCENT = 100f

/** 피그마 글자 스타일 13개. 이름은 피그마 이름의 낙타 표기예요(`Body/Medium Strong` → `bodyMediumStrong`). */
@Immutable
data class MorningTypography(
    val displayLarge: TextStyle = textStyle("Display/Large"),
    val displayLarge12h: TextStyle = textStyle("Display/Large 12h"),
    val displayMedium: TextStyle = textStyle("Display/Medium"),
    val displayMediumMuted: TextStyle = textStyle("Display/Medium Muted"),
    val headlineNumber: TextStyle = textStyle("Headline/Number"),
    val headlineLarge: TextStyle = textStyle("Headline/Large"),
    val titleMedium: TextStyle = textStyle("Title/Medium"),
    val bodyMedium: TextStyle = textStyle("Body/Medium"),
    val bodyMediumStrong: TextStyle = textStyle("Body/Medium Strong"),
    val bodySmall: TextStyle = textStyle("Body/Small"),
    val bodySmallStrong: TextStyle = textStyle("Body/Small Strong"),
    val captionMedium: TextStyle = textStyle("Caption/Medium"),
    val captionStrong: TextStyle = textStyle("Caption/Strong"),
)

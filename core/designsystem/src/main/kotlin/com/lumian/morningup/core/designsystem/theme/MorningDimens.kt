package com.lumian.morningup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun dimen(name: String): Dp = DimensionTokens.getValue(name).dp

/** 피그마 `Dimension` 모음. 이름은 피그마 이름의 낙타 표기예요. */
@Immutable
object MorningDimens {
    val space4 = dimen("space/4")
    val space8 = dimen("space/8")
    val space12 = dimen("space/12")
    val space16 = dimen("space/16")
    val space20 = dimen("space/20")
    val space24 = dimen("space/24")
    val space32 = dimen("space/32")
    val space40 = dimen("space/40")
    val space48 = dimen("space/48")

    val layoutScreenMargin = dimen("layout/screen-margin")
    val layoutCardGap = dimen("layout/card-gap")
    val layoutCardPadding = dimen("layout/card-padding")
    val layoutHeaderTop = dimen("layout/header-top")

    val radiusCard = dimen("radius/card")
    val radiusTile = dimen("radius/tile")
    val radiusButton = dimen("radius/button")
    val radiusChip = dimen("radius/chip")
    val radiusCheckbox = dimen("radius/checkbox")
    val radiusFull = dimen("radius/full")

    val sizeRow = dimen("size/row")
    val sizeRowPlain = dimen("size/row-plain")
    val sizeButton = dimen("size/button")
    val sizeButtonLg = dimen("size/button-lg")
    val sizeButtonCircle = dimen("size/button-circle")
    val sizeChip = dimen("size/chip")
    val sizeIconBox = dimen("size/icon-box")
    val sizeCheckbox = dimen("size/checkbox")
    val sizeSwitchW = dimen("size/switch-w")
    val sizeSwitchH = dimen("size/switch-h")
    val sizeSwitchSmW = dimen("size/switch-sm-w")
    val sizeSwitchSmH = dimen("size/switch-sm-h")
    val sizeSwitchInset = dimen("size/switch-inset")

    val iconSm = dimen("icon/sm")
    val iconMd = dimen("icon/md")
    val iconLg = dimen("icon/lg")
}

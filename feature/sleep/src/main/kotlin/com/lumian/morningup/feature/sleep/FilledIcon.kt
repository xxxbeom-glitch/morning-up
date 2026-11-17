package com.lumian.morningup.feature.sleep

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath

/**
 * Lucide 선 아이콘을 채운 모양으로 바꿔요(Z2 재생, 일시정지. 11-16 결정: 이 둘만 채움).
 * 모양은 Lucide 그대로 두고, 선 안쪽을 같은 색으로 칠해요. 색은 Icon의 tint가 덮어써요.
 */
internal fun ImageVector.filled(): ImageVector {
    val builder = ImageVector.Builder(
        name = "${name}Filled",
        defaultWidth = defaultWidth,
        defaultHeight = defaultHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
    )
    fun addAll(group: VectorGroup) {
        group.forEach { node ->
            when (node) {
                is VectorPath -> builder.addPath(
                    pathData = node.pathData,
                    pathFillType = node.pathFillType,
                    fill = SolidColor(Color.Black),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = node.strokeLineWidth,
                    strokeLineCap = node.strokeLineCap,
                    strokeLineJoin = node.strokeLineJoin,
                )
                is VectorGroup -> addAll(node)
            }
        }
    }
    addAll(root)
    return builder.build()
}

package com.investly.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun builder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
)

private fun ImageVector.Builder.stroke(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeCap.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = block
    )
}

val IcHome: ImageVector = builder("IcHome").stroke {
    moveTo(3f, 11.5f)
    lineTo(12f, 3.5f)
    lineTo(21f, 11.5f)
    moveTo(5.5f, 10f)
    verticalLineTo(20f)
    horizontalLineTo(9.8f)
    verticalLineTo(15f)
    horizontalLineTo(14.2f)
    verticalLineTo(20f)
    horizontalLineTo(18.5f)
    verticalLineTo(10f)
}.build()

val IcTrend: ImageVector = builder("IcTrend").stroke {
    moveTo(3f, 17f)
    lineTo(9.5f, 10.5f)
    lineTo(13f, 14f)
    lineTo(21f, 6f)
    moveTo(15.5f, 6f)
    horizontalLineTo(21f)
    verticalLineTo(11.5f)
}.build()

val IcWallet: ImageVector = builder("IcWallet").stroke {
    moveTo(4f, 7.5f)
    curveTo(4f, 6f, 5f, 5f, 6.5f, 5f)
    horizontalLineTo(18f)
    verticalLineTo(7.5f)
    close()
    moveTo(3f, 7.5f)
    horizontalLineTo(21f)
    verticalLineTo(19f)
    horizontalLineTo(3f)
    close()
    moveTo(16f, 12.2f)
    quadToRelative(1.8f, -0.9f, 1.8f, 0.8f)
    quadToRelative(0f, 1.7f, -1.8f, 0.8f)
    quadToRelative(-1.8f, -0.9f, 0f, -1.6f)
    close()
}.build()

val IcSwap: ImageVector = builder("IcSwap").stroke {
    moveTo(20f, 7.5f)
    horizontalLineTo(4f)
    moveTo(16.5f, 4f)
    lineTo(20f, 7.5f)
    lineTo(16.5f, 11f)
    moveTo(4f, 16.5f)
    horizontalLineTo(20f)
    moveTo(7.5f, 13f)
    lineTo(4f, 16.5f)
    lineTo(7.5f, 20f)
}.build()

val IcPerson: ImageVector = builder("IcPerson").stroke {
    moveTo(12f, 4.5f)
    quadTo(15.5f, 4.5f, 15.5f, 8f)
    quadTo(15.5f, 11.5f, 12f, 11.5f)
    quadTo(8.5f, 11.5f, 8.5f, 8f)
    quadTo(8.5f, 4.5f, 12f, 4.5f)
    close()
    moveTo(4.5f, 20f)
    quadTo(4.5f, 13.5f, 12f, 13.5f)
    quadTo(19.5f, 13.5f, 19.5f, 20f)
    close()
}.build()

val IcBell: ImageVector = builder("IcBell").stroke {
    moveTo(12f, 3.5f)
    quadTo(8f, 3.5f, 8f, 8.5f)
    quadTo(8f, 12f, 6.5f, 14f)
    horizontalLineTo(17.5f)
    quadTo(16f, 12f, 16f, 8.5f)
    quadTo(16f, 3.5f, 12f, 3.5f)
    close()
    moveTo(10f, 17f)
    quadTo(10.5f, 19f, 12f, 19f)
    quadTo(13.5f, 19f, 14f, 17f)
}.build()

val IcArrowUpRight: ImageVector = builder("IcArrowUpRight").stroke {
    moveTo(6f, 18f)
    lineTo(18f, 6f)
    moveTo(9f, 6f)
    horizontalLineTo(18f)
    verticalLineTo(15f)
}.build()

val IcCopy: ImageVector = builder("IcCopy").stroke {
    moveTo(9f, 9f)
    horizontalLineTo(19f)
    verticalLineTo(19f)
    horizontalLineTo(9f)
    close()
    moveTo(5f, 15f)
    verticalLineTo(5f)
    horizontalLineTo(15f)
}.build()

val IcLogout: ImageVector = builder("IcLogout").stroke {
    moveTo(9f, 21f)
    horizontalLineTo(5f)
    verticalLineTo(3f)
    horizontalLineTo(9f)
    moveTo(15f, 17f)
    lineTo(21f, 12f)
    lineTo(15f, 7f)
    moveTo(21f, 12f)
    horizontalLineTo(9f)
}.build()

val IcBack: ImageVector = builder("IcBack").stroke {
    moveTo(15f, 5f)
    lineTo(8f, 12f)
    lineTo(15f, 19f)
}.build()

val IcCheckCircle: ImageVector = builder("IcCheckCircle").stroke {
    moveTo(12f, 3f)
    arcTo(9f, 9f, 0f, true, true, 3f, 12f)
    arcTo(9f, 9f, 0f, true, true, 12f, 3f)
    close()
    moveTo(8.5f, 12f)
    lineTo(11f, 14.5f)
    lineTo(15.5f, 9.5f)
}.build()

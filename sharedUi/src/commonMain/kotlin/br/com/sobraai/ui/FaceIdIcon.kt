package br.com.sobraai.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FaceIdIcon: ImageVector by lazy {
    ImageVector.Builder("Face ID", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8f, 3f); horizontalLineTo(5f); curveTo(4f, 3f, 3f, 4f, 3f, 5f); verticalLineTo(8f)
            moveTo(16f, 3f); horizontalLineTo(19f); curveTo(20f, 3f, 21f, 4f, 21f, 5f); verticalLineTo(8f)
            moveTo(3f, 16f); verticalLineTo(19f); curveTo(3f, 20f, 4f, 21f, 5f, 21f); horizontalLineTo(8f)
            moveTo(21f, 16f); verticalLineTo(19f); curveTo(21f, 20f, 20f, 21f, 19f, 21f); horizontalLineTo(16f)
            moveTo(8f, 8f); verticalLineTo(10f)
            moveTo(16f, 8f); verticalLineTo(10f)
            moveTo(12f, 8f); verticalLineTo(13f); horizontalLineTo(10.5f)
            moveTo(8f, 16f); curveTo(10f, 18f, 14f, 18f, 16f, 16f)
        }
    }.build()
}

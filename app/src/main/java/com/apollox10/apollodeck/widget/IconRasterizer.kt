package com.apollox10.apollodeck.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

// Glance renders through RemoteViews, which can't host live Compose content
// — Image(ImageProvider(bitmap)) is the supported way to show an icon there.
// Rasterizes by walking ImageVector's public node tree (VectorGroup/
// VectorPath) directly rather than via Composition, since there's no
// attached view to compose into at render time. Always fills with [tint],
// ignoring the vector's own color — every icon here is a single-color
// glyph drawn on a solid widget background, so this matches every caller.
fun ImageVector.toBitmap(sizePx: Int, tint: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint.toArgb()
        style = Paint.Style.FILL
    }
    canvas.scale(sizePx / viewportWidth, sizePx / viewportHeight)
    drawGroup(canvas, root, paint)
    return bitmap
}

private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint) {
    canvas.save()
    canvas.translate(group.translationX, group.translationY)
    canvas.translate(group.pivotX, group.pivotY)
    canvas.rotate(group.rotation)
    canvas.scale(group.scaleX, group.scaleY)
    canvas.translate(-group.pivotX, -group.pivotY)
    for (node: VectorNode in group) {
        when (node) {
            is VectorGroup -> drawGroup(canvas, node, paint)
            is VectorPath -> canvas.drawPath(node.pathData.toPath().asAndroidPath(), paint)
        }
    }
    canvas.restore()
}

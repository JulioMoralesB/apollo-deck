package com.apollox10.apollodeck.wear.tile

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
import androidx.wear.protolayout.ResourceBuilders
import java.nio.ByteBuffer

// Renders a Material icon into a protolayout inline image resource — a
// Tile's LayoutElement can only reference an image by resource id declared
// in onTileResourcesRequest, and there's no live Compose tree to draw an
// Icon() into at that point, so the vector is walked and rasterized by hand
// (mirrors the phone widget's IconRasterizer, which has the same
// RemoteViews constraint). InlineImageResource wants raw ARGB_8888 pixel
// bytes, not an encoded format like PNG — copyPixelsToBuffer() with
// IMAGE_FORMAT_ARGB_8888 is the documented pairing for a runtime-generated
// bitmap. Baked in white: a Tile's Button tints icon content from its own
// ButtonColors content color via a color filter, so the exact fill color
// baked in here doesn't survive — only the alpha shape does.
fun ImageVector.toInlineImageResource(sizePx: Int): ResourceBuilders.ImageResource {
    val bitmap = toIconBitmap(sizePx)
    val buffer = ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(buffer)
    return ResourceBuilders.ImageResource.Builder()
        .setInlineResource(
            ResourceBuilders.InlineImageResource.Builder()
                .setData(buffer.array())
                .setWidthPx(sizePx)
                .setHeightPx(sizePx)
                .setFormat(ResourceBuilders.IMAGE_FORMAT_ARGB_8888)
                .build(),
        )
        .build()
}

private fun ImageVector.toIconBitmap(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
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

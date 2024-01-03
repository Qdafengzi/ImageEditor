package com.example.mycamerax.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.res.painterResource
import com.example.mycamerax.R

@Composable
fun TransparentRectangleOverlay() {
    // 用于加载图片的示例，替换为你需要显示的图片
    val image = painterResource(R.mipmap.ic_editor)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 显示图片
        Image(painter = image, contentDescription = "Your Image")

        // 绘制遮罩和透明矩形
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1f


            val cropSize = size.width.coerceAtMost(size.height) * 0.6f
            val cropOffset = Offset((size.width - cropSize) / 2, (size.height - cropSize) / 2)
            val cropRectangle = Rect(
                offset = cropOffset,
                size = Size(cropSize, cropSize),
            )

            //裁剪区域
            val cropPath = Path().apply {
                addRect(cropRectangle)
            }

            val screenPath = Path().apply {
                addRect(Rect(Offset.Zero, size))
            }

            val combinedPath = Path().apply {
                op(screenPath, cropPath, PathOperation.Difference)
            }

            drawPath(path = combinedPath, color = Color.Black.copy(alpha = 0.7f))
        }
    }
}
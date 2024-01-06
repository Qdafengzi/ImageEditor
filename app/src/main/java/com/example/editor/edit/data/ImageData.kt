package com.example.editor.edit.data

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ImageData(
    val image: Bitmap,
    val position: Offset,
    val imageDefaultSize: Dp = 40.dp,
    val defaultSize:Size = Size.Zero,
    val rotate: Float = 0f,
    val scale: Float = 1f,
    val scaleIconOffset: Offset = Offset.Zero,
    val deleteIconOffset: Offset = Offset.Zero,
    val rotateIconOffset: Offset = Offset.Zero,
    val deleteRectF: RectF = RectF(),
    val rotateRectF: RectF = RectF(),
    val scaleRectF: RectF = RectF(),
)
package com.example.editor.edit.data

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ImageData(
    val image: Bitmap,
    val position: Offset,
    val imageSize: Dp = 40.dp,
    val rotate: Float = 0f,
    val scale: Float = 1f,
    val scaleIconOffset: Offset = Offset.Zero,
    val deleteIconOffset: Offset = Offset.Zero,
    val rotateIconOffset: Offset = Offset.Zero,
)
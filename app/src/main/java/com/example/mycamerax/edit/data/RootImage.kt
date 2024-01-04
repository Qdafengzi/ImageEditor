package com.example.mycamerax.edit.data

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.example.mycamerax.edit.EditeType

data class RootImage(
    val rootBitmap: Bitmap? = null,
    val center: Offset = Offset.Zero,
    val editType: EditeType = EditeType.NONE,
    val originalSize: Size = Size.Zero,
    val cropRect: Rect = Rect.Zero,
)
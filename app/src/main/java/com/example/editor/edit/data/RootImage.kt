package com.example.editor.edit.data

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.editor.edit.EditeType

data class RootImage(
    val rootBitmap: Bitmap? = null,
    val center: Offset = Offset.Zero,
    val editType: EditeType = EditeType.NONE,
    val originalSize: Size = Size.Zero,
    val destRect: RectF = RectF(),
    val helpBox: RectF = RectF(),
    val initWidth:Float = 0f,
    val matrix: Matrix = Matrix(),



    )
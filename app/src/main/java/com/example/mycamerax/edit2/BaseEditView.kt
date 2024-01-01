package com.example.mycamerax.edit2

import android.content.Context
import android.graphics.Bitmap
import android.view.View

abstract class BaseEditView(context: Context): View(context) {
    val deque = ArrayList<Bitmap>()

    abstract fun updateBitmap(bitmap: Bitmap)

    abstract fun updateCenterOffset()

    abstract fun addBitmap(bitmap: Bitmap)
    abstract fun deleteBitmap(index:Int)


    abstract fun bingImageToFont(index:Int)

}
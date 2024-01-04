package com.example.editor.edit.crop.util.model

import android.graphics.RectF

class AnimatedRectF : RectF() {

    fun setTop(top: Float) {
        this.top = top
    }

    fun setBottom(bottom: Float) {
        this.bottom = bottom
    }

    fun setRight(right: Float) {
        this.right = right
    }

    fun setLeft(left: Float) {
        this.left = left
    }
}
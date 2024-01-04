package com.example.editor.edit.crop.util.model


enum class Corner {
    NONE,
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}

fun Corner.opposite() {
    when (this) {
        Corner.TOP_RIGHT -> Corner.BOTTOM_LEFT
        Corner.TOP_LEFT -> Corner.BOTTOM_RIGHT
        Corner.BOTTOM_RIGHT -> Corner.TOP_LEFT
        Corner.BOTTOM_LEFT -> Corner.TOP_RIGHT
        Corner.NONE -> Corner.NONE
    }
}
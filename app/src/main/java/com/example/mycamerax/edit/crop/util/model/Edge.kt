package com.example.mycamerax.edit.crop.util.model

import com.example.mycamerax.edit.crop.util.model.Edge.BOTTOM
import com.example.mycamerax.edit.crop.util.model.Edge.LEFT
import com.example.mycamerax.edit.crop.util.model.Edge.NONE
import com.example.mycamerax.edit.crop.util.model.Edge.RIGHT
import com.example.mycamerax.edit.crop.util.model.Edge.TOP

enum class Edge {
    NONE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

fun Edge.opposite() {
    when (this) {
        LEFT -> RIGHT
        TOP -> BOTTOM
        RIGHT -> LEFT
        BOTTOM -> TOP
        NONE -> NONE
    }
}
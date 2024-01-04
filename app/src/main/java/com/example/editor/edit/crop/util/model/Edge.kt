package com.example.editor.edit.crop.util.model


enum class Edge {
    NONE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

fun Edge.opposite() {
    when (this) {
        Edge.LEFT -> Edge.RIGHT
        Edge.TOP -> Edge.BOTTOM
        Edge.RIGHT -> Edge.LEFT
        Edge.BOTTOM ->Edge.TOP
        Edge.NONE -> Edge.NONE
    }
}
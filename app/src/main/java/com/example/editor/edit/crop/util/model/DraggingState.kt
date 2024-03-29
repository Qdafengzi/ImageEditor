package com.example.editor.edit.crop.util.model

sealed class DraggingState {
    data class DraggingCorner(var corner: Corner) : DraggingState()

    data class DraggingEdge(var edge: Edge) : DraggingState()

    object DraggingBitmap : DraggingState()

    object Idle : DraggingState()
}